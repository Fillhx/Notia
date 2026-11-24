package co.edu.univalle.notia.features.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Detalle de notas de una materia (RF-15). Solo recibe actividades publicadas (RN-06, RNF-05):
 * los borradores nunca llegan hasta aquí.
 */
@Composable
fun SubjectDetailScreen(
    grades: StudentSubjectGrades,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a materias")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    grades.subject.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "${grades.subject.code} · ${grades.subject.teacherName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SummaryCard(grades) }
            item {
                Text(
                    "Actividades publicadas",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
            }
            if (grades.activities.isEmpty()) {
                item { MessageRow(Icons.Filled.Info, "Aún no hay actividades publicadas.") }
            } else {
                items(grades.activities, key = { it.id }) { ActivityCard(it) }
            }
        }
    }
}

@Composable
private fun SummaryCard(grades: StudentSubjectGrades) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusLabel(grades.status)
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                GradeStat("Parcial", formatGrade(grades.partial))
                GradeStat("Definitiva", formatGrade(grades.definitive))
                GradeStat("Evaluado", formatPercent(grades.evaluatedPercent))
            }
            if (grades.definitive == null) {
                MessageRow(
                    Icons.Filled.Info,
                    "La definitiva se calcula cuando todas las actividades estén publicadas y calificadas.",
                )
            }
        }
    }
}

@Composable
private fun ActivityCard(activity: StudentActivity) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(activity.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Peso: ${formatPercent(activity.percentage)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (activity.grade != null) {
                    Text(formatGrade(activity.grade), style = MaterialTheme.typography.titleLarge)
                } else {
                    PendingLabel()
                }
            }
            if (!activity.feedback.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Retroalimentación del docente",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(activity.feedback, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
