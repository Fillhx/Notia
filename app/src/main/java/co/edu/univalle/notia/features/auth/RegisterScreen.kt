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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import co.edu.univalle.notia.core.ui.NotiaBackButton
import co.edu.univalle.notia.core.ui.NotiaPrimaryButton
import co.edu.univalle.notia.core.ui.NotiaTextField
import co.edu.univalle.notia.ui.theme.NotiaInk
import co.edu.univalle.notia.ui.theme.NotiaPaper
import co.edu.univalle.notia.ui.theme.NotiaPrimary
import co.edu.univalle.notia.ui.theme.NotiaSurface
import co.edu.univalle.notia.ui.theme.NotiaTextSecondary
import co.edu.univalle.notia.ui.theme.NotiaTheme
import co.edu.univalle.notia.ui.theme.SuccessBg
import co.edu.univalle.notia.ui.theme.SuccessText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onRegistered: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }
    var showSuccessDialog by rememberSaveable { mutableStateOf(false) }

    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmPasswordError by rememberSaveable { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    fun handleRegister() {
        var hasError = false
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()

        if (trimmedName.isEmpty()) {
            nameError = "Ingresa tu nombre"
            hasError = true
        } else {
            nameError = null
        }

        if (trimmedEmail.isEmpty()) {
            emailError = "Ingresa tu correo"
            hasError = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            emailError = "El correo no tiene un formato válido"
            hasError = true
        } else {
            emailError = null
        }

        if (password.length < 8) {
            passwordError = "La contraseña debe tener mínimo 8 caracteres"
            hasError = true
        } else {
            passwordError = null
        }

        if (confirmPassword != password) {
            confirmPasswordError = "Las contraseñas no coinciden"
            hasError = true
        } else {
            confirmPasswordError = null
        }

        if (!hasError) {
            loading = true
            coroutineScope.launch {
                delay(1000)
                loading = false
                showSuccessDialog = true
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
        Spacer(modifier = Modifier.height(16.dp))

        NotiaBackButton(onClick = onBack)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Crea tu cuenta",
            style = MaterialTheme.typography.headlineMedium,
            color = NotiaInk
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Regístrate para consultar tus notas y unirte a tus organizaciones.",
            style = MaterialTheme.typography.bodyMedium,
            color = NotiaTextSecondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        NotiaTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = null
            },
            label = "Nombre completo",
            isError = nameError != null,
            errorMessage = nameError
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        Column {
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
            if (passwordError == null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mínimo 8 caracteres",
                    style = MaterialTheme.typography.labelSmall,
                    color = NotiaTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NotiaTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                confirmPasswordError = null
            },
            label = "Confirmar contraseña",
            isPassword = true,
            isError = confirmPasswordError != null,
            errorMessage = confirmPasswordError
        )

        Spacer(modifier = Modifier.height(28.dp))

        NotiaPrimaryButton(
            text = "Crear cuenta",
            onClick = { handleRegister() },
            loading = loading
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "¿Ya tienes cuenta? ",
                style = MaterialTheme.typography.bodyMedium,
                color = NotiaTextSecondary
            )
            Text(
                text = "Ingresar",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = NotiaPrimary,
                modifier = Modifier.clickable { onBack() }
            )
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            shape = RoundedCornerShape(16.dp),
            containerColor = NotiaSurface,
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color = SuccessBg, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = SuccessText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "¡Cuenta creada!",
                    style = MaterialTheme.typography.titleLarge,
                    color = NotiaInk,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Te enviamos un correo de bienvenida a ${email.trim()}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NotiaTextSecondary,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                NotiaPrimaryButton(
                    text = "Continuar",
                    onClick = {
                        showSuccessDialog = false
                        onRegistered()
                    }
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    NotiaTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = NotiaPaper
        ) {
            RegisterScreen(
                onBack = {},
                onRegistered = {}
            )
        }
    }
}
