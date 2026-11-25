package co.edu.univalle.notia.features.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import co.edu.univalle.notia.core.grades.MAX_GRADE
import co.edu.univalle.notia.core.grades.RequiredGrade
import java.math.BigDecimal

/** Simulador de notas (RF-16): notas hipotéticas en actividades pendientes y nota necesaria (RN-05). */
@Composable
fun SimulatorScreen(
    state: StudentLoadState<List<StudentSubjectGrades>>,
    inputs: SimulatorInputs,
    onInputsChange: (SimulatorInputs) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is StudentLoadState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator()
                Text("Cargando simulador…", style = MaterialTheme.typography.bodyMedium)
            }
        }

        is StudentLoadState.Error -> Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            MessageRow(Icons.Filled.Warning, state.message)
        }

        is StudentLoadState.Success -> if (state.data.isEmpty()) {
            Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                MessageRow(Icons.Filled.Info, "No tienes materias inscritas para simular.")
            }
        } else {
            SimulatorContent(state.data, inputs, onInputsChange, modifier)
        }
    }
}

@Composable
private fun SimulatorContent(
    subjects: List<StudentSubjectGrades>,
    inputs: SimulatorInputs,
    onInputsChange: (SimulatorInputs) -> Unit,
    modifier: Modifier,
) {
    val subject = subjects.firstOrNull { it.subject.id == inputs.subjectId } ?: subjects.first()

    val goalInput = parseGradeInput(inputs.goal)
    val goal = (goalInput as? GradeInput.Valid)?.value

    val parsed = subject.simulableActivities.associate { it.id to parseGradeInput(inputs.grades[it.id].orEmpty()) }
    val simulated = parsed.mapNotNull { (id, input) -> (input as? GradeInput.Valid)?.let { id to it.value } }.toMap()
    val hasInvalidInput = parsed.values.any { it is GradeInput.Invalid }
    val projected = subject.withSimulated(simulated)
    val graded = subject.activities.filter { it.grade != null }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Simulador de notas",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
        }
        item {
            SubjectSelector(
                subjects = subjects,
                selected = subject,
                onSelect = { onInputsChange(inputs.copy(subjectId = it)) },
            )
        }
        item { CurrentStateCard(subject) }
        item {
            OutlinedTextField(
                value = inputs.goal,
                onValueChange = { onInputsChange(inputs.copy(goal = it)) },
                label = { Text("Nota meta (0.0 a 5.0)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = goalInput !is GradeInput.Valid,
                trailingIcon = if (goalInput !is GradeInput.Valid) {
                    { Icon(Icons.Filled.Warning, contentDescription = null) }
                } else {
                    null
                },
                supportingText = {
                    Text(
                        when (goalInput) {
                            is GradeInput.Empty -> "Ingresa una meta entre 0.0 y 5.0."
                            is GradeInput.Invalid -> goalInput.error.message
                            is GradeInput.Valid -> "Por defecto 3.0, la nota para aprobar."
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (graded.isNotEmpty()) {
            item {
                Text(
                    "Actividades calificadas",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
            }
            item { GradedActivitiesCard(graded) }
        }

        item {
            Text(
                "Actividades pendientes",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (subject.simulableActivities.isEmpty()) {
            item { MessageRow(Icons.Filled.Info, "No hay actividades publicadas pendientes para simular.") }
        } else {
            items(subject.simulableActivities, key = { it.id }) { activity ->
                val error = (parsed[activity.id] as? GradeInput.Invalid)?.error?.message
                SimulableActivityCard(
                    activity = activity,
                    text = inputs.grades[activity.id].orEmpty(),
                    error = error,
                    onTextChange = { onInputsChange(inputs.withGrade(activity.id, it)) },
                )
            }
        }

        item { ResultCard(projected, goal, hasInvalidInput) }

        item {
            OutlinedButton(
                onClick = { onInputsChange(inputs.reset()) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Restablecer simulación")
            }
        }
    }
}

@Composable
private fun SubjectSelector(
    subjects: List<StudentSubjectGrades>,
    selected: StudentSubjectGrades,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Materia",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(selected.subject.name, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                subjects.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.subject.name) },
                        onClick = {
                            onSelect(option.subject.id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentStateCard(subject: StudentSubjectGrades) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Estado actual", style = MaterialTheme.typography.titleMedium)
            StatusLabel(subject.status)
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                GradeStat("Parcial", formatGrade(subject.partial))
                GradeStat("Definitiva", formatGrade(subject.definitive))
                GradeStat("Evaluado", formatPercent(subject.evaluatedPercent))
            }
        }
    }
}

@Composable
private fun GradedActivitiesCard(activities: List<StudentActivity>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            activities.forEach { activity ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${activity.name} · ${formatPercent(activity.percentage)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(formatGrade(activity.grade), style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun SimulableActivityCard(
    activity: StudentActivity,
    text: String,
    error: String?,
    onTextChange: (String) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(activity.name, style = MaterialTheme.typography.titleSmall)
            Text(
                "Peso: ${formatPercent(activity.percentage)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text("Nota hipotética (0.0 a 5.0)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = error != null,
                trailingIcon = if (error != null) {
                    { Icon(Icons.Filled.Warning, contentDescription = null) }
                } else {
                    null
                },
                supportingText = if (error != null) {
                    { Text(error) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ResultCard(projected: StudentSubjectGrades, goal: BigDecimal?, hasInvalidInput: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Resultado proyectado",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                GradeStat("Parcial", formatGrade(projected.partial))
                GradeStat("Definitiva", formatGrade(projected.definitive))
                GradeStat("Evaluado", formatPercent(projected.evaluatedPercent))
            }
            if (goal != null) {
                RequiredMessage(projected.requiredFor(goal), goal)
            } else {
                StatusMessage(
                    Icons.Filled.Warning,
                    "Corrige la meta para ver la nota necesaria.",
                    MaterialTheme.colorScheme.error,
                )
            }
            if (hasInvalidInput) {
                StatusMessage(
                    Icons.Filled.Warning,
                    "Corrige las notas marcadas: no se incluyen en el cálculo.",
                    MaterialTheme.colorScheme.error,
                )
            }
            if (projected.hiddenWeight > 0) {
                MessageRow(Icons.Filled.Info, "Parte de la nota aún no está publicada; se cuenta como pendiente.")
            }
        }
    }
}

@Composable
private fun RequiredMessage(required: RequiredGrade, goal: BigDecimal) {
    val goalText = formatGrade(goal)
    when (required) {
        is RequiredGrade.Needed -> StatusMessage(
            Icons.Filled.Info,
            "Necesitas ${formatGrade(required.grade)} en promedio en lo que falta para llegar a $goalText.",
            MaterialTheme.colorScheme.secondary,
        )

        RequiredGrade.Achieved -> StatusMessage(
            Icons.Filled.CheckCircle,
            "Meta alcanzada: con estas notas ya llegas a $goalText.",
            MaterialTheme.colorScheme.primary,
        )

        RequiredGrade.OutOfReach -> StatusMessage(
            Icons.Filled.Warning,
            "Fuera de alcance: necesitarías más de ${formatGrade(MAX_GRADE)} en lo que falta para llegar a $goalText.",
            MaterialTheme.colorScheme.error,
        )

        RequiredGrade.NoPending -> StatusMessage(
            Icons.Filled.Info,
            "No quedan actividades pendientes.",
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Mensaje de resultado o validación: siempre icono + texto (RNF-09). */
@Composable
private fun StatusMessage(icon: ImageVector, text: String, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}
