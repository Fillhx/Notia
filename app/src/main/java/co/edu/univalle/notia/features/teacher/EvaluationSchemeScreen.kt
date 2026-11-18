package co.edu.univalle.notia.features.teacher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.univalle.notia.R
import co.edu.univalle.notia.core.ui.NotiaPrimaryButton
import co.edu.univalle.notia.core.ui.NotiaTextField
import co.edu.univalle.notia.ui.theme.InterFontFamily
import co.edu.univalle.notia.ui.theme.NotiaBorder
import co.edu.univalle.notia.ui.theme.NotiaInk
import co.edu.univalle.notia.ui.theme.NotiaPaper
import co.edu.univalle.notia.ui.theme.NotiaPrimary
import co.edu.univalle.notia.ui.theme.NotiaSurface
import co.edu.univalle.notia.ui.theme.NotiaTextSecondary
import co.edu.univalle.notia.ui.theme.NotiaTheme
import kotlinx.coroutines.launch

/** Una actividad mientras se edita el esquema; los cambios se guardan juntos con "Guardar esquema". */
private class SchemeRow(
    val key: Int,
    val id: String?,
    val status: ActivityStatus?,
    val gradeCount: Int,
    val originalName: String?,
    val originalPercent: Int?,
    name: String,
    percentText: String,
) {
    var name by mutableStateOf(name)
    var percentText by mutableStateOf(percentText)

    val percent: PercentInput get() = parsePercent(percentText)
    val isPublished: Boolean get() = status == ActivityStatus.PUBLISHED
    val canDelete: Boolean get() = gradeCount == 0
    val isChanged: Boolean
        get() = id == null || name != originalName || percentText != originalPercent?.toString()
}

private class SchemeEditorState(
    private val repo: FakeTeacherRepository,
    private val subjectId: String,
) {
    val subject = repo.subject(subjectId)
    val rows = mutableStateListOf<SchemeRow>()
    private var removedSaved by mutableStateOf(0)
    private var nextKey = 0

    init {
        load()
    }

    private fun load() {
        rows.clear()
        removedSaved = 0
        repo.activities(subjectId).forEach { activity ->
            rows += SchemeRow(
                key = nextKey++,
                id = activity.id,
                status = activity.status,
                gradeCount = repo.gradeCount(subjectId, activity.id),
                originalName = activity.name,
                originalPercent = activity.percentage,
                name = activity.name,
                percentText = activity.percentage.toString()
            )
        }
    }

    val total: SchemeTotal
        get() = schemeTotal(rows.map { (it.percent as? PercentInput.Valid)?.value ?: 0 })
    val hasInvalidPercent: Boolean get() = rows.any { it.percent !is PercentInput.Valid }
    val isDirty: Boolean get() = removedSaved > 0 || rows.any { it.isChanged }
    val canSave: Boolean get() = isDirty && !hasInvalidPercent && total is SchemeTotal.Complete

    /** Actividades publicadas cuyo porcentaje cambió: cambian la definitiva de los estudiantes. */
    val publishedPercentChanges: List<SchemeRow>
        get() = rows.filter { row ->
            val value = (row.percent as? PercentInput.Valid)?.value
            row.isPublished && value != null && value != row.originalPercent
        }

    fun totalWithout(row: SchemeRow?): Int =
        rows.filter { it !== row }.sumOf { (it.percent as? PercentInput.Valid)?.value ?: 0 }

    fun namesExcept(row: SchemeRow?): List<String> = rows.filter { it !== row }.map { it.name }

    fun add(name: String, percent: Int) {
        rows += SchemeRow(nextKey++, null, null, 0, null, null, name.trim(), percent.toString())
    }

    fun update(row: SchemeRow, name: String, percent: Int) {
        row.name = name.trim()
        row.percentText = percent.toString()
    }

    fun delete(row: SchemeRow) {
        check(row.canDelete) { "No se puede eliminar una actividad con notas" }
        if (row.id != null) removedSaved++
        rows.remove(row)
    }

    fun save() {
        repo.saveScheme(
            subjectId,
            rows.map { SchemeEntry(it.id, it.name, (it.percent as PercentInput.Valid).value) }
        )
        load()
    }
}

private sealed interface SchemeDialog {
    data object ConfirmPublishedChanges : SchemeDialog
    data object DiscardChanges : SchemeDialog
}

/** RF-10 y RN-03 · Figma "7. Docente · Esquema de evaluación" y "8. Docente · Editar actividad". */
@Composable
fun EvaluationSchemeScreen(
    subjectId: String,
    onBack: () -> Unit,
) {
    val state = remember(subjectId) { SchemeEditorState(FakeTeacherRepository, subjectId) }
    var dialog by remember { mutableStateOf<SchemeDialog?>(null) }
    // null = hoja cerrada; Adding = nueva actividad; Editing = actividad existente.
    var sheet by remember { mutableStateOf<SheetTarget?>(null) }

    fun handleBack() {
        if (state.isDirty) dialog = SchemeDialog.DiscardChanges else onBack()
    }

    fun handleSave() {
        if (state.publishedPercentChanges.isNotEmpty()) {
            dialog = SchemeDialog.ConfirmPublishedChanges
        } else {
            state.save()
            onBack()
        }
    }

    BackHandler(onBack = ::handleBack)

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
                onBack = ::handleBack
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Esquema de evaluación", style = MaterialTheme.typography.headlineMedium, color = NotiaInk)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Actividades de la materia y cuánto vale cada una.",
                style = MaterialTheme.typography.bodyMedium,
                color = NotiaTextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            SchemeSummaryCard(total = state.total)
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ACTIVIDADES (${state.rows.size})",
                    style = SectionLabelStyle,
                    color = SectionLabel,
                    modifier = Modifier.weight(1f)
                )
                Text(text = "Toca una para editarla", style = MaterialTheme.typography.bodySmall, color = NotiaTextSecondary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (state.rows.isNotEmpty()) {
                ActivitiesCard(
                    rows = state.rows,
                    onOpen = { sheet = SheetTarget.Editing(it) },
                    onPercentChange = { row, text -> row.percentText = sanitizePercentText(text) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            AddActivityButton(onClick = { sheet = SheetTarget.Adding })
            Spacer(modifier = Modifier.height(16.dp))
        }

        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp)) {
            NotiaPrimaryButton(text = "Guardar esquema", onClick = ::handleSave, enabled = state.canSave)
            Spacer(modifier = Modifier.height(16.dp))
            val (helper, color) = when {
                state.hasInvalidPercent -> "Revisa los porcentajes marcados en rojo." to GradeError
                state.total !is SchemeTotal.Complete ->
                    "Los porcentajes deben sumar exactamente 100 % para guardar." to ChipWarnText
                !state.isDirty -> "El esquema está completo." to NotiaTextSecondary
                else -> "Listo para guardar." to NotiaTextSecondary
            }
            Text(
                text = helper,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    sheet?.let { target ->
        val row = (target as? SheetTarget.Editing)?.row
        ActivitySheet(
            row = row,
            otherNames = state.namesExcept(row),
            otherTotal = state.totalWithout(row),
            onSave = { name, percent ->
                if (row == null) state.add(name, percent) else state.update(row, name, percent)
            },
            onDelete = row?.let { { state.delete(it) } },
            onDismiss = { sheet = null }
        )
    }

    when (dialog) {
        SchemeDialog.ConfirmPublishedChanges -> ConfirmDialog(
            title = "¿Cambiar porcentajes publicados?",
            text = "Cambia la definitiva parcial que ven los estudiantes en: " +
                state.publishedPercentChanges.joinToString { row ->
                    "${row.name} (${row.originalPercent} % → ${row.percentText} %)"
                } + ".",
            confirmLabel = "Guardar",
            onConfirm = {
                dialog = null
                state.save()
                onBack()
            },
            onDismiss = { dialog = null }
        )
        SchemeDialog.DiscardChanges -> ConfirmDialog(
            title = "¿Descartar cambios?",
            text = "Los cambios del esquema todavía no están guardados.",
            confirmLabel = "Descartar",
            onConfirm = {
                dialog = null
                onBack()
            },
            onDismiss = { dialog = null }
        )
        null -> Unit
    }
}

private sealed interface SheetTarget {
    data object Adding : SheetTarget
    data class Editing(val row: SchemeRow) : SheetTarget
}

@Composable
private fun SchemeSummaryCard(total: SchemeTotal) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NotiaSurface, shape)
            .border(1.dp, NotiaBorder, shape)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)
    ) {
        Text(text = "Total del esquema", style = MaterialTheme.typography.labelSmall, color = NotiaTextSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Row {
            Text(
                text = "${total.total} %",
                style = MaterialTheme.typography.displaySmall,
                color = NotiaInk,
                modifier = Modifier.alignByBaseline()
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "de 100 %",
                style = MaterialTheme.typography.bodyMedium,
                color = NotiaTextSecondary,
                modifier = Modifier.alignByBaseline()
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(RowDivider)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(total.total.coerceIn(0, 100) / 100f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (total is SchemeTotal.Exceeds) GradeError else NotiaPrimary)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        when (total) {
            is SchemeTotal.Complete -> StatusChip(R.drawable.ic_check, "Suma 100 %", ChipOkBg, ChipOkText)
            is SchemeTotal.Missing ->
                StatusChip(R.drawable.ic_warning, "Faltan ${total.missing} % para llegar a 100 %", ChipWarnBg, ChipWarnText)
            is SchemeTotal.Exceeds ->
                StatusChip(R.drawable.ic_close_chip, "Sobran ${total.extra} %: el máximo es 100 %", ChipBadBg, GradeError)
        }
    }
}

@Composable
private fun ActivitiesCard(
    rows: List<SchemeRow>,
    onOpen: (SchemeRow) -> Unit,
    onPercentChange: (SchemeRow, String) -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(NotiaSurface)
            .border(1.dp, NotiaBorder, shape)
    ) {
        rows.forEachIndexed { index, row ->
            val error = (row.percent as? PercentInput.Invalid)?.message
                ?: if (row.percent is PercentInput.Empty) "Escribe el porcentaje" else null
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clickable { onOpen(row) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = row.name, style = MaterialTheme.typography.titleMedium, color = NotiaInk)
                    Spacer(modifier = Modifier.height(3.dp))
                    ActivityMeta(row)
                }
                Spacer(modifier = Modifier.width(12.dp))
                PercentCell(
                    text = row.percentText,
                    isError = error != null,
                    contentDescription = "Porcentaje de ${row.name}",
                    onValueChange = { onPercentChange(row, it) }
                )
            }
            if (error != null) {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) { GradeErrorLine(message = error, startPadding = 0.dp) }
            }
            if (index < rows.lastIndex) {
                HorizontalDivider(thickness = 1.dp, color = RowDivider, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun ActivityMeta(row: SchemeRow) {
    val notes = if (row.gradeCount == 1) "1 nota" else "${row.gradeCount} notas"
    val (icon, text) = when {
        row.id == null -> null to "Nueva · sin guardar"
        row.gradeCount == 0 -> null to "Sin notas"
        row.isPublished -> R.drawable.ic_eye_small to "Publicada · $notes"
        else -> R.drawable.ic_pencil to "Borrador · $notes"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = if (row.isPublished) NotiaPrimary else ChipDraftText,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = NotiaTextSecondary)
    }
}

/** Muestra " %" después del número sin que sea parte del texto editable. */
private val PercentSuffix = VisualTransformation { text ->
    if (text.isEmpty()) {
        TransformedText(text, OffsetMapping.Identity)
    } else {
        TransformedText(
            AnnotatedString(text.text + " %"),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = offset
                override fun transformedToOriginal(offset: Int) = offset.coerceAtMost(text.length)
            }
        )
    }
}

/** Celda de porcentaje: mismo estilo que la celda de nota (guía de estilo). */
@Composable
private fun PercentCell(
    text: String,
    isError: Boolean,
    contentDescription: String,
    onValueChange: (String) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)

    BasicTextField(
        value = text,
        onValueChange = onValueChange,
        singleLine = true,
        visualTransformation = PercentSuffix,
        cursorBrush = SolidColor(NotiaPrimary),
        textStyle = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = if (isError) GradeError else NotiaInk
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = Modifier
            .size(width = 72.dp, height = 44.dp)
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
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                if (text.isEmpty() && !focused) {
                    Text(
                        text = "—",
                        style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp),
                        color = EmptyDash
                    )
                }
                inner()
            }
        }
    )
}

@Composable
private fun AddActivityButton(onClick: () -> Unit) {
    val radius = 14.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(radius))
            .drawBehind {
                drawRoundRect(
                    color = NotiaPrimary,
                    cornerRadius = CornerRadius(radius.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))
                    )
                )
            }
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = null,
            tint = NotiaPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Agregar actividad", style = MaterialTheme.typography.titleMedium, color = NotiaPrimary)
    }
}

/** Hoja para agregar o editar una actividad (Figma "8. Docente · Editar actividad"). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivitySheet(
    row: SchemeRow?,
    otherNames: List<String>,
    otherTotal: Int,
    onSave: (name: String, percent: Int) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(row?.name.orEmpty()) }
    var percentText by remember { mutableStateOf(row?.percentText.orEmpty()) }
    var showErrors by remember { mutableStateOf(false) }

    val nameError = validateActivityName(name, otherNames)
    val percent = parsePercent(percentText)
    val percentError = when (percent) {
        is PercentInput.Invalid -> percent.message
        PercentInput.Empty -> "Escribe el porcentaje"
        is PercentInput.Valid -> null
    }

    fun close(then: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            then()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NotiaSurface,
        scrimColor = Color(0x7314213D),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(CellBorder, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)
        ) {
            Text(
                text = if (row == null) "Nueva actividad" else "Editar actividad",
                style = MaterialTheme.typography.titleLarge,
                color = NotiaInk
            )
            if (row?.status != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ActivityStatusChip(status = row.status, long = false)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (row.gradeCount == 1) "1 nota registrada" else "${row.gradeCount} notas registradas",
                        style = MaterialTheme.typography.bodySmall,
                        color = NotiaTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            NotiaTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                label = "Nombre",
                isError = showErrors && nameError != null,
                errorMessage = nameError
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Porcentaje", style = MaterialTheme.typography.labelSmall, color = NotiaTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PercentField(
                    text = percentText,
                    isError = showErrors && percentError != null,
                    onValueChange = { percentText = sanitizePercentText(it) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                PercentHint(row = row, percent = percent, otherTotal = otherTotal)
            }
            if (showErrors && percentError != null) {
                GradeErrorLine(message = percentError, startPadding = 0.dp)
            }

            if (row?.isPublished == true) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ChipWarnBg, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_warning),
                        contentDescription = null,
                        tint = ChipWarnText,
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Esta actividad ya está publicada. Cambiar el porcentaje cambia la definitiva " +
                            "parcial de los estudiantes; te pediremos confirmar antes de guardar.",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 17.sp,
                        color = ChipWarnText
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            NotiaPrimaryButton(
                text = if (row == null) "Agregar actividad" else "Guardar cambios",
                onClick = {
                    if (nameError != null || percent !is PercentInput.Valid) {
                        showErrors = true
                    } else {
                        close { onSave(name, percent.value) }
                    }
                }
            )

            if (row != null && onDelete != null) {
                Spacer(modifier = Modifier.height(8.dp))
                DeleteActivityButton(enabled = row.canDelete, onClick = { close(onDelete) })
                if (!row.canDelete) {
                    Text(
                        text = "No se puede eliminar porque ya tiene notas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NotiaTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun PercentField(text: String, isError: Boolean, onValueChange: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    BasicTextField(
        value = text,
        onValueChange = onValueChange,
        singleLine = true,
        cursorBrush = SolidColor(NotiaPrimary),
        textStyle = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = if (isError) GradeError else NotiaInk
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        modifier = Modifier
            .size(width = 120.dp, height = 52.dp)
            .background(NotiaSurface, shape)
            .border(
                width = if (focused && !isError) 2.dp else 1.dp,
                color = when {
                    isError -> GradeError
                    focused -> NotiaPrimary
                    else -> NotiaBorder
                },
                shape = shape
            )
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = "Porcentaje" },
        decorationBox = { inner ->
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) { inner() }
                Text(text = "%", style = MaterialTheme.typography.bodyLarge, color = NotiaTextSecondary)
            }
        }
    )
}

@Composable
private fun PercentHint(row: SchemeRow?, percent: PercentInput, otherTotal: Int) {
    val value = (percent as? PercentInput.Valid)?.value ?: return
    val before = row?.originalPercent?.takeIf { it != value }
    Text(
        text = buildAnnotatedString {
            if (before != null) append("Antes $before %. ")
            append(if (row == null) "Con esta actividad el esquema suma " else "Con este cambio el esquema suma ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = NotiaInk)) { append("${otherTotal + value} %") }
            append(".")
        },
        style = MaterialTheme.typography.bodySmall,
        lineHeight = 16.sp,
        color = NotiaTextSecondary
    )
}

@Composable
private fun DeleteActivityButton(enabled: Boolean, onClick: () -> Unit) {
    val color = if (enabled) GradeError else DisabledText
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(R.drawable.ic_trash), contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Eliminar actividad", style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun EvaluationSchemeScreenPreview() {
    NotiaTheme {
        Surface(color = NotiaPaper) {
            EvaluationSchemeScreen(subjectId = FakeTeacherRepository.DEMO_SUBJECT_ID, onBack = {})
        }
    }
}
