package co.edu.univalle.notia.features.teacher

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

private enum class TeacherPage { GRADES, SCHEME, HISTORY }

/**
 * Entrada del rol Docente: "Mis materias" → registro de notas de una materia, y desde ahí el
 * esquema (RF-10) y el historial (RF-19). Volver siempre regresa a la pantalla anterior.
 */
@Composable
fun TeacherScreen(onBack: () -> Unit = {}) {
    var subjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var page by rememberSaveable { mutableStateOf(TeacherPage.GRADES) }
    // El esquema también se abre directo desde la lista ("Completar"): al volver, regresa a la lista.
    var schemeFromList by rememberSaveable { mutableStateOf(false) }

    fun backToList() {
        subjectId = null
        page = TeacherPage.GRADES
        schemeFromList = false
    }

    val current = subjectId
    if (current == null) {
        TeacherSubjectsScreen(
            onOpenSubject = { id, destination ->
                subjectId = id
                page = if (destination == SubjectDestination.SCHEME) TeacherPage.SCHEME else TeacherPage.GRADES
                schemeFromList = destination == SubjectDestination.SCHEME
            },
            onChangeOrganization = onBack
        )
        return
    }

    BackHandler(enabled = page == TeacherPage.GRADES, onBack = ::backToList)

    when (page) {
        TeacherPage.GRADES -> GradeEntryScreen(
            subjectId = current,
            onBack = ::backToList,
            onEditScheme = { page = TeacherPage.SCHEME },
            onOpenHistory = { page = TeacherPage.HISTORY }
        )
        TeacherPage.SCHEME -> EvaluationSchemeScreen(
            subjectId = current,
            onBack = { if (schemeFromList) backToList() else page = TeacherPage.GRADES }
        )
        TeacherPage.HISTORY -> GradeHistoryScreen(subjectId = current, onBack = { page = TeacherPage.GRADES })
    }
}
