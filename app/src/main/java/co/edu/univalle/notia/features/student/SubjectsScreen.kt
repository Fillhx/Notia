package co.edu.univalle.notia.features.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Lista de materias del estudiante (RF-14, RF-15). Tocar una materia abre su detalle (RNF-01). */
@Composable
fun SubjectsScreen(
    state: StudentLoadState<List<StudentSubjectGrades>>,
    onSubjectClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is StudentLoadState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator()
                Text("Cargando materias…", style = MaterialTheme.typography.bodyMedium)
            }
        }

        is StudentLoadState.Error -> Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            MessageRow(Icons.Filled.Warning, state.message)
        }

        is StudentLoadState.Success -> if (state.data.isEmpty()) {
            Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                MessageRow(Icons.Filled.Info, "No tienes materias inscritas.")
            }
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "Mis materias",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                items(state.data, key = { it.subject.id }) { grades ->
                    SubjectCard(grades, onClick = { onSubjectClick(grades.subject.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectCard(grades: StudentSubjectGrades, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(grades.subject.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${grades.subject.code} · ${grades.subject.teacherName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
            StatusLabel(grades.status)
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                GradeStat("Parcial", formatGrade(grades.partial))
                GradeStat("Definitiva", formatGrade(grades.definitive))
                GradeStat("Evaluado", formatPercent(grades.evaluatedPercent))
            }
        }
    }
}
