package co.edu.univalle.notia.features.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import co.edu.univalle.notia.core.grades.SubjectStatus
import java.math.BigDecimal

internal const val NO_VALUE = "—"

internal fun formatGrade(grade: BigDecimal?): String = grade?.toPlainString() ?: NO_VALUE

internal fun formatPercent(percent: Int): String = "$percent %"

internal fun SubjectStatus.label(): String = when (this) {
    SubjectStatus.PASSED -> "Aprobado"
    SubjectStatus.FAILED -> "Reprobado"
    SubjectStatus.IN_PROGRESS -> "En curso"
    SubjectStatus.NO_GRADES -> "Sin notas"
}

internal fun SubjectStatus.icon(): ImageVector = when (this) {
    SubjectStatus.PASSED -> Icons.Filled.CheckCircle
    SubjectStatus.FAILED -> Icons.Filled.Close
    SubjectStatus.IN_PROGRESS -> Icons.Filled.Refresh
    SubjectStatus.NO_GRADES -> Icons.Filled.Info
}

@Composable
internal fun SubjectStatus.tint(): Color = when (this) {
    SubjectStatus.PASSED -> MaterialTheme.colorScheme.primary
    SubjectStatus.FAILED -> MaterialTheme.colorScheme.error
    SubjectStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondary
    SubjectStatus.NO_GRADES -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Estado siempre con icono + texto (RNF-09). */
@Composable
internal fun StatusLabel(status: SubjectStatus, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(status.icon(), contentDescription = null, tint = status.tint(), modifier = Modifier.size(20.dp))
        Text(status.label(), style = MaterialTheme.typography.labelLarge)
    }
}

/** Indicador de actividad publicada aún sin calificar, con icono + texto (RNF-09). */
@Composable
internal fun PendingLabel(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            Icons.Filled.DateRange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text("Pendiente", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun GradeStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

/** Mensaje con icono + texto (vacío, error, avisos). */
@Composable
internal fun MessageRow(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
