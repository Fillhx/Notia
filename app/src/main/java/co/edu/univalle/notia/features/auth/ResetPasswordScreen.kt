package co.edu.univalle.notia.features.auth

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.edu.univalle.notia.core.ui.NotiaPrimaryButton
import co.edu.univalle.notia.core.ui.NotiaSecondaryButton
import co.edu.univalle.notia.core.ui.NotiaTextField
import co.edu.univalle.notia.ui.theme.ErrorBg
import co.edu.univalle.notia.ui.theme.ErrorText
import co.edu.univalle.notia.ui.theme.NotiaInk
import co.edu.univalle.notia.ui.theme.NotiaPaper
import co.edu.univalle.notia.ui.theme.NotiaPrimary
import co.edu.univalle.notia.ui.theme.NotiaTextSecondary
import co.edu.univalle.notia.ui.theme.NotiaTheme
import co.edu.univalle.notia.ui.theme.SuccessBg
import co.edu.univalle.notia.ui.theme.SuccessText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ResetPasswordScreen(
    linkExpired: Boolean,
    onGoToLogin: () -> Unit,
    onRequestNewLink: () -> Unit
) {
    var passwordChanged by rememberSaveable { mutableStateOf(false) }

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

        when {
            linkExpired -> {
                LinkExpiredContent(
                    onRequestNewLink = onRequestNewLink,
                    onGoToLogin = onGoToLogin
                )
            }
            passwordChanged -> {
                PasswordChangedContent(
                    onGoToLogin = onGoToLogin
                )
            }
            else -> {
                ResetPasswordFormContent(
                    onPasswordChanged = { passwordChanged = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun LinkExpiredContent(
    onRequestNewLink: () -> Unit,
    onGoToLogin: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(color = ErrorBg, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.LinkOff,
                contentDescription = null,
                tint = ErrorText,
                modifier = Modifier.size(36.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Este enlace ya no es válido",
        style = MaterialTheme.typography.headlineMedium,
        color = NotiaInk,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "Los enlaces de recuperación expiran a los 15 minutos o después de usarse una vez. Solicita uno nuevo para continuar.",
        style = MaterialTheme.typography.bodyMedium,
        color = NotiaTextSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(28.dp))

    NotiaPrimaryButton(
        text = "Solicitar nuevo enlace",
        onClick = onRequestNewLink
    )

    Spacer(modifier = Modifier.height(12.dp))

    NotiaSecondaryButton(
        text = "Volver al ingreso",
        onClick = onGoToLogin
    )
}

@Composable
private fun ResetPasswordFormContent(
    onPasswordChanged: () -> Unit
) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }

    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmPasswordError by rememberSaveable { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val hasMinLength = password.length >= 8
    val hasUppercase = password.any { it.isUpperCase() }
    val hasNumber = password.any { it.isDigit() }

    fun handleSavePassword() {
        var hasError = false

        if (!(hasMinLength && hasUppercase && hasNumber)) {
            passwordError = "La contraseña no cumple los requisitos"
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
                onPasswordChanged()
            }
        }
    }

    Text(
        text = "Crea una nueva contraseña",
        style = MaterialTheme.typography.headlineMedium,
        color = NotiaInk
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "Elige una contraseña que no hayas usado antes.",
        style = MaterialTheme.typography.bodyMedium,
        color = NotiaTextSecondary
    )

    Spacer(modifier = Modifier.height(28.dp))

    Column {
        NotiaTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = null
            },
            label = "Nueva contraseña",
            isPassword = true,
            isError = passwordError != null,
            errorMessage = passwordError
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RequirementRow(
                text = "Mínimo 8 caracteres",
                isMet = hasMinLength
            )
            RequirementRow(
                text = "Al menos una letra mayúscula",
                isMet = hasUppercase
            )
            RequirementRow(
                text = "Al menos un número",
                isMet = hasNumber
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
        text = "Guardar contraseña",
        onClick = { handleSavePassword() },
        loading = loading
    )
}

@Composable
private fun RequirementRow(
    text: String,
    isMet: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isMet) SuccessText else NotiaTextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (isMet) SuccessText else NotiaTextSecondary
        )
    }
}

@Composable
private fun PasswordChangedContent(
    onGoToLogin: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(color = SuccessBg, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = SuccessText,
                modifier = Modifier.size(36.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Contraseña actualizada",
        style = MaterialTheme.typography.headlineMedium,
        color = NotiaInk,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "Ya puedes ingresar con tu nueva contraseña.",
        style = MaterialTheme.typography.bodyMedium,
        color = NotiaTextSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(20.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color(0xFFE8ECF7), shape = RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Email,
                contentDescription = null,
                tint = NotiaPrimary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Te enviamos un aviso a tu correo sobre este cambio. Si no fuiste tú, contáctanos de inmediato.",
                style = MaterialTheme.typography.bodySmall,
                color = NotiaInk
            )
        }
    }

    Spacer(modifier = Modifier.height(28.dp))

    NotiaPrimaryButton(
        text = "Ir al ingreso",
        onClick = onGoToLogin
    )
}

@Preview(showBackground = true)
@Composable
private fun LinkExpiredPreview() {
    NotiaTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = NotiaPaper
        ) {
            ResetPasswordScreen(
                linkExpired = true,
                onGoToLogin = {},
                onRequestNewLink = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResetPasswordFormPreview() {
    NotiaTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = NotiaPaper
        ) {
            ResetPasswordScreen(
                linkExpired = false,
                onGoToLogin = {},
                onRequestNewLink = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PasswordChangedPreview() {
    NotiaTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = NotiaPaper
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                PasswordChangedContent(onGoToLogin = {})
            }
        }
    }
}
