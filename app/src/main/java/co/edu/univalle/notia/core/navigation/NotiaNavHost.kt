package co.edu.univalle.notia.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import co.edu.univalle.notia.features.auth.LoginScreen
import co.edu.univalle.notia.features.coordinator.CoordinatorScreen
import co.edu.univalle.notia.features.organizations.OrganizationsScreen
import co.edu.univalle.notia.features.student.StudentScreen
import co.edu.univalle.notia.features.teacher.TeacherScreen

object Routes {
    const val LOGIN = "login"
    const val ORGANIZATIONS = "organizations"
    const val COORDINATOR = "coordinator"
    const val TEACHER = "teacher"
    const val STUDENT = "student"
}

/**
 * Navegación de primer nivel: login → organizaciones → pantalla del rol.
 * Cada integrante construye la navegación interna de su área dentro de su carpeta en `features/`.
 */
@Composable
fun NotiaNavHost() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(onLoggedIn = { nav.navigate(Routes.ORGANIZATIONS) })
        }
        composable(Routes.ORGANIZATIONS) {
            OrganizationsScreen(
                onEnterAsCoordinator = { nav.navigate(Routes.COORDINATOR) },
                onEnterAsTeacher = { nav.navigate(Routes.TEACHER) },
                onEnterAsStudent = { nav.navigate(Routes.STUDENT) },
            )
        }
        composable(Routes.COORDINATOR) { CoordinatorScreen() }
        composable(Routes.TEACHER) { TeacherScreen() }
        composable(Routes.STUDENT) { StudentScreen() }
    }
}
