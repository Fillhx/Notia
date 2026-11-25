package co.edu.univalle.notia.features.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.edu.univalle.notia.R
import co.edu.univalle.notia.core.ui.NotiaBackButton
import co.edu.univalle.notia.core.ui.NotiaSecondaryButton
import co.edu.univalle.notia.features.organizations.ActiveOrganization
import co.edu.univalle.notia.ui.theme.NotiaBorder
import co.edu.univalle.notia.ui.theme.NotiaInk
import co.edu.univalle.notia.ui.theme.NotiaPaper
import co.edu.univalle.notia.ui.theme.NotiaPrimary
import co.edu.univalle.notia.ui.theme.NotiaSurface
import co.edu.univalle.notia.ui.theme.NotiaTextSecondary
import kotlinx.coroutines.launch

@Composable
fun TeacherScreen(
    onBack: () -> Unit = {},
    repository: TeacherRepository = remember { FirestoreTeacherRepository() },
) {
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }

    if (selectedSubjectId != null) {
        GradeEntryScreen(
            subjectId = selectedSubjectId!!,
            onBack = { selectedSubjectId = null },
            repository = repository,
        )
        return
    }

    val scope = rememberCoroutineScope()
    val orgName = ActiveOrganization.current?.orgName ?: "Organización"
    var subjects by remember { mutableStateOf<List<TeacherSubject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadSubjects() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                subjects = repository.getSubjects()
            } catch (e: Throwable) {
                errorMessage = (e as? TeacherRepositoryException)?.message
                    ?: "No fue posible cargar tus materias."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(ActiveOrganization.current?.orgId) {
        loadSubjects()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NotiaPaper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            NotiaBackButton(onClick = onBack, icon = painterResource(R.drawable.ic_chevron_left))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Mis materias",
                    style = MaterialTheme.typography.titleLarge,
                    color = NotiaInk,
                )
                Text(
                    text = orgName,
                    style = MaterialTheme.typography.bodySmall,
                    color = NotiaTextSecondary,
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NotiaPrimary)
                }
            }
            errorMessage != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = errorMessage!!, style = MaterialTheme.typography.bodyMedium, color = NotiaInk)
                    Spacer(modifier = Modifier.height(12.dp))
                    NotiaSecondaryButton(text = "Reintentar", onClick = ::loadSubjects)
                }
            }
            subjects.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "No tienes materias asignadas en esta organización.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NotiaTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NotiaSecondaryButton(text = "Actualizar", onClick = ::loadSubjects)
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(subjects, key = { it.id }) { subject ->
                        TeacherSubjectCard(
                            subject = subject,
                            onClick = { selectedSubjectId = subject.id },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherSubjectCard(
    subject: TeacherSubject,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(NotiaSurface)
            .border(1.dp, NotiaBorder, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFE8EDFB), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = subject.name.take(2).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = NotiaPrimary,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subject.name,
                style = MaterialTheme.typography.titleMedium,
                color = NotiaInk,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${subject.organizationName} · ${subject.periodName}",
                style = MaterialTheme.typography.bodySmall,
                color = NotiaTextSecondary,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_down),
            contentDescription = "Abrir materia",
            tint = NotiaTextSecondary,
            modifier = Modifier.size(16.dp),
        )
    }
}