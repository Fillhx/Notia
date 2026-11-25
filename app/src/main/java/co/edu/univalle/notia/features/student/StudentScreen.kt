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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

enum class StudentTab(val label: String, val icon: ImageVector) {
    SUBJECTS("Materias", Icons.AutoMirrored.Filled.List),
    SIMULATOR("Simulador", Icons.Filled.Build),
    NOTICES("Avisos", Icons.Filled.Notifications),
    PROFILE("Perfil", Icons.Filled.Person),
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

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

    val noticesState by produceState<StudentLoadState<List<StudentNotice>>>(
        initialValue = StudentLoadState.Loading,
        key1 = repository,
    ) {
        try {
            repository.observeNotices().collect { value = StudentLoadState.Success(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            value = StudentLoadState.Error("No se pudieron cargar tus avisos. Inténtalo de nuevo.")
        }
    }

    val profileState by produceState<StudentLoadState<StudentProfile>>(
        initialValue = StudentLoadState.Loading,
        key1 = repository,
    ) {
        value = try {
            StudentLoadState.Success(repository.getProfile())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            StudentLoadState.Error("No se pudo cargar tu perfil. Inténtalo de nuevo.")
        }
    }

    // Notificaciones locales: la primera lista es la línea base; después se notifican solo avisos nuevos sin leer.
    val notifier = remember(context) { StudentNoticeNotifier(context) }
    val detector = remember { NewNoticeDetector() }
    val notificationSetup = rememberNotificationSetup(notifier)
    LaunchedEffect(noticesState) {
        val notices = (noticesState as? StudentLoadState.Success)?.data ?: return@LaunchedEffect
        detector.newUnread(notices).forEach { notifier.notify(it) }
    }

    val unreadCount = (noticesState as? StudentLoadState.Success)?.data?.count { !it.isRead } ?: 0

    val detail = (subjectsState as? StudentLoadState.Success)
        ?.data
        ?.firstOrNull { it.subject.id == selectedSubjectId }

    // Si el repositorio falla, el Flow de avisos sigue siendo la fuente de verdad de la lista.
    fun runRepo(block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Sin efecto: la UI se mantiene como estaba.
            }
        }
    }

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
                        icon = {
                            if (destination == StudentTab.NOTICES && unreadCount > 0) {
                                BadgedBox(badge = { Badge { Text(unreadCount.toString()) } }) {
                                    Icon(destination.icon, contentDescription = null)
                                }
                            } else {
                                Icon(destination.icon, contentDescription = null)
                            }
                        },
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

                StudentTab.NOTICES -> NoticesScreen(
                    state = noticesState,
                    notificationsEnabled = notificationSetup.enabled,
                    onEnableNotifications = notificationSetup.onEnableClick,
                    onNoticeClick = { notice ->
                        if (!notice.isRead) runRepo { repository.markNoticeRead(notice.id) }
                        selectedSubjectId = notice.subjectId
                        tab = StudentTab.SUBJECTS
                    },
                    onMarkRead = { id -> runRepo { repository.markNoticeRead(id) } },
                    onMarkAllRead = { runRepo { repository.markAllNoticesRead() } },
                )

                StudentTab.PROFILE -> ProfileScreen(state = profileState)
            }
        }
    }
}
