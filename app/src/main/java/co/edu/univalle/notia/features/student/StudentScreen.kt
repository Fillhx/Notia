package co.edu.univalle.notia.features.student

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.CancellationException

enum class StudentTab(val label: String, val icon: ImageVector, val placeholderText: String) {
    SUBJECTS("Materias", Icons.AutoMirrored.Filled.List, ""),
    SIMULATOR("Simulador", Icons.Filled.Build, ""),
    NOTICES("Avisos", Icons.Filled.Notifications, "Aquí verás las notas publicadas o editadas por tus docentes."),
    PROFILE("Perfil", Icons.Filled.Person, "Aquí verás tus datos de estudiante."),
}

/**
 * Punto de entrada del módulo estudiante (Routes.STUDENT).
 * Materias es el destino inicial: materia → detalle de notas queda a 1 toque (RNF-01).
 */
@Composable
fun StudentScreen(
    repository: StudentRepository = remember { FakeStudentRepository() },
) {
    var tab by rememberSaveable { mutableStateOf(StudentTab.SUBJECTS) }
    var selectedSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var simulatorInputs by rememberSaveable { mutableStateOf(SimulatorInputs()) }

    val subjectsState by produceState<StudentLoadState<List<StudentSubjectGrades>>>(
        initialValue = StudentLoadState.Loading,
        key1 = repository,
    ) {
        value = try {
            StudentLoadState.Success(repository.getSubjects())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            StudentLoadState.Error("No se pudieron cargar tus materias. Inténtalo de nuevo.")
        }
    }

    val detail = (subjectsState as? StudentLoadState.Success)
        ?.data
        ?.firstOrNull { it.subject.id == selectedSubjectId }

    BackHandler(enabled = tab == StudentTab.SUBJECTS && selectedSubjectId != null) {
        selectedSubjectId = null
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                StudentTab.values().forEach { destination ->
                    NavigationBarItem(
                        selected = tab == destination,
                        onClick = { tab = destination },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (tab) {
                StudentTab.SUBJECTS -> if (detail != null) {
                    SubjectDetailScreen(grades = detail, onBack = { selectedSubjectId = null })
                } else {
                    SubjectsScreen(
                        state = subjectsState,
                        onSubjectClick = { selectedSubjectId = it },
                    )
                }

                StudentTab.SIMULATOR -> SimulatorScreen(
                    state = subjectsState,
                    inputs = simulatorInputs,
                    onInputsChange = { simulatorInputs = it },
                )

                else -> PlaceholderScreen(tab)
            }
        }
    }
}
