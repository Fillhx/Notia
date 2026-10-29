package co.edu.univalle.notia.features.auth

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.edu.univalle.notia.core.ui.NotiaPrimaryButton
import co.edu.univalle.notia.core.ui.NotiaTextField
import co.edu.univalle.notia.ui.theme.NotiaAccent
import co.edu.univalle.notia.ui.theme.NotiaInk
import co.edu.univalle.notia.ui.theme.NotiaPaper
import co.edu.univalle.notia.ui.theme.NotiaPrimary
import co.edu.univalle.notia.ui.theme.NotiaTextSecondary
import co.edu.univalle.notia.ui.theme.NotiaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onForgotPassword: () -> Unit = {},
    onCreateAccount: () -> Unit = {}
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    fun handleLogin() {
        var hasError = false
        val trimmedEmail = email.trim()

        if (trimmedEmail.isEmpty()) {
            emailError = "Ingresa tu correo"
            hasError = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            emailError = "El correo no tiene un formato válido"
            hasError = true
        } else {
            emailError = null
        }

        if (password.isEmpty()) {
            passwordError = "Ingresa tu contraseña"
            hasError = true
        } else {
            passwordError = null
        }

        if (!hasError) {
            loading = true
            coroutineScope.launch {
                delay(1000)
                loading = false
                onLoggedIn()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // 1. Logo
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NotiaPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = NotiaAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Notia",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = NotiaInk
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // 3. Título
        Text(
            text = "Hola de nuevo",
            style = MaterialTheme.typography.headlineMedium,
            color = NotiaInk
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 4. Subtítulo
        Text(
            text = "Ingresa para ver tus notas y tus organizaciones.",
            style = MaterialTheme.typography.bodyMedium,
            color = NotiaTextSecondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 6. NotiaTextField "Correo electrónico"
        NotiaTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = null
            },
            label = "Correo electrónico",
            keyboardType = KeyboardType.Email,
            isError = emailError != null,
            errorMessage = emailError
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 8. NotiaTextField "Contraseña"
        NotiaTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = null
            },
            label = "Contraseña",
            isPassword = true,
            isError = passwordError != null,
            errorMessage = passwordError
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 10. "¿Olvidaste tu contraseña?"
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clickable { onForgotPassword() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = NotiaPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 12. NotiaPrimaryButton "Ingresar"
        NotiaPrimaryButton(
            text = "Ingresar",
            onClick = { handleLogin() },
            loading = loading
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 14. Fila centrada: "¿No tienes cuenta? Crear cuenta"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "¿No tienes cuenta? ",
                style = MaterialTheme.typography.bodyMedium,
                color = NotiaTextSecondary
            )
            Text(
                text = "Crear cuenta",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = NotiaPrimary,
                modifier = Modifier.clickable { onCreateAccount() }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    NotiaTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = NotiaPaper
        ) {
            LoginScreen(onLoggedIn = {})
        }
    }
}
