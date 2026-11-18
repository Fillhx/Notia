package co.edu.univalle.notia.features.teacher

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.univalle.notia.R
import co.edu.univalle.notia.core.ui.NotiaBackButton
import co.edu.univalle.notia.ui.theme.InterFontFamily
import co.edu.univalle.notia.ui.theme.NotiaInk
import co.edu.univalle.notia.ui.theme.NotiaPrimary
import co.edu.univalle.notia.ui.theme.NotiaSurface
import co.edu.univalle.notia.ui.theme.NotiaTextSecondary

// Colores del Figma (pantallas del docente y guía de estilo) que aún no están en ui/theme.
internal val ChipDraftBg = Color(0xFFECEDEF)
internal val ChipDraftText = Color(0xFF4B5563)
internal val ChipOkBg = Color(0xFFE3F3EA)
internal val ChipOkText = Color(0xFF1B6E3F)
internal val ChipWarnBg = Color(0xFFFDF0D9)
internal val ChipWarnText = Color(0xFF8A5300)
internal val ChipBadBg = Color(0xFFFDE7E5)
internal val PrimarySoft = Color(0xFFE8EDFB)
internal val GradeError = Color(0xFFB42318)
internal val SectionLabel = Color(0xFF3F4757)
internal val CellBorder = Color(0xFFD6D2C7)
internal val RowDivider = Color(0xFFEFECE5)
internal val EmptyDash = Color(0xFF7A8191)
internal val DisabledText = Color(0xFF9AA0AB)

internal val SectionLabelStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 13.sp,
    letterSpacing = 0.6.sp
)

/** Encabezado de las pantallas de una materia: volver + nombre + organización · periodo. */
@Composable
internal fun SubjectHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        NotiaBackButton(onClick = onBack, icon = painterResource(R.drawable.ic_chevron_left))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = NotiaInk)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = NotiaTextSecondary)
        }
    }
}

/** Chip de estado de la guía de estilo: icono + texto + color (RNF-09). */
@Composable
internal fun StatusChip(@DrawableRes icon: Int, text: String, background: Color, content: Color) {
    Row(
        modifier = Modifier
            .height(28.dp)
            .background(background, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = content
        )
    }
}

@Composable
internal fun ActivityStatusChip(status: ActivityStatus, long: Boolean) {
    when (status) {
        ActivityStatus.DRAFT -> StatusChip(
            icon = R.drawable.ic_pencil,
            text = if (long) "Borrador · solo tú lo ves" else "Borrador",
            background = ChipDraftBg,
            content = ChipDraftText
        )
        ActivityStatus.PUBLISHED -> StatusChip(
            icon = R.drawable.ic_eye_small,
            text = if (long) "Publicada · la ven los estudiantes" else "Publicada",
            background = PrimarySoft,
            content = NotiaPrimary
        )
    }
}

@Composable
internal fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = NotiaSurface,
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge, color = NotiaInk) },
        text = { Text(text = text, style = MaterialTheme.typography.bodyMedium, color = NotiaTextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                Text(text = confirmLabel, style = MaterialTheme.typography.titleMedium, color = NotiaPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                Text(text = "Cancelar", style = MaterialTheme.typography.titleMedium, color = NotiaTextSecondary)
            }
        }
    )
}
