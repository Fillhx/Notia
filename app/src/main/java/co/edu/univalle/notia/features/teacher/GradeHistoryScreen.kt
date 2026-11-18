package co.edu.univalle.notia.features.teacher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.univalle.notia.R
import co.edu.univalle.notia.ui.theme.InterFontFamily
import co.edu.univalle.notia.ui.theme.NotiaBorder
import co.edu.univalle.notia.ui.theme.NotiaInk
import co.edu.univalle.notia.ui.theme.NotiaPaper
import co.edu.univalle.notia.ui.theme.NotiaPrimary
import co.edu.univalle.notia.ui.theme.NotiaSurface
import co.edu.univalle.notia.ui.theme.NotiaTextSecondary
import co.edu.univalle.notia.ui.theme.NotiaTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * RF-19 y RN-08 · Figma "10. Docente · Historial de cambios".
 * Coordinación puede reutilizar esta pantalla desde su pestaña "Historial".
 */
@Composable
fun GradeHistoryScreen(
    subjectId: String,
    onBack: () -> Unit,
) {
    val repo = FakeTeacherRepository
    val subject = remember(subjectId) { repo.subject(subjectId) }
    val changes = remember(subjectId) { repo.history(subjectId) }
    val activities = remember(subjectId) { repo.activities(subjectId) }
    val studentsById = remember(subjectId) { repo.students(subjectId).associateBy { it.id } }
    val activityNames = remember(subjectId) { activities.associate { it.id to it.name } }
    // Solo las actividades publicadas pueden tener cambios registrados.
    val filters = remember(subjectId) { activities.filter { it.status == ActivityStatus.PUBLISHED } }
    var selectedActivityId by rememberSaveable { mutableStateOf<String?>(null) }

    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val groups = groupHistoryByDay(
        changes.filter { selectedActivityId == null || it.activityId == selectedActivityId },
        zone
    )

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NotiaPaper)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                SubjectHeader(
                    title = subject.name,
                    subtitle = "${subject.organizationName} · ${subject.periodName}",
                    onBack = onBack
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Historial de cambios", style = MaterialTheme.typography.headlineMedium, color = NotiaInk)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Cambios hechos a notas ya publicadas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NotiaTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(text = "Todas", selected = selectedActivityId == null) { selectedActivityId = null }
                filters.forEach { activity ->
                    FilterChip(text = activity.name, selected = selectedActivityId == activity.id) {
                        selectedActivityId = activity.id
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                if (groups.isEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    EmptyHistoryCard()
                }
                groups.forEach { (date, dayChanges) ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = historyDayLabel(date, today), style = SectionLabelStyle, color = SectionLabel)
                    Spacer(modifier = Modifier.height(10.dp))
                    HistoryCard(
                        changes = dayChanges,
                        studentsById = studentsById,
                        activityNames = activityNames,
                        zone = zone
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_pencil),
                contentDescription = null,
                tint = NotiaTextSecondary,
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Las notas en borrador no aparecen aquí: solo se registran los cambios después de publicar. " +
                    "Coordinación ve este mismo historial.",
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 17.sp,
                color = NotiaTextSecondary
            )
        }
    }
}

/** Filtro por actividad: 36 dp visibles dentro de un área táctil de 44 dp (RNF-09). */
@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(36.dp)
                .background(if (selected) NotiaPrimary else NotiaSurface, shape)
                .border(1.dp, if (selected) NotiaPrimary else NotiaBorder, shape)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = if (selected) NotiaSurface else NotiaInk
            )
        }
    }
}

@Composable
private fun HistoryCard(
    changes: List<GradeChange>,
    studentsById: Map<String, EnrolledStudent>,
    activityNames: Map<String, String>,
    zone: ZoneId,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NotiaSurface, shape)
            .border(1.dp, NotiaBorder, shape)
            .padding(horizontal = 16.dp)
    ) {
        changes.forEachIndexed { index, change ->
            val student = studentsById[change.studentId]
            val activityName = activityNames[change.activityId] ?: "Actividad eliminada"
            val time = formatHistoryTime(Instant.ofEpochMilli(change.timestamp).atZone(zone).toLocalTime())
            val oldText = formatHistoryGrade(change.oldValue)
            val newText = formatHistoryGrade(change.newValue)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${student?.name ?: "Estudiante"}, $activityName: " +
                            "de $oldText a $newText, por ${change.author} a las $time"
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudentAvatar(initials = student?.initials ?: "?")
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student?.name ?: "Estudiante",
                        style = MaterialTheme.typography.titleMedium,
                        color = NotiaInk
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "$activityName · ${change.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NotiaTextSecondary,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = oldText,
                            style = TextStyle(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                textDecoration = if (change.oldValue != null) TextDecoration.LineThrough else null
                            ),
                            color = EmptyDash
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_right),
                            contentDescription = null,
                            tint = NotiaTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = newText,
                            style = TextStyle(
                                fontFamily = InterFontFamily,
                                fontWeight = if (change.newValue != null) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 16.sp
                            ),
                            color = if (change.newValue != null) NotiaInk else EmptyDash
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(text = time, style = MaterialTheme.typography.bodySmall, color = NotiaTextSecondary)
                }
            }
            if (index < changes.lastIndex) HorizontalDivider(thickness = 1.dp, color = RowDivider)
        }
    }
}

@Composable
private fun EmptyHistoryCard() {
    val shape = RoundedCornerShape(18.dp)
    Text(
        text = "Todavía no hay cambios en notas publicadas.",
        style = MaterialTheme.typography.bodyMedium,
        color = NotiaTextSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(NotiaSurface)
            .border(1.dp, NotiaBorder, shape)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun GradeHistoryScreenPreview() {
    NotiaTheme {
        Surface(color = NotiaPaper) {
            GradeHistoryScreen(subjectId = FakeTeacherRepository.DEMO_SUBJECT_ID, onBack = {})
        }
    }
}
