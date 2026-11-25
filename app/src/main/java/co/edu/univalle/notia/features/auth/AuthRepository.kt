package co.edu.univalle.notia.features.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** Error de autenticación con un mensaje ya listo para mostrar al usuario. */
class AuthException(message: String) : Exception(message)

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    suspend fun register(name: String, email: String, password: String): Result<Unit> = runAuth {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val profile = UserProfileChangeRequest.Builder().setDisplayName(name).build()
        result.user?.updateProfile(profile)?.await()
        result.user?.uid?.let { userId ->
            try {
                FirebaseFirestore.getInstance().collection("users").document(userId)
                    .set(mapOf("nombre" to name, "correo" to email)).await()
            } catch (e: FirebaseFirestoreException) {
                // Si falla guardar el perfil, la cuenta igual queda creada.
            }
        }
        result.user?.sendEmailVerification()?.await()
        // La cuenta no se usa hasta que el correo esté verificado.
        auth.signOut()
    }

    suspend fun login(email: String, password: String): Result<Unit> = runAuth {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        if (result.user?.isEmailVerified != true) {
            auth.signOut()
            throw AuthException("Verifica tu correo antes de ingresar. Revisa tu bandeja de entrada o spam")
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runAuth {
        auth.sendPasswordResetEmail(email).await()
    }

    fun logout() {
        auth.signOut()
    }

    private suspend fun runAuth(block: suspend () -> Unit): Result<Unit> =
        try {
            block()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(AuthException(e.toFriendlyMessage()))
        }

    private fun Exception.toFriendlyMessage(): String = when (this) {
        is AuthException -> message ?: "Ocurrió un error inesperado. Inténtalo de nuevo"
        // WeakPassword hereda de InvalidCredentials, por eso va primero.
        is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil"
        is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos"
        is FirebaseAuthInvalidUserException -> "No pudimos iniciar sesión con esta cuenta"
        is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo"
        is FirebaseNetworkException -> "Sin conexión. Revisa tu internet e inténtalo de nuevo"
        else -> "Ocurrió un error inesperado. Inténtalo de nuevo"
    }
}
