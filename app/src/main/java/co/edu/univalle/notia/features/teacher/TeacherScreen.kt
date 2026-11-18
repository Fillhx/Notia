package co.edu.univalle.notia.features.teacher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Entrada del rol Docente. Por ahora abre directo la materia de ejemplo; la lista de materias del
 * docente viene después. Desde el registro de notas se entra al esquema de evaluación (RF-10).
 */
@Composable
fun TeacherScreen(onBack: () -> Unit = {}) {
    val subjectId = FakeTeacherRepository.DEMO_SUBJECT_ID
    var showScheme by rememberSaveable { mutableStateOf(false) }

    if (showScheme) {
        EvaluationSchemeScreen(subjectId = subjectId, onBack = { showScheme = false })
    } else {
        GradeEntryScreen(subjectId = subjectId, onBack = onBack, onEditScheme = { showScheme = true })
    }
}
