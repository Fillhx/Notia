package co.edu.univalle.notia.features.student

import co.edu.univalle.notia.features.organizations.ActiveOrganization
import co.edu.univalle.notia.features.organizations.OrganizationMembership
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementación de [StudentRepository] sobre Firestore (RN-06, RNF-04, RNF-05).
 * Los borradores nunca salen de aquí: se consultan solo actividades `estado == "publicada"`
 * y solo notas propias publicadas; el filtrado se repite en [buildSubjectGrades] y lo refuerzan las reglas.
 * Todos los errores salen como [StudentRepositoryException] con mensaje en español.
 */
class FirestoreStudentRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val activeOrganization: () -> OrganizationMembership? = { ActiveOrganization.current },
) : StudentRepository {

    private data class Session(val uid: String, val orgId: String)

    private fun session(): Session {
        val uid = auth.currentUser?.uid
            ?: throw StudentRepositoryException("Inicia sesión para ver tus notas.")
        val org = activeOrganization()
        return Session(uid, requireStudentOrgId(org?.orgId, org?.role))
    }

    private fun orgRef(orgId: String): DocumentReference = db.collection("organizations").document(orgId)

    private fun subjectsRef(orgId: String): CollectionReference = orgRef(orgId).collection("subjects")

    private fun noticesRef(orgId: String): CollectionReference = orgRef(orgId).collection("notices")

    override suspend fun getProfile(): StudentProfile = guarded {
        val user = auth.currentUser
            ?: throw StudentRepositoryException("Inicia sesión para ver tu perfil.")
        val orgId = activeOrganization()?.orgId?.takeIf { it.isNotBlank() }

        // Lecturas opcionales: si fallan, el perfil se arma con los datos de FirebaseAuth.
        val userData = bestEffort { db.collection("users").document(user.uid).get().awaitTask().data }
        val memberData = orgId?.let {
            bestEffort { orgRef(it).collection("members").document(user.uid).get().awaitTask().data }
        }
        buildProfile(user.uid, user.email, user.displayName, userData, memberData)
    }

    override suspend fun getSubjects(): List<StudentSubjectGrades> = guarded {
        val (uid, orgId) = session()
        val enrolled = subjectsRef(orgId)
            .whereArrayContains(StudentSchema.STUDENT_IDS, uid)
            .get()
            .awaitTask()
            .toRawDocs()
        coroutineScope {
            enrolled.map { async { loadSubject(orgId, uid, it) } }.awaitAll()
        }.sortedBy { it.subject.name.lowercase() }
    }

    override suspend fun getSubject(subjectId: String): StudentSubjectGrades? = guarded {
        val (uid, orgId) = session()
        val snapshot = subjectsRef(orgId).document(subjectId).get().awaitTask()
        if (!snapshot.exists()) return@guarded null
        val data = snapshot.data.orEmpty()
        if (!isEnrolled(data, uid)) return@guarded null
        loadSubject(orgId, uid, RawDoc(snapshot.id, data))
    }

    override fun observeNotices(): Flow<List<StudentNotice>> = callbackFlow {
        val (uid, orgId) = session()
        // Se ordena en cliente: evita exigir un índice compuesto (studentId + createdAt).
        val registration = noticesRef(orgId)
            .whereEqualTo(StudentSchema.STUDENT_ID, uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(StudentRepositoryException(friendlyErrorMessage(error), error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val notices = snapshot.documents
                        .mapNotNull { mapNotice(RawDoc(it.id, it.data.orEmpty()), uid) }
                        .sortedByDescending { it.createdAt }
                    trySend(notices)
                }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun markNoticeRead(noticeId: String) = guarded {
        val (_, orgId) = session()
        noticesRef(orgId).document(noticeId).update(StudentSchema.READ, true).awaitTask()
        Unit
    }

    override suspend fun markAllNoticesRead() = guarded {
        val (uid, orgId) = session()
        val unread = noticesRef(orgId)
            .whereEqualTo(StudentSchema.STUDENT_ID, uid)
            .whereEqualTo(StudentSchema.READ, false)
            .get()
            .awaitTask()
            .documents
        unread.chunked(MAX_BATCH_WRITES).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.update(it.reference, StudentSchema.READ, true) }
            batch.commit().awaitTask()
        }
        Unit
    }

    private suspend fun loadSubject(orgId: String, uid: String, subject: RawDoc): StudentSubjectGrades =
        coroutineScope {
            val subjectRef = subjectsRef(orgId).document(subject.id)
            val published = PublicationState.PUBLISHED.raw
            val activities = async {
                subjectRef.collection("activities")
                    .whereEqualTo(StudentSchema.ESTADO, published)
                    .get().awaitTask().toRawDocs()
            }
            val grades = async {
                subjectRef.collection("grades")
                    .whereEqualTo(StudentSchema.STUDENT_ID, uid)
                    .whereEqualTo(StudentSchema.ESTADO, published)
                    .get().awaitTask().toRawDocs()
            }
            buildSubjectGrades(subject, activities.await(), grades.await(), uid)
        }

    private fun QuerySnapshot.toRawDocs(): List<RawDoc> = documents.map { RawDoc(it.id, it.data.orEmpty()) }

    private suspend fun <T> guarded(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: StudentRepositoryException) {
        throw e
    } catch (e: Exception) {
        throw StudentRepositoryException(friendlyErrorMessage(e), e)
    }

    private suspend fun <T> bestEffort(block: suspend () -> T?): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val MAX_BATCH_WRITES = 450
    }
}

/** Espera un Task sin depender de kotlinx-coroutines-play-services. */
internal suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
