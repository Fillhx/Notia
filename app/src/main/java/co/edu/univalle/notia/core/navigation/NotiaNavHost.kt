package co.edu.univalle.notia.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import co.edu.univalle.notia.features.auth.ForgotPasswordScreen
import co.edu.univalle.notia.features.auth.LoginScreen
import co.edu.univalle.notia.features.auth.RegisterScreen
import co.edu.univalle.notia.features.auth.ResetPasswordScreen
import co.edu.univalle.notia.features.coordinator.CoordinatorScreen
import co.edu.univalle.notia.features.organizations.OrganizationsScreen
import co.edu.univalle.notia.features.student.StudentScreen
import co.edu.univalle.notia.features.teacher.TeacherScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
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
            LoginScreen(
                onLoggedIn = {
                    nav.navigate(Routes.ORGANIZATIONS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onForgotPassword = { nav.navigate(Routes.FORGOT_PASSWORD) },
                onCreateAccount = { nav.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onBack = { nav.popBackStack() },
                onRegistered = {
                    nav.navigate(Routes.ORGANIZATIONS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onBack = { nav.popBackStack() },
                onOpenResetLink = { expired ->
                    nav.navigate("reset_password/$expired")
                }
            )
        }
        composable(
            route = "reset_password/{expired}",
            arguments = listOf(
                navArgument("expired") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val expired = backStackEntry.arguments?.getBoolean("expired") ?: false
            ResetPasswordScreen(
                linkExpired = expired,
                onGoToLogin = {
                    nav.navigate(Routes.LOGIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onRequestNewLink = {
                    nav.navigate(Routes.FORGOT_PASSWORD) {
                        popUpTo(Routes.FORGOT_PASSWORD) { inclusive = true }
                    }
                }
            )
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
