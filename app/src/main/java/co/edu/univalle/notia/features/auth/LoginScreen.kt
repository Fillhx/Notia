package co.edu.univalle.notia.features.auth

import androidx.compose.runtime.Composable
import co.edu.univalle.notia.core.ui.PlaceholderScreen

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    PlaceholderScreen(
        title = "Cuenta",
        requirements = "RF-01, RF-02",
        actions = listOf("Entrar (temporal)" to onLoggedIn),
    )
}
