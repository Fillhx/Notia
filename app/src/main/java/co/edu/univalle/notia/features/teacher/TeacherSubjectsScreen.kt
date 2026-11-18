package co.edu.univalle.notia.features.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

/** A dónde lleva tocar una materia. */
enum class SubjectDestination { GRADES, SCHEME }

private data class SubjectCardData(
    val subject: TeacherSubject,
    val studentCount: Int,
    val publishedPercent: Int,
    val activityCount: Int,
    val pending: SubjectPending,
)

/**
 * Figma "11. Docente · Mis materias". El selector de organización vuelve a "Tus organizaciones"
 * (RF-05); el perfil (cerrar sesión, RF-01) lo conecta el paquete A.
 */
@Composable
fun TeacherSubjectsScreen(
    onOpenSubject: (subjectId: String, destination: SubjectDestination) -> Unit,
    onChangeOrganization: () -> Unit,
    onOpenProfile: () -> Unit = {},
) {
    val repo = FakeTeacherRepository
    // Se recalcula cada vez que se vuelve a esta pantalla (el estado viene del repositorio).
    val cards = remember {
        repo.subjects().map { subject ->
            val activities = repo.activities(subject.id)
            SubjectCardData(
                subject = subject,
                studentCount = repo.students(subject.id).size,
                publishedPercent = publishedPercent(activities),
                activityCount = activities.size,
                pending = subjectPending(activities, activities.associate { it.id to repo.gradeCount(subject.id, it.id) })
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NotiaPaper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OrganizationSelector(
                name = FakeTeacherRepository.ORGANIZATION_NAME,
                detail = "Docente · ${FakeTeacherRepository.PERIOD_NAME}",
                onClick = onChangeOrganization
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(NotiaSurface)
                    .border(1.dp, NotiaBorder, RoundedCornerShape(14.dp))
                    .clickable(onClickLabel = "Perfil", onClick = onOpenProfile),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_profile),
                    contentDescription = "Perfil",
                    tint = NotiaInk,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Mis materias",
            style = MaterialTheme.typography.headlineMedium,
            letterSpacing = (-0.4).sp,
            color = NotiaInk
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Toca una materia para registrar sus notas.",
            style = MaterialTheme.typography.bodyMedium,
            color = NotiaTextSecondary
        )

        if (cards.isEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Todavía no tienes materias asignadas.",
                style = MaterialTheme.typography.bodyMedium,
                color = NotiaTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NotiaSurface, RoundedCornerShape(18.dp))
                    .border(1.dp, NotiaBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            )
        }
        cards.forEach { card ->
            Spacer(modifier = Modifier.height(16.dp))
            SubjectCard(card = card, onOpen = { destination -> onOpenSubject(card.subject.id, destination) })
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
                tint = NotiaTextSecondary,
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Coordinación te asigna las materias. Si falta alguna, pídele que te agregue como docente.",
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 17.sp,
                color = NotiaTextSecondary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun OrganizationSelector(name: String, detail: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .height(50.dp)
            .clip(shape)
            .background(NotiaSurface)
            .border(1.dp, NotiaBorder, shape)
            .clickable(onClickLabel = "Cambiar de organización", onClick = onClick)
            .padding(start = 8.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(NotiaPrimary, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = organizationInitials(name),
                style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                color = NotiaSurface
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = name,
                style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                color = NotiaInk
            )
            Text(
                text = detail,
                style = TextStyle(fontFamily = InterFontFamily, fontSize = 11.sp),
                color = NotiaTextSecondary
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Icon(
            painter = painterResource(R.drawable.ic_chevron_down_small),
            contentDescription = null,
            tint = NotiaInk,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun SubjectCard(card: SubjectCardData, onOpen: (SubjectDestination) -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    // La tarjeta entera hace la acción que muestra su enlace.
    val (destination, actionLabel) = when (card.pending) {
        is SubjectPending.IncompleteScheme -> SubjectDestination.SCHEME to "Completar"
        SubjectPending.AllPublished -> SubjectDestination.GRADES to "Ver notas"
        else -> SubjectDestination.GRADES to "Registrar notas"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(NotiaSurface)
            .border(1.dp, NotiaBorder, shape)
            .clickable(onClickLabel = actionLabel) { onOpen(destination) }
            .padding(16.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${card.subject.name}, ${card.studentCount} estudiantes, " +
                    subjectMetaText(card.publishedPercent, card.activityCount)
            }
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = card.subject.name, style = MaterialTheme.typography.titleMedium, color = NotiaInk)
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subjectMetaText(card.publishedPercent, card.activityCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = NotiaTextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = card.studentCount.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    letterSpacing = (-0.5).sp,
                    color = NotiaInk
                )
                Text(
                    text = if (card.studentCount == 1) "estudiante" else "estudiantes",
                    style = TextStyle(fontFamily = InterFontFamily, fontSize = 11.sp),
                    color = NotiaTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ProgressTrack)
        ) {
            if (card.publishedPercent > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(card.publishedPercent / 100f)
                        .background(NotiaPrimary)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (val pending = card.pending) {
                is SubjectPending.IncompleteScheme ->
                    StatusChip(R.drawable.ic_warning, "Esquema incompleto · ${pending.total} %", ChipWarnBg, ChipWarnText)
                is SubjectPending.DraftWithGrades ->
                    StatusChip(R.drawable.ic_pencil, "${pending.activityName} en borrador", ChipDraftBg, ChipDraftText)
                is SubjectPending.NoGrades ->
                    StatusChip(R.drawable.ic_pencil, "${pending.activityName} sin notas", ChipDraftBg, ChipDraftText)
                SubjectPending.AllPublished ->
                    StatusChip(R.drawable.ic_check, "Todo publicado", ChipOkBg, ChipOkText)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$actionLabel →",
                style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = NotiaPrimary
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun TeacherSubjectsScreenPreview() {
    NotiaTheme {
        Surface(color = NotiaPaper) {
            TeacherSubjectsScreen(onOpenSubject = { _, _ -> }, onChangeOrganization = {})
        }
    }
}
