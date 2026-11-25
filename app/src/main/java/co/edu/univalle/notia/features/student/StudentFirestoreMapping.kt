package co.edu.univalle.notia.features.student

import co.edu.univalle.notia.core.grades.MAX_GRADE
import co.edu.univalle.notia.core.grades.MIN_GRADE
import co.edu.univalle.notia.core.grades.TOTAL_WEIGHT
import co.edu.univalle.notia.features.organizations.Roles
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.Date

/** Error del repositorio con un mensaje listo para mostrar en la UI (en español). */
class StudentRepositoryException(override val message: String, cause: Throwable? = null) : Exception(message, cause)

/** Mensaje para la UI: el del repositorio si lo hay; si no, [default]. */
internal fun Throwable.toUserMessage(default: String): String =
    if (this is StudentRepositoryException) message else default

/**
 * Esquema de Firestore que espera el repositorio (el lado docente debe escribir lo mismo):
 * - subjects/{id}: nombre, codigo, docenteNombre, studentIds: [uid]
 * - activities/{id}: nombre, porcentaje, estado ("publicada" | "borrador")
 * - grades/{id}: studentId, activityId, estado (copia del estado de la actividad), nota, retroalimentacion, actualizadoEn
 * - notices/{id}: studentId, subjectId, subjectName, activityId, activityName, type, createdAt, read
 * El campo `estado` es el que usan las reglas de seguridad; sin él el estudiante no puede leer el documento.
 */
internal object StudentSchema {
    const val ESTADO = "estado"
    const val STUDENT_IDS = "studentIds"
    const val STUDENT_ID = "studentId"
    const val ACTIVITY_ID = "activityId"
    const val READ = "read"
}

/** Documento de Firestore ya convertido a tipos simples (permite probar el mapeo sin Firebase). */
internal data class RawDoc(val id: String, val data: Map<String, Any?>) {
    fun text(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key -> (data[key] as? String)?.trim()?.takeIf { it.isNotEmpty() } }

    fun first(vararg keys: String): Any? = keys.firstNotNullOfOrNull { data[it] }
}

private val PUBLISHED = PublicationState.PUBLISHED

private fun RawDoc.isPublished(): Boolean = PublicationState.fromRaw(text(StudentSchema.ESTADO)) == PUBLISHED

/** Nota como BigDecimal con 1 decimal (HALF_UP). Null si no es número o está fuera de 0.0..5.0. */
internal fun parseGrade(raw: Any?): BigDecimal? {
    val text = when (raw) {
        is Number -> raw.toString() // Double.toString(2.95) = "2.95": nunca BigDecimal(Double).
        is String -> raw.trim().replace(',', '.')
        else -> return null
    }
    val value = text.toBigDecimalOrNull() ?: return null
    if (value < MIN_GRADE || value > MAX_GRADE) return null
    return value.setScale(1, RoundingMode.HALF_UP)
}

/** Porcentaje entero en 1..100. Null si no es un entero válido. */
internal fun parsePercentage(raw: Any?): Int? {
    val value = when (raw) {
        is Number -> raw.toString().toBigDecimalOrNull()
        is String -> raw.trim().toBigDecimalOrNull()
        else -> null
    } ?: return null
    val exact = try {
        value.intValueExact()
    } catch (e: ArithmeticException) {
        return null
    }
    return exact.takeIf { it in 1..TOTAL_WEIGHT }
}

internal fun parseInstant(raw: Any?): Instant? = when (raw) {
    is Timestamp -> raw.toDate().toInstant()
    is Date -> raw.toInstant()
    is Instant -> raw
    is Number -> Instant.ofEpochMilli(raw.toLong())
    else -> null
}

internal fun mapSubject(doc: RawDoc): StudentSubject = StudentSubject(
    id = doc.id,
    name = doc.text("nombre", "name") ?: "Materia sin nombre",
    code = doc.text("codigo", "code") ?: "—",
    teacherName = doc.text("docenteNombre", "docente", "teacherName") ?: "Docente sin asignar",
)

internal fun isEnrolled(subjectData: Map<String, Any?>, uid: String): Boolean =
    (subjectData[StudentSchema.STUDENT_IDS] as? Collection<*>)?.contains(uid) == true

/**
 * Arma la materia del estudiante a partir de documentos crudos (RN-06, RNF-05):
 * - solo actividades con estado "publicada" (cualquier otro valor se trata como borrador);
 * - solo notas del propio estudiante, publicadas y de actividades publicadas;
 * - actividades con porcentaje inválido se descartan y notas fuera de 0.0..5.0 se ignoran (quedan pendientes);
 * - usa [StudentSubjectGrades.fromPublished] para que `hiddenWeight` sea consistente.
 */
