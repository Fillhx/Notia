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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.edu.univalle.notia.core.ui.NotiaBackButton
import co.edu.univalle.notia.core.ui.NotiaPrimaryButton
import co.edu.univalle.notia.core.ui.NotiaTextField
import co.edu.univalle.notia.ui.theme.NotiaInk
import co.edu.univalle.notia.ui.theme.NotiaPaper
import co.edu.univalle.notia.ui.theme.NotiaPrimary
import co.edu.univalle.notia.ui.theme.NotiaTextSecondary
import co.edu.univalle.notia.ui.theme.NotiaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onOpenResetLink: (expired: Boolean) -> Unit = {}
) {
    var emailSent by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    fun handleSendLink() {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            emailError = "Ingresa tu correo"
        } else if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            emailError = "El correo no tiene un formato válido"
        } else {
            emailError = null
            loading = true
            coroutineScope.launch {
                delay(1000)
                loading = false
                emailSent = true
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

        if (!emailSent) {
            // ESTADO A — Solicitar enlace
            Text(
                text = "Recupera tu contraseña",
                style = MaterialTheme.typography.headlineMedium,
                color = NotiaInk
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Ingresa el correo con el que te registraste y te enviaremos un enlace para crear una nueva contraseña.",
                style = MaterialTheme.typography.bodyMedium,
                color = NotiaTextSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

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

            Spacer(modifier = Modifier.height(24.dp))

            NotiaPrimaryButton(
                text = "Enviar enlace",
                onClick = { handleSendLink() },
                loading = loading
            )
        } else {
            // ESTADO B — Enlace enviado
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(color = Color(0xFFE8ECF7), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MarkEmailRead,
                        contentDescription = null,
                        tint = NotiaPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Revisa tu correo",
                style = MaterialTheme.typography.headlineMedium,
                color = NotiaInk,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = buildAnnotatedString {
                    append("Enviamos un enlace de recuperación a ")
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            color = NotiaInk
                        )
                    ) {
                        append(email.trim())
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = NotiaTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Tarjeta informativa
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color(0xFFFDF0D5), shape = RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF8A5A00),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "El enlace es de un solo uso y expira en 15 minutos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8A5A00)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            NotiaPrimaryButton(
                text = "Volver al ingreso",
                onClick = onBack
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clickable { onOpenResetLink(false) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "[demo] Abrir enlace del correo",
                        style = MaterialTheme.typography.labelSmall,
                        color = NotiaTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Box(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clickable { onOpenResetLink(true) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "[demo] Abrir enlace expirado",
                        style = MaterialTheme.typography.labelSmall,
                        color = NotiaTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun ForgotPasswordScreenPreview() {
    NotiaTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = NotiaPaper
        ) {
            ForgotPasswordScreen(onBack = {})
        }
    }
}
