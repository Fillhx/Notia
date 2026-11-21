package co.edu.univalle.notia.features.organizations

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.univalle.notia.core.ui.PlaceholderScreen
import co.edu.univalle.notia.features.auth.AuthViewModel

@Composable
fun OrganizationsScreen(
    onEnterAsCoordinator: () -> Unit,
    onEnterAsTeacher: () -> Unit,
    onEnterAsStudent: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    PlaceholderScreen(
        title = "Organizaciones",
        requirements = "RF-03, RF-04, RF-05",
        // Botones temporales para recorrer cada rol; se borran al implementar RF-05.
        actions = listOf(
            "[demo] Coordinación" to onEnterAsCoordinator,
            "[demo] Docente" to onEnterAsTeacher,
            "[demo] Estudiante" to onEnterAsStudent,
            "Cerrar sesión" to { viewModel.logout(); onLoggedOut() },
        ),
    )
}