internal fun buildSubjectGrades(
    subject: RawDoc,
    activities: List<RawDoc>,
    grades: List<RawDoc>,
    uid: String,
): StudentSubjectGrades {
    val gradeByActivity = grades
        .filter { it.text(StudentSchema.STUDENT_ID) == uid && it.isPublished() }
        .mapNotNull { doc -> doc.text(StudentSchema.ACTIVITY_ID)?.let { it to doc } }
        .toMap()

    val published = activities
        .filter { it.isPublished() }
        .mapNotNull { doc ->
            val percentage = parsePercentage(doc.first("porcentaje", "percentage")) ?: return@mapNotNull null
            val grade = gradeByActivity[doc.id]
            StudentActivity(
                id = doc.id,
                name = doc.text("nombre", "name") ?: "Actividad",
                percentage = percentage,
                grade = grade?.let { parseGrade(it.first("nota", "valor", "grade")) },
                feedback = grade?.text("retroalimentacion", "feedback"),
                updatedAt = grade?.let { parseInstant(it.first("actualizadoEn", "updatedAt")) },
            )
        }

    return StudentSubjectGrades.fromPublished(mapSubject(subject), published)
}

internal fun parseNoticeType(raw: String?): NoticeType? = when (raw?.trim()?.lowercase()) {
    "grade_published", "publicada", "published" -> NoticeType.GRADE_PUBLISHED
    "grade_edited", "editada", "edited" -> NoticeType.GRADE_EDITED
    else -> null
}

/** Aviso del estudiante [uid]; null si es de otro estudiante o está incompleto. */
internal fun mapNotice(doc: RawDoc, uid: String): StudentNotice? {
    if (doc.text(StudentSchema.STUDENT_ID) != uid) return null
    val type = parseNoticeType(doc.text("type")) ?: return null
    val createdAt = parseInstant(doc.data["createdAt"]) ?: return null
    val subjectId = doc.text("subjectId") ?: return null
    return StudentNotice(
        id = doc.id,
        subjectId = subjectId,
        subjectName = doc.text("subjectName") ?: "Materia",
        activityId = doc.text(StudentSchema.ACTIVITY_ID) ?: "",
        activityName = doc.text("activityName") ?: "Actividad",
        type = type,
        createdAt = createdAt,
        isRead = doc.data[StudentSchema.READ] == true,
    )
}

/** Perfil con valores seguros: usa lo que exista en users/{uid}, el miembro de la organización y FirebaseAuth. */
internal fun buildProfile(
    uid: String,
    authEmail: String?,
    authName: String?,
    userData: Map<String, Any?>?,
    memberData: Map<String, Any?>?,
): StudentProfile {
    val user = RawDoc(uid, userData.orEmpty())
    val member = RawDoc(uid, memberData.orEmpty())
    return StudentProfile(
        id = uid,
        name = user.text("nombre") ?: member.text("nombre") ?: authName?.trim().orEmpty(),
        code = user.text("codigo") ?: member.text("codigo") ?: "",
        email = user.text("correo") ?: authEmail?.trim().orEmpty(),
        program = user.text("programa") ?: "",
    )
}

/** Organización activa válida para el módulo estudiante (RN-06); si no, error claro en español. */
internal fun requireStudentOrgId(orgId: String?, role: String?): String {
    if (orgId.isNullOrBlank()) {
        throw StudentRepositoryException("Selecciona una organización para ver tus materias.")
    }
    if (role != Roles.STUDENT) {
        throw StudentRepositoryException("Tu rol en esta organización no es Estudiante. Elige otra organización.")
    }
    return orgId
}

internal fun friendlyErrorMessage(error: Throwable): String = when (error) {
    is StudentRepositoryException -> error.message
    is FirebaseNetworkException -> "Sin conexión. Revisa tu internet e inténtalo de nuevo."
    is FirebaseFirestoreException -> when (error.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> "No tienes permiso para ver esta información."
        FirebaseFirestoreException.Code.UNAUTHENTICATED -> "Tu sesión expiró. Inicia sesión de nuevo."
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        -> "No hay conexión con el servidor. Inténtalo de nuevo."
        else -> "Ocurrió un error al consultar tus datos. Inténtalo de nuevo."
    }
    else -> "Ocurrió un error inesperado. Inténtalo de nuevo."
}
