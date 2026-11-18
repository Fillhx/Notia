package co.edu.univalle.notia.features.teacher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

private enum class TeacherPage { GRADES, SCHEME, HISTORY }

/**
 * Entrada del rol Docente. Por ahora abre directo la materia de ejemplo; la lista de materias del
 * docente viene después. Desde el registro de notas se entra al esquema (RF-10) y al historial (RF-19).
 */
@Composable
fun TeacherScreen(onBack: () -> Unit = {}) {
    val subjectId = FakeTeacherRepository.DEMO_SUBJECT_ID
    var page by rememberSaveable { mutableStateOf(TeacherPage.GRADES) }

    when (page) {
        TeacherPage.GRADES -> GradeEntryScreen(
            subjectId = subjectId,
            onBack = onBack,
            onEditScheme = { page = TeacherPage.SCHEME },
            onOpenHistory = { page = TeacherPage.HISTORY }
        )
        TeacherPage.SCHEME -> EvaluationSchemeScreen(subjectId = subjectId, onBack = { page = TeacherPage.GRADES })
        TeacherPage.HISTORY -> GradeHistoryScreen(subjectId = subjectId, onBack = { page = TeacherPage.GRADES })
    }
}
