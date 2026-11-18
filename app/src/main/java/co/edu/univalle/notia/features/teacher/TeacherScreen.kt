package co.edu.univalle.notia.features.teacher

import androidx.compose.runtime.Composable

/**
 * Entrada del rol Docente. Por ahora abre directo el registro de notas de la materia de ejemplo;
 * la lista de materias del docente y el esquema de evaluación (RF-10) vienen después.
 */
@Composable
fun TeacherScreen(onBack: () -> Unit = {}) {
    GradeEntryScreen(subjectId = FakeTeacherRepository.DEMO_SUBJECT_ID, onBack = onBack)
}
