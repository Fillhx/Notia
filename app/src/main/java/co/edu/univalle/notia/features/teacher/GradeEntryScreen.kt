package co.edu.univalle.notia.features.teacher

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.univalle.notia.R
import co.edu.univalle.notia.core.ui.NotiaBackButton
import co.edu.univalle.notia.core.ui.NotiaPrimaryButton
import co.edu.univalle.notia.core.ui.NotiaSecondaryButton
import co.edu.univalle.notia.ui.theme.InterFontFamily
import co.edu.univalle.notia.ui.theme.NotiaBorder
import co.edu.univalle.notia.ui.theme.NotiaInk
import co.edu.univalle.notia.ui.theme.NotiaPaper
import co.edu.univalle.notia.ui.theme.NotiaPrimary
import co.edu.univalle.notia.ui.theme.NotiaSurface
import co.edu.univalle.notia.ui.theme.NotiaTextSecondary
import co.edu.univalle.notia.ui.theme.NotiaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.math.BigDecimal

// Colores del diseño (pantalla 5 y guía de estilo) que aún no están en ui/theme.
private val ChipDraftBg = Color(0xFFECEDEF)
private val ChipDraftText = Color(0xFF4B5563)
private val PrimarySoft = Color(0xFFE8EDFB)
private val GradeError = Color(0xFFB42318)
private val SectionLabel = Color(0xFF3F4757)
private val CellBorder = Color(0xFFD6D2C7)
private val RowDivider = Color(0xFFEFECE5)
private val EmptyDash = Color(0xFF7A8191)

private const val AUTOSAVE_DELAY_MS = 1500L

/** Estado de la pantalla de registro de notas de una materia. */
private class GradeEntryState(
    private val repo: FakeTeacherRepository,
    private val subjectId: String,
) {
    val subject = repo.subject(subjectId)
    val students = repo.students(subjectId)
    var activities by mutableStateOf(repo.activities(subjectId))
        private set
    var selectedId by mutableStateOf(
        activities.firstOrNull { it.status == ActivityStatus.DRAFT }?.id ?: activities.first().id
    )
        private set

    /** Texto de cada celda, por id de estudiante. */
    val inputs = mutableStateMapOf<String, String>()
    private var savedInputs by mutableStateOf(emptyMap<String, String>())
    var lastSavedAt by mutableStateOf<Long?>(null)
        private set

    init {
        load(selectedId)
    }

    val selected: GradeActivity get() = activities.first { it.id == selectedId }
    val isPublished: Boolean get() = selected.status == ActivityStatus.PUBLISHED
    val parsed: Map<String, GradeInput> get() = inputs.mapValues { parseGrade(it.value) }
    val isDirty: Boolean get() = inputs.toMap() != savedInputs
    val hasErrors: Boolean get() = parsed.values.any { it is GradeInput.Invalid }
    val missingCount: Int get() = parsed.values.count { it is GradeInput.Empty }

    fun load(activityId: String) {
        selectedId = activityId
        val grades = repo.grades(subjectId, activityId)
        inputs.clear()
        students.forEach { inputs[it.id] = grades[it.id]?.toPlainString().orEmpty() }
        savedInputs = inputs.toMap()
        lastSavedAt = null
    }

    /** Notas listas para guardar: vacías se borran, las inválidas se dejan como estaban. */
    private fun gradesToSave(): Map<String, BigDecimal?> = parsed.mapNotNull { (studentId, input) ->
        when (input) {
            GradeInput.Empty -> studentId to null
            is GradeInput.Valid -> studentId to input.value
            is GradeInput.Invalid -> null
        }
    }.toMap()

    fun saveDraft() {
        if (isPublished) return
        repo.saveDraft(subjectId, selectedId, gradesToSave())
        savedInputs = inputs.toMap()
        lastSavedAt = System.currentTimeMillis()
    }

    fun publish() {
        saveDraft()
        repo.publish(subjectId, selectedId)
        activities = repo.activities(subjectId)
    }

    fun savePublishedChanges() {
        repo.updatePublished(subjectId, selectedId, gradesToSave(), FakeTeacherRepository.CURRENT_TEACHER)
        savedInputs = inputs.toMap()
        lastSavedAt = System.currentTimeMillis()
    }
}

private sealed interface PendingDialog {
    data object Publish : PendingDialog
    data object SavePublished : PendingDialog
    data class DiscardAndSwitch(val activityId: String) : PendingDialog
}

/** RF-11, RF-12, RNF-06 · Figma "5. Docente · Registro de notas". */
@Composable
fun GradeEntryScreen(
    subjectId: String,
    onBack: () -> Unit,
) {
    val state = remember(subjectId) { GradeEntryState(FakeTeacherRepository, subjectId) }
    var dialog by remember { mutableStateOf<PendingDialog?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }

    // RNF-06: el borrador se guarda solo, poco después de dejar de escribir.
    LaunchedEffect(state.selectedId) {
        snapshotFlow { state.inputs.toMap() }.collectLatest {
            if (!state.isPublished && state.isDirty) {
                delay(AUTOSAVE_DELAY_MS)
                state.saveDraft()
            }
        }
    }

    fun switchActivity(activityId: String) {
        when {
            activityId == state.selectedId -> Unit
            state.isPublished && state.isDirty -> dialog = PendingDialog.DiscardAndSwitch(activityId)
            else -> {
                state.saveDraft()
                state.load(activityId)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NotiaPaper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            SubjectHeader(
                title = state.subject.name,
                subtitle = "${state.subject.organizationName} · ${state.subject.periodName}",
                onBack = onBack
            )
            Spacer(modifier = Modifier.height(16.dp))
            ActivityCard(
                activities = state.activities,
                selected = state.selected,
                saveStatus = saveStatusText(state.isPublished, state.isDirty, state.lastSavedAt, now),
                onSelect = ::switchActivity
            )
            Spacer(modifier = Modifier.height(16.dp))
            StudentsHeader(count = state.students.size)
            Spacer(modifier = Modifier.height(16.dp))
            StudentsCard(
                students = state.students,
                inputs = state.inputs,
                parsed = state.parsed,
                onValueChange = { studentId, text -> state.inputs[studentId] = sanitizeGradeText(text) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        BottomActions(
            isPublished = state.isPublished,
            isDirty = state.isDirty,
            hasErrors = state.hasErrors,
            missingCount = state.missingCount,
            onSaveDraft = state::saveDraft,
            onPublish = { dialog = PendingDialog.Publish },
            onSavePublished = { dialog = PendingDialog.SavePublished }
        )
    }

    when (val current = dialog) {
        PendingDialog.Publish -> ConfirmDialog(
            title = "¿Publicar ${state.selected.name}?",
            text = buildString {
                if (state.missingCount > 0) append("${missingText(state.missingCount)} sin nota. ")
                append("Los estudiantes verán sus notas y recibirán una notificación.")
            },
            confirmLabel = "Publicar",
            onConfirm = { state.publish(); dialog = null },
            onDismiss = { dialog = null }
        )
        PendingDialog.SavePublished -> ConfirmDialog(
            title = "¿Guardar cambios?",
            text = "Estas notas ya están publicadas: los estudiantes verán el cambio de inmediato, " +
                "recibirán una notificación y el cambio quedará en el historial.",
            confirmLabel = "Guardar",
            onConfirm = { state.savePublishedChanges(); dialog = null },
            onDismiss = { dialog = null }
        )
        is PendingDialog.DiscardAndSwitch -> ConfirmDialog(
            title = "¿Descartar cambios?",
            text = "Hay cambios sin guardar en ${state.selected.name}.",
            confirmLabel = "Descartar",
            onConfirm = { state.load(current.activityId); dialog = null },
            onDismiss = { dialog = null }
        )
        null -> Unit
    }
}

private fun missingText(count: Int) = if (count == 1) "Falta 1 nota" else "Faltan $count notas"

private fun saveStatusText(isPublished: Boolean, isDirty: Boolean, lastSavedAt: Long?, now: Long): String {
    if (isDirty) return if (isPublished) "Cambios sin guardar" else "Guardando…"
    if (lastSavedAt == null) return if (isPublished) "Visible para estudiantes" else "Sin cambios"
    val seconds = ((now - lastSavedAt) / 1000).coerceAtLeast(0)
    return when {
        seconds < 60 -> "Guardado hace $seconds s"
        else -> "Guardado hace ${seconds / 60} min"
    }
}

@Composable
private fun SubjectHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        NotiaBackButton(onClick = onBack, icon = painterResource(R.drawable.ic_chevron_left))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = NotiaInk)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = NotiaTextSecondary)
        }
    }
}

@Composable
private fun ActivityCard(
    activities: List<GradeActivity>,
    selected: GradeActivity,
    saveStatus: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(NotiaSurface)
                .border(1.dp, NotiaBorder, shape)
                .clickable { expanded = true }
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Actividad", style = MaterialTheme.typography.labelSmall, color = NotiaTextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${selected.name} · ${selected.percentage} %",
                        style = MaterialTheme.typography.titleMedium,
                        color = NotiaInk
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = "Cambiar actividad",
                    tint = NotiaInk,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActivityStatusChip(status = selected.status, long = true)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = saveStatus, style = MaterialTheme.typography.bodySmall, color = NotiaTextSecondary)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = NotiaSurface
        ) {
            activities.forEach { activity ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${activity.name} · ${activity.percentage} %",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (activity.id == selected.id) FontWeight.SemiBold else FontWeight.Normal,
                            color = NotiaInk
                        )
                    },
                    trailingIcon = { ActivityStatusChip(status = activity.status, long = false) },
                    onClick = {
                        expanded = false
                        onSelect(activity.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun ActivityStatusChip(status: ActivityStatus, long: Boolean) {
    when (status) {
        ActivityStatus.DRAFT -> StatusChip(
            icon = R.drawable.ic_pencil,
            text = if (long) "Borrador · solo tú lo ves" else "Borrador",
            background = ChipDraftBg,
            content = ChipDraftText
        )
        ActivityStatus.PUBLISHED -> StatusChip(
            icon = R.drawable.ic_eye_small,
            text = if (long) "Publicada · la ven los estudiantes" else "Publicada",
            background = PrimarySoft,
            content = NotiaPrimary
        )
    }
}

/** Chip de estado de la guía de estilo: icono + texto + color (RNF-09). */
@Composable
private fun StatusChip(@DrawableRes icon: Int, text: String, background: Color, content: Color) {
    Row(
        modifier = Modifier
            .height(28.dp)
            .background(background, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = content
        )
    }
}

@Composable
private fun StudentsHeader(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "ESTUDIANTES ($count)",
            style = TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.6.sp
            ),
            color = SectionLabel,
            modifier = Modifier.weight(1f)
        )
        Text(text = "Escala 0.0 – 5.0", style = MaterialTheme.typography.bodySmall, color = NotiaTextSecondary)
    }
}

@Composable
private fun StudentsCard(
    students: List<EnrolledStudent>,
    inputs: Map<String, String>,
    parsed: Map<String, GradeInput>,
    onValueChange: (studentId: String, text: String) -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NotiaSurface, shape)
            .border(1.dp, NotiaBorder, shape)
            .padding(horizontal = 16.dp)
    ) {
        students.forEachIndexed { index, student ->
            val error = (parsed[student.id] as? GradeInput.Invalid)?.message
            StudentGradeRow(
                student = student,
                text = inputs[student.id].orEmpty(),
                isError = error != null,
                onValueChange = { onValueChange(student.id, it) }
            )
            if (error != null) GradeErrorLine(message = error)
            if (index < students.lastIndex) HorizontalDivider(thickness = 1.dp, color = RowDivider)
        }
    }
}

@Composable
private fun StudentGradeRow(
    student: EnrolledStudent,
    text: String,
    isError: Boolean,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(PrimarySoft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = student.initials,
                style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                color = NotiaPrimary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = student.name, style = MaterialTheme.typography.titleMedium, color = NotiaInk)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = student.code, style = MaterialTheme.typography.bodySmall, color = NotiaTextSecondary)
        }
        GradeCell(
            text = text,
            isError = isError,
            contentDescription = "Nota de ${student.name}",
            onValueChange = onValueChange
        )
    }
}

/** Celda de nota de la guía de estilo: normal, vacía ("—") y error. */
@Composable
private fun GradeCell(
    text: String,
    isError: Boolean,
    contentDescription: String,
    onValueChange: (String) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val color = if (isError) GradeError else NotiaInk

    BasicTextField(
        value = text,
        onValueChange = onValueChange,
        singleLine = true,
        cursorBrush = SolidColor(NotiaPrimary),
        textStyle = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = color
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        modifier = Modifier
            .size(width = 64.dp, height = 44.dp)
            .background(NotiaSurface, shape)
            .border(
                width = if (focused && !isError) 2.dp else 1.dp,
                color = when {
                    isError -> GradeError
                    focused -> NotiaPrimary
                    else -> CellBorder
                },
                shape = shape
            )
            .onFocusChanged { focused = it.isFocused }
            .semantics { this.contentDescription = contentDescription },
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                if (text.isEmpty() && !focused) {
                    Text(
                        text = "—",
                        style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp),
                        color = EmptyDash
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun GradeErrorLine(message: String) {
    Row(
        modifier = Modifier.padding(start = 48.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_close_small),
            contentDescription = null,
            tint = GradeError,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = message, style = MaterialTheme.typography.labelSmall, color = GradeError)
    }
}

@Composable
private fun BottomActions(
    isPublished: Boolean,
    isDirty: Boolean,
    hasErrors: Boolean,
    missingCount: Int,
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit,
    onSavePublished: () -> Unit,
) {
    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp)) {
        if (isPublished) {
            NotiaPrimaryButton(
                text = "Guardar cambios",
                onClick = onSavePublished,
                enabled = isDirty && !hasErrors
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NotiaSecondaryButton(
                    text = "Guardar borrador",
                    onClick = onSaveDraft,
                    modifier = Modifier.weight(147f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                )
                NotiaPrimaryButton(
                    text = "Publicar notas",
                    onClick = onPublish,
                    modifier = Modifier.weight(189f),
                    enabled = !hasErrors,
                    leadingIcon = painterResource(R.drawable.ic_eye),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when {
                hasErrors -> "Corrige las notas marcadas en rojo antes de continuar."
                isPublished -> "Las notas están publicadas: los cambios se ven de inmediato y quedan en el historial."
                missingCount > 0 -> "${missingText(missingCount)}. Al publicar, los estudiantes reciben una notificación."
                else -> "Al publicar, los estudiantes reciben una notificación."
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (hasErrors) GradeError else NotiaTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = NotiaSurface,
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge, color = NotiaInk) },
        text = { Text(text = text, style = MaterialTheme.typography.bodyMedium, color = NotiaTextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                Text(text = confirmLabel, style = MaterialTheme.typography.titleMedium, color = NotiaPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                Text(text = "Cancelar", style = MaterialTheme.typography.titleMedium, color = NotiaTextSecondary)
            }
        }
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun GradeEntryScreenPreview() {
    NotiaTheme {
        Surface(color = NotiaPaper) {
            GradeEntryScreen(subjectId = FakeTeacherRepository.DEMO_SUBJECT_ID, onBack = {})
        }
    }
}
