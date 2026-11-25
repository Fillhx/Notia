package co.edu.univalle.notia.features.organizations

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Roles guardados en Firestore (texto exacto, igual que en firestore.rules). */
object Roles {
    const val COORDINATION = "Coordinación"
    const val TEACHER = "Docente"
    const val STUDENT = "Estudiante"
}

/** Pertenencia del usuario actual a una organización, con su rol en ella. */
data class OrganizationMembership(
    val orgId: String,
    val orgName: String,
    val role: String,
    val inviteCode: String? = null
)

/** Organización activa elegida por el usuario. Las pantallas de cada rol la leen de aquí. */
object ActiveOrganization {
    var current by mutableStateOf<OrganizationMembership?>(null)
}
