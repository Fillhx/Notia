package co.edu.univalle.notia.features.teacher

import co.edu.univalle.notia.features.organizations.ActiveOrganization
import co.edu.univalle.notia.features.organizations.OrganizationMembership
import co.edu.univalle.notia.features.organizations.Roles
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TeacherRepositoryException(override val message: String, cause: Throwable? = null) : Exception(message, cause)

interface TeacherRepository {
    suspend fun getSubjects(): List<TeacherSubject>
    suspend fun loadSubjectWorkspace(subjectId: String): TeacherSubjectWorkspace
    suspend fun saveDraft(subjectId: String, activityId: String, values: Map<String, BigDecimal?>)
    suspend fun publishActivity(subjectId: String, activityId: String, values: Map<String, BigDecimal?>)
    suspend fun updatePublishedGrades(subjectId: String, activityId: String, values: Map<String, BigDecimal?>)
}

data class TeacherSubjectWorkspace(
    val subject: TeacherSubject,
    val students: List<EnrolledStudent>,
    val activities: List<GradeActivity>,
    val gradesByActivity: Map<String, Map<String, BigDecimal>>,
)

class FirestoreTeacherRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val activeOrganization: () -> OrganizationMembership? = { ActiveOrganization.current },
) : TeacherRepository {

    private data class Session(val uid: String, val orgId: String, val orgName: String)

    private fun session(): Session {
        val uid = auth.currentUser?.uid
            ?: throw TeacherRepositoryException("Inicia sesión para gestionar tus materias.")
        val org = activeOrganization()
        if (org == null || org.orgId.isBlank()) {
            throw TeacherRepositoryException("Selecciona una organización para ver tus materias.")
        }
        if (org.role != Roles.TEACHER) {
            throw TeacherRepositoryException("Tu rol en esta organización no es Docente.")
        }
        return Session(uid = uid, orgId = org.orgId, orgName = org.orgName)
    }

    private fun orgRef(orgId: String): DocumentReference =
        db.collection("organizations").document(orgId)

    private fun subjectRef(orgId: String, subjectId: String): DocumentReference =
        orgRef(orgId).collection("subjects").document(subjectId)

    override suspend fun getSubjects(): List<TeacherSubject> = guarded {
        val s = session()
        val snap = orgRef(s.orgId).collection("subjects")
            .whereEqualTo("docenteId", s.uid)
            .get()
            .awaitTask()

        val periodNames = loadPeriodNames(s.orgId)
        snap.documents.map { doc ->
            val data = doc.data.orEmpty()
            val periodId = data.text("periodoId", "periodId")
            TeacherSubject(
                id = doc.id,
                name = data.text("nombre", "name") ?: "Materia sin nombre",
                organizationName = data.text("organizacionNombre", "organizationName") ?: s.orgName,
                periodName = data.text("periodoNombre", "periodName")
                    ?: periodId?.let { periodNames[it] }
                    ?: "Periodo actual",
            )
        }.sortedBy { it.name.lowercase() }
    }

    override suspend fun loadSubjectWorkspace(subjectId: String): TeacherSubjectWorkspace = guarded {
        val s = session()
        val subRef = subjectRef(s.orgId, subjectId)
        val subSnap = subRef.get().awaitTask()
        if (!subSnap.exists()) {
            throw TeacherRepositoryException("La materia no existe.")
        }
        val subData = subSnap.data.orEmpty()
        val docenteId = subData.text("docenteId")
        if (docenteId != null && docenteId != s.uid) {
            throw TeacherRepositoryException("No tienes asignada esta materia.")
        }

        val periodNames = loadPeriodNames(s.orgId)
        val periodId = subData.text("periodoId", "periodId")
        val subject = TeacherSubject(
            id = subSnap.id,
            name = subData.text("nombre", "name") ?: "Materia sin nombre",
            organizationName = subData.text("organizacionNombre", "organizationName") ?: s.orgName,
            periodName = subData.text("periodoNombre", "periodName")
                ?: periodId?.let { periodNames[it] }
                ?: "Periodo actual",
        )

        val studentIds = (subData["studentIds"] as? Collection<*>)
            ?.mapNotNull { (it as? String)?.trim()?.takeIf(String::isNotEmpty) }
            ?.distinct()
            .orEmpty()

        coroutineScope {
            val studentsDeferred = async { resolveStudents(s.orgId, studentIds) }
            val activitiesDeferred = async { subRef.collection("activities").get().awaitTask() }
            val gradesDeferred = async { subRef.collection("grades").get().awaitTask() }

            val activities = activitiesDeferred.await().documents.map { doc ->
                val d = doc.data.orEmpty()
                val rawEstado = d.text("estado", "status")?.lowercase()
                val status = if (rawEstado == "publicada" || rawEstado == "published") {
                    ActivityStatus.PUBLISHED
                } else {
                    ActivityStatus.DRAFT
                }
                val pct = parsePercentage(d["porcentaje"] ?: d["percentage"]) ?: 0
                GradeActivity(
                    id = doc.id,
                    name = d.text("nombre", "name") ?: "Actividad",
                    percentage = pct,
                    status = status,
                )
            }

            val gradesByActivity = mutableMapOf<String, MutableMap<String, BigDecimal>>()
            gradesDeferred.await().documents.forEach { doc ->
                val d = doc.data.orEmpty()
                val activityId = d.text("activityId") ?: return@forEach
                val studentId = d.text("studentId") ?: return@forEach
                val parsed = parseGradeBigDecimal(d["nota"] ?: d["valor"] ?: d["grade"]) ?: return@forEach
                gradesByActivity.getOrPut(activityId) { mutableMapOf() }[studentId] = parsed
            }

            TeacherSubjectWorkspace(
                subject = subject,
                students = studentsDeferred.await(),
                activities = activities,
                gradesByActivity = gradesByActivity.mapValues { it.value.toMap() },
            )
        }
    }

    override suspend fun saveDraft(
        subjectId: String,
        activityId: String,
        values: Map<String, BigDecimal?>,
    ) = guarded {
        val s = session()
        val subRef = subjectRef(s.orgId, subjectId)
        val actRef = subRef.collection("activities").document(activityId)
        val actSnap = actRef.get().awaitTask()
        val statusRaw = actSnap.data.orEmpty().text("estado", "status")?.lowercase()
        if (statusRaw == "publicada" || statusRaw == "published") {
            throw TeacherRepositoryException("La actividad ya está publicada.")
        }

        val now = Timestamp.now()
        val gradesCol = subRef.collection("grades")
        val existingSnap = gradesCol.whereEqualTo("activityId", activityId).get().awaitTask()
        val existingByStudent = existingSnap.documents.associateBy {
            it.data.orEmpty().text("studentId").orEmpty()
        }

        val batch = db.batch()
        values.forEach { (studentId, gradeValue) ->
            val existingDoc = existingByStudent[studentId]
            if (gradeValue == null) {
                existingDoc?.reference?.let { batch.delete(it) }
            } else {
                val targetRef = existingDoc?.reference ?: gradesCol.document("${activityId}_${studentId}")
                batch.set(
                    targetRef,
                    mapOf(
                        "studentId" to studentId,
                        "activityId" to activityId,
                        "nota" to gradeValue.toDouble(),
                        "estado" to "borrador",
                        "actualizadoEn" to now,
                    ),
                    SetOptions.merge()
                )
            }
        }
        batch.commit().awaitTask()
        Unit
    }

    override suspend fun publishActivity(
        subjectId: String,
        activityId: String,
        values: Map<String, BigDecimal?>,
    ) = guarded {
        val s = session()
        val subRef = subjectRef(s.orgId, subjectId)
        val actRef = subRef.collection("activities").document(activityId)
        val subSnap = subRef.get().awaitTask()
        val actSnap = actRef.get().awaitTask()

        val subjectName = subSnap.data.orEmpty().text("nombre", "name") ?: "Materia"
        val activityName = actSnap.data.orEmpty().text("nombre", "name") ?: "Actividad"
        val now = Timestamp.now()

        val gradesCol = subRef.collection("grades")
        val existingSnap = gradesCol.whereEqualTo("activityId", activityId).get().awaitTask()
        val existingByStudent = existingSnap.documents.associateBy {
            it.data.orEmpty().text("studentId").orEmpty()
        }
        val noticesCol = orgRef(s.orgId).collection("notices")

        val batch = db.batch()
        batch.update(actRef, mapOf("estado" to "publicada", "actualizadoEn" to now))

        values.forEach { (studentId, gradeValue) ->
            val existingDoc = existingByStudent[studentId]
            if (gradeValue == null) {
                existingDoc?.reference?.let { batch.delete(it) }
            } else {
                val targetRef = existingDoc?.reference ?: gradesCol.document("${activityId}_${studentId}")
                batch.set(
                    targetRef,
                    mapOf(
                        "studentId" to studentId,
                        "activityId" to activityId,
                        "nota" to gradeValue.toDouble(),
                        "estado" to "publicada",
                        "actualizadoEn" to now,
                    ),
                    SetOptions.merge()
                )
                val noticeRef = noticesCol.document()
                batch.set(
                    noticeRef,
                    mapOf(
                        "studentId" to studentId,
                        "subjectId" to subjectId,
                        "subjectName" to subjectName,
                        "activityId" to activityId,
                        "activityName" to activityName,
                        "type" to "grade_published",
                        "createdAt" to now,
                        "read" to false,
                    )
                )
            }
        }

        batch.commit().awaitTask()
        Unit
    }

    override suspend fun updatePublishedGrades(
        subjectId: String,
        activityId: String,
        values: Map<String, BigDecimal?>,
    ) = guarded {
        val s = session()
        val subRef = subjectRef(s.orgId, subjectId)
        val actRef = subRef.collection("activities").document(activityId)
        val subSnap = subRef.get().awaitTask()
        val actSnap = actRef.get().awaitTask()

        val subjectName = subSnap.data.orEmpty().text("nombre", "name") ?: "Materia"
        val activityName = actSnap.data.orEmpty().text("nombre", "name") ?: "Actividad"
        val now = Timestamp.now()

        val gradesCol = subRef.collection("grades")
        val historyCol = subRef.collection("history")
        val noticesCol = orgRef(s.orgId).collection("notices")

        val existingSnap = gradesCol.whereEqualTo("activityId", activityId).get().awaitTask()
        val existingByStudent = existingSnap.documents.associateBy {
            it.data.orEmpty().text("studentId").orEmpty()
        }

        val batch = db.batch()
        values.forEach { (studentId, newGrade) ->
            val existingDoc = existingByStudent[studentId]
            val oldGrade = existingDoc?.data?.get("nota")?.let { parseGradeBigDecimal(it) }
            val changed = when {
                oldGrade == null && newGrade == null -> false
                oldGrade == null || newGrade == null -> true
                else -> oldGrade.compareTo(newGrade) != 0
            }
            if (!changed) return@forEach

            if (newGrade == null) {
                existingDoc?.reference?.let { batch.delete(it) }
            } else {
                val targetRef = existingDoc?.reference ?: gradesCol.document("${activityId}_${studentId}")
                batch.set(
                    targetRef,
                    mapOf(
                        "studentId" to studentId,
                        "activityId" to activityId,
                        "nota" to newGrade.toDouble(),
                        "estado" to "publicada",
                        "actualizadoEn" to now,
                    ),
                    SetOptions.merge()
                )
            }

            batch.set(
                historyCol.document(),
                mapOf(
                    "activityId" to activityId,
                    "studentId" to studentId,
                    "oldValue" to oldGrade?.toDouble(),
                    "newValue" to newGrade?.toDouble(),
                    "authorId" to s.uid,
                    "timestamp" to now,
                )
            )

            batch.set(
                noticesCol.document(),
                mapOf(
                    "studentId" to studentId,
                    "subjectId" to subjectId,
                    "subjectName" to subjectName,
                    "activityId" to activityId,
                    "activityName" to activityName,
                    "type" to "grade_edited",
                    "createdAt" to now,
                    "read" to false,
                )
            )
        }

        batch.commit().awaitTask()
        Unit
    }

    private suspend fun loadPeriodNames(orgId: String): Map<String, String> = try {
        orgRef(orgId).collection("periods").get().awaitTask().documents.associate { doc ->
            val name = doc.data.orEmpty().text("nombre", "name") ?: doc.id
            doc.id to name
        }
    } catch (_: Exception) {
        emptyMap()
    }

    private suspend fun resolveStudents(orgId: String, studentIds: List<String>): List<EnrolledStudent> {
        if (studentIds.isEmpty()) return emptyList()
        return coroutineScope {
            studentIds.map { studentId ->
                async {
                    val memberData = try {
                        orgRef(orgId).collection("members").document(studentId).get().awaitTask().data.orEmpty()
                    } catch (_: Exception) {
                        emptyMap()
                    }
                    val userData = try {
                        db.collection("users").document(studentId).get().awaitTask().data.orEmpty()
                    } catch (_: Exception) {
                        emptyMap()
                    }
                    val name = memberData.text("nombre", "name")
                        ?: userData.text("nombre", "name")
                        ?: "Estudiante"
                    val code = memberData.text("codigo", "code")
                        ?: userData.text("codigo", "code")
                        ?: studentId.take(8)
                    EnrolledStudent(id = studentId, name = name, code = code)
                }
            }.awaitAll().sortedBy { it.name.lowercase() }
        }
    }

    private fun Map<String, Any?>.text(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            (this[key] as? String)?.trim()?.takeIf { it.isNotEmpty() }
        }

    private fun parseGradeBigDecimal(raw: Any?): BigDecimal? {
        val text = when (raw) {
            is Number -> raw.toString()
            is String -> raw.trim().replace(',', '.')
            else -> return null
        }
        val value = text.toBigDecimalOrNull() ?: return null
        if (value < BigDecimal.ZERO || value > BigDecimal("5.0")) return null
        return value.setScale(1, RoundingMode.HALF_UP)
    }

    private fun parsePercentage(raw: Any?): Int? {
        val bd = when (raw) {
            is Number -> raw.toString().toBigDecimalOrNull()
            is String -> raw.trim().toBigDecimalOrNull()
            else -> null
        } ?: return null
        return try {
            bd.intValueExact().takeIf { it in 1..100 }
        } catch (_: ArithmeticException) {
            null
        }
    }

    private suspend fun <T> guarded(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: TeacherRepositoryException) {
        throw e
    } catch (e: FirebaseNetworkException) {
        throw TeacherRepositoryException("Sin conexión. Revisa tu internet e inténtalo de nuevo.", e)
    } catch (e: FirebaseFirestoreException) {
        val msg = when (e.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "No tienes permiso para realizar esta acción en Firestore."
            FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                "Tu sesión expiró. Inicia sesión de nuevo."
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                "No hay conexión con el servidor. Inténtalo de nuevo."
            else -> "Error al consultar o guardar los datos en Firestore."
        }
        throw TeacherRepositoryException(msg, e)
    } catch (e: Exception) {
        throw TeacherRepositoryException("Ocurrió un error inesperado. Inténtalo de nuevo.", e)
    }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}