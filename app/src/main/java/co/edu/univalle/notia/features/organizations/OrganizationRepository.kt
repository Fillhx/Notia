package co.edu.univalle.notia.features.organizations

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

/** Error de organizaciones con un mensaje ya listo para mostrar al usuario. */
class OrganizationException(message: String) : Exception(message)

class OrganizationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val random = SecureRandom()

    private val uid: String
        get() = auth.currentUser?.uid
            ?: throw OrganizationException("Tu sesión expiró. Ingresa de nuevo")

    /** RF-03: crea la organización; quien la crea queda como Coordinación y se genera el código de invitación. */
    suspend fun createOrganization(name: String): Result<OrganizationMembership> = runOrg {
        val userId = uid
        val orgName = name.trim()
        if (orgName.isEmpty()) throw OrganizationException("Ingresa el nombre de la organización")
        val userName = auth.currentUser?.displayName.orEmpty()

        var lastError: Exception? = null
        for (attempt in 1..3) {
            val code = generateInviteCode()
            val orgRef = db.collection("organizations").document()
            val batch = db.batch()
            batch.set(
                orgRef,
                mapOf("nombre" to orgName, "codigoInvitacion" to code, "creadaPor" to userId)
            )
            batch.set(
                orgRef.collection("members").document(userId),
                mapOf("uid" to userId, "rol" to Roles.COORDINATION, "nombre" to userName)
            )
            batch.set(
                db.collection("inviteCodes").document(code),
                mapOf("orgId" to orgRef.id, "orgNombre" to orgName)
            )
            try {
                batch.commit().await()
                return@runOrg OrganizationMembership(orgRef.id, orgName, Roles.COORDINATION, code)
            } catch (e: FirebaseFirestoreException) {
                // Si el código ya existe, las reglas lo rechazan y se reintenta con otro código.
                lastError = e
            }
        }
        throw lastError ?: OrganizationException("No se pudo crear la organización")
    }

    /** RF-04: unirse con un código de invitación; entra como Estudiante. */
    suspend fun joinWithCode(rawCode: String): Result<OrganizationMembership> = runOrg {
        val userId = uid
        val code = rawCode.trim().uppercase()
        if (code.isEmpty()) throw OrganizationException("Ingresa un código")

        val codeDoc = db.collection("inviteCodes").document(code).get().await()
        val orgId = codeDoc.getString("orgId")
        val orgName = codeDoc.getString("orgNombre")
        if (!codeDoc.exists() || orgId == null || orgName == null) {
            throw OrganizationException("Código no válido")
        }

        val memberRef = db.collection("organizations").document(orgId)
            .collection("members").document(userId)
        if (memberRef.get().await().exists()) {
            throw OrganizationException("Ya perteneces a esta organización")
        }

        memberRef.set(
            mapOf(
                "uid" to userId,
                "rol" to Roles.STUDENT,
                "nombre" to auth.currentUser?.displayName.orEmpty(),
                "codigo" to code
            )
        ).await()
        OrganizationMembership(orgId, orgName, Roles.STUDENT)
    }

    /** RF-05: todas las organizaciones a las que pertenece el usuario, con su rol en cada una. */
    suspend fun myOrganizations(): Result<List<OrganizationMembership>> = runOrg {
        val userId = uid
        val memberships = db.collectionGroup("members").whereEqualTo("uid", userId).get().await()
        memberships.documents.mapNotNull { doc ->
            val orgRef = doc.reference.parent.parent ?: return@mapNotNull null
            val role = doc.getString("rol") ?: return@mapNotNull null
            val orgName = orgRef.get().await().getString("nombre") ?: return@mapNotNull null
            OrganizationMembership(orgRef.id, orgName, role)
        }
    }

    private fun generateInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val suffix = (1..4).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
        return "NOT-$suffix"
    }

    private suspend fun <T> runOrg(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: OrganizationException) {
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("OrganizationRepo", "Error en organizaciones", e)
            Result.failure(OrganizationException(e.toFriendlyMessage()))
        }

    private fun Exception.toFriendlyMessage(): String = when {
        this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "No tienes permiso para hacer esto"
        this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
            "Falta configurar un índice en Firestore. Avísale al equipo"
        this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Sin conexión. Revisa tu internet e inténtalo de nuevo"
        this is FirebaseNetworkException ->
            "Sin conexión. Revisa tu internet e inténtalo de nuevo"
        else -> "Ocurrió un error inesperado. Inténtalo de nuevo"
    }
}
