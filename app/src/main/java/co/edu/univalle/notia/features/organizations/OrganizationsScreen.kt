package co.edu.univalle.notia.features.organizations

import androidx.compose.runtime.Composable
import co.edu.univalle.notia.core.ui.PlaceholderScreen

@Composable
fun OrganizationsScreen(
    onEnterAsCoordinator: () -> Unit,
    onEnterAsTeacher: () -> Unit,
    onEnterAsStudent: () -> Unit,
) {
    PlaceholderScreen(
        title = "Organizaciones",
        requirements = "RF-03, RF-04, RF-05",
        // Botones temporales para recorrer cada rol; se borran al implementar RF-05.
        actions = listOf(
            "[demo] Coordinación" to onEnterAsCoordinator,
            "[demo] Docente" to onEnterAsTeacher,
            "[demo] Estudiante" to onEnterAsStudent,
        ),
    )
}
