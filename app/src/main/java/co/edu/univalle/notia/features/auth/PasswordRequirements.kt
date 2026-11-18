package co.edu.univalle.notia.features.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.edu.univalle.notia.ui.theme.NotiaTextSecondary
import co.edu.univalle.notia.ui.theme.SuccessText

/** Regla única de contraseña para registro y restablecimiento. */
private val passwordRules: List<Pair<String, (String) -> Boolean>> = listOf(
    "Mínimo 8 caracteres" to { it.length >= 8 },
    "Al menos una letra mayúscula" to { p -> p.any { it.isUpperCase() } },
    "Al menos un número" to { p -> p.any { it.isDigit() } },
)

fun isValidPassword(password: String): Boolean = passwordRules.all { (_, rule) -> rule(password) }

/** Lista de requisitos que se marcan a medida que se escribe la contraseña. */
@Composable
fun PasswordRequirementsList(password: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        passwordRules.forEach { (text, rule) -> RequirementRow(text = text, isMet = rule(password)) }
    }
}

@Composable
private fun RequirementRow(text: String, isMet: Boolean) {
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
