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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.launch
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
private const val MAX_TOTAL_PERCENTAGE = 100

/** Estado de la pantalla de registro de notas de una materia. */
private class GradeEntryState(
    private val repo: TeacherRepository,
    private val subjectId: String,
) {
    var subject by mutableStateOf<TeacherSubject?>(null)
        private set
    var students by mutableStateOf<List<EnrolledStudent>>(emptyList())
        private set
    var activities by mutableStateOf<List<GradeActivity>>(emptyList())
        private set
    private var gradesByActivity: Map<String, Map<String, BigDecimal>> = emptyMap()

    var selectedId by mutableStateOf<String?>(null)
        private set
    var isLoading by mutableStateOf(true)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    /** Texto de cada celda, por id de estudiante. */
    val inputs = mutableStateMapOf<String, String>()
    private var savedInputs by mutableStateOf(emptyMap<String, String>())
    var lastSavedAt by mutableStateOf<Long?>(null)
        private set

    val selected: GradeActivity?
        get() = activities.firstOrNull { it.id == selectedId }
    val isPublished: Boolean
        get() = selected?.status == ActivityStatus.PUBLISHED
    val totalPercentage: Int
        get() = activities.sumOf { it.percentage }
    val remainingPercentage: Int
        get() = (MAX_TOTAL_PERCENTAGE - totalPercentage).coerceAtLeast(0)
    val canAddActivity: Boolean
        get() = totalPercentage < MAX_TOTAL_PERCENTAGE
    val parsed: Map<String, GradeInput>
        get() = inputs.mapValues { parseGrade(it.value) }
    val isDirty: Boolean
        get() = inputs.toMap() != savedInputs
    val hasErrors: Boolean
        get() = parsed.values.any { it is GradeInput.Invalid }
    val missingCount: Int
        get() = parsed.values.count { it is GradeInput.Empty }

    suspend fun loadWorkspace(preferredActivityId: String? = selectedId) {
        isLoading = true
        errorMessage = null
        try {
            val workspace = repo.loadSubjectWorkspace(subjectId)
            subject = workspace.subject
            students = workspace.students
            activities = workspace.activities
            gradesByActivity = workspace.gradesByActivity

            val targetActivityId = preferredActivityId
                ?.takeIf { id -> activities.any { it.id == id } }
                ?: activities.firstOrNull { it.status == ActivityStatus.DRAFT }?.id
                ?: activities.firstOrNull()?.id

            if (targetActivityId != null) {
                loadActivityInputs(targetActivityId)
            } else {
                selectedId = null
                inputs.clear()
                savedInputs = emptyMap()
            }
        } catch (e: Throwable) {
            errorMessage = (e as? TeacherRepositoryException)?.message
                ?: "No fue posible cargar la materia."
        } finally {
            isLoading = false
        }
    }

    fun loadActivityInputs(activityId: String) {
        if (activities.none { it.id == activityId }) return
        selectedId = activityId
        val grades = gradesByActivity[activityId].orEmpty()
        inputs.clear()
        students.forEach { inputs[it.id] = grades[it.id]?.toPlainString().orEmpty() }
        savedInputs = inputs.toMap()
        lastSavedAt = null
    }

    suspend fun createActivity(name: String, percentage: Int, status: ActivityStatus): Boolean {
        if (isSaving) return false
        isSaving = true
        errorMessage = null
        return try {
            val created = repo.createActivity(subjectId, name, percentage, status)
            loadWorkspace(preferredActivityId = created.id)
            true
        } catch (e: Throwable) {
            errorMessage = (e as? TeacherRepositoryException)?.message
                ?: "No se pudo crear la actividad."
            false
        } finally {
            isSaving = false
        }
    }

    suspend fun updateActivity(
        activityId: String,
        name: String,
        percentage: Int,
        status: ActivityStatus,
    ): Boolean {
        if (isSaving) return false
        isSaving = true
        errorMessage = null
        return try {
            val updated = repo.updateActivity(subjectId, activityId, name, percentage, status)
            loadWorkspace(preferredActivityId = updated.id)
            true
        } catch (e: Throwable) {
            errorMessage = (e as? TeacherRepositoryException)?.message
                ?: "No se pudo actualizar la actividad."
            false
        } finally {
            isSaving = false
        }
    }

    /** Notas listas para guardar: vacías se borran, las inválidas se dejan como estaban. */
    private fun gradesToSave(): Map<String, BigDecimal?> = parsed.mapNotNull { (studentId, input) ->
        when (input) {
            GradeInput.Empty -> studentId to null
            is GradeInput.Valid -> studentId to input.value
            is GradeInput.Invalid -> null
        }
    }.toMap()

    suspend fun saveDraft(): Boolean {
        val currentActivity = selected ?: return false
        if (isPublished || isSaving) return false
        isSaving = true
        errorMessage = null
        return try {
            val toSave = gradesToSave()
            repo.saveDraft(subjectId, currentActivity.id, toSave)
            val updatedGrades = gradesByActivity[currentActivity.id].orEmpty().toMutableMap()
            toSave.forEach { (studentId, gradeValue) ->
                if (gradeValue == null) updatedGrades.remove(studentId) else updatedGrades[studentId] = gradeValue
            }
            gradesByActivity = gradesByActivity + (currentActivity.id to updatedGrades)
            savedInputs = inputs.toMap()
            lastSavedAt = System.currentTimeMillis()
            true
        } catch (e: Throwable) {
            errorMessage = (e as? TeacherRepositoryException)?.message
                ?: "No se pudo guardar el borrador."
            false
        } finally {
            isSaving = false
        }
    }

    suspend fun publish(): Boolean {
        val currentActivity = selected ?: return false
        if (isSaving) return false
        isSaving = true
        errorMessage = null
        return try {
            val toSave = gradesToSave()
            repo.publishActivity(subjectId, currentActivity.id, toSave)
            lastSavedAt = System.currentTimeMillis()
            loadWorkspace(preferredActivityId = currentActivity.id)
            true
        } catch (e: Throwable) {
            errorMessage = (e as? TeacherRepositoryException)?.message
                ?: "No se pudieron publicar las notas."
            false
        } finally {
            isSaving = false
        }
    }

    suspend fun savePublishedChanges(): Boolean {
        val currentActivity = selected ?: return false
        if (isSaving) return false
        isSaving = true
        errorMessage = null
        return try {
            val toSave = gradesToSave()
            repo.updatePublishedGrades(subjectId, currentActivity.id, toSave)
            val updatedGrades = gradesByActivity[currentActivity.id].orEmpty().toMutableMap()
            toSave.forEach { (studentId, gradeValue) ->
                if (gradeValue == null) updatedGrades.remove(studentId) else updatedGrades[studentId] = gradeValue
            }
            gradesByActivity = gradesByActivity + (currentActivity.id to updatedGrades)
            savedInputs = inputs.toMap()
            lastSavedAt = System.currentTimeMillis()
            true
        } catch (e: Throwable) {
            errorMessage = (e as? TeacherRepositoryException)?.message
                ?: "No se pudieron guardar los cambios."
            false
        } finally {
            isSaving = false
        }
    }
}

private sealed interface PendingDialog {
    data object Publish : PendingDialog
    data object SavePublished : PendingDialog
    data class DiscardAndSwitch(val activityId: String) : PendingDialog
    data object CreateActivity : PendingDialog
    data class EditActivity(val activity: GradeActivity) : PendingDialog
}

/** RF-10, RF-11, RF-12, RNF-06 · Figma "5. Docente · Registro de notas". */
@Composable
fun GradeEntryScreen(
    subjectId: String,
    onBack: () -> Unit,
    repository: TeacherRepository = remember { FirestoreTeacherRepository() },
) {
    val scope = rememberCoroutineScope()
    val state = remember(subjectId, repository) { GradeEntryState(repository, subjectId) }
    var dialog by remember { mutableStateOf<PendingDialog?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(subjectId, repository) {
        state.loadWorkspace()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }

    // RNF-06: el borrador se guarda solo, poco después de dejar de escribir.
    LaunchedEffect(state.selectedId, state.isPublished) {
        snapshotFlow { state.inputs.toMap() }.collectLatest {
            if (state.selected != null && !state.isPublished && !state.hasErrors && state.isDirty) {
                delay(AUTOSAVE_DELAY_MS)
                state.saveDraft()
            }
        }
    }

    fun switchActivity(activityId: String) {
        when {
            activityId == state.selectedId -> Unit
            state.isPublished && state.isDirty -> dialog = PendingDialog.DiscardAndSwitch(activityId)
            else -> scope.launch {
                state.saveDraft()
                state.loadActivityInputs(activityId)
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
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NotiaPrimary)
                }
            }
            state.subject == null && state.errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NotiaInk,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NotiaSecondaryButton(
                        text = "Reintentar",
                        onClick = { scope.launch { state.loadWorkspace() } },
                    )
                }
            }
            else -> {
                val subject = state.subject
                val selectedActivity = state.selected

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SubjectHeader(
                        title = subject?.name ?: "Materia",
                        subtitle = listOfNotNull(subject?.organizationName, subject?.periodName)
                            .joinToString(" · "),
                        onBack = onBack
                    )

                    if (state.errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = GradeError,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "ACTIVIDAD",
                            style = TextStyle(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 0.6.sp
                            ),
                            color = SectionLabel,
                        )
                        Text(
                            text = "Total: ${state.totalPercentage} % / 100 %",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (state.totalPercentage > MAX_TOTAL_PERCENTAGE) GradeError else NotiaTextSecondary,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedActivity != null) {
                        ActivityCard(
                            activities = state.activities,
                            selected = selectedActivity,
                            saveStatus = saveStatusText(
                                isPublished = state.isPublished,
                                isDirty = state.isDirty,
                                isSaving = state.isSaving,
                                lastSavedAt = state.lastSavedAt,
                                now = now
                            ),
                            onSelect = ::switchActivity,
                            onEdit = { dialog = PendingDialog.EditActivity(selectedActivity) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        StudentsHeader(count = state.students.size)
                        Spacer(modifier = Modifier.height(12.dp))
                        if (state.students.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(NotiaSurface, RoundedCornerShape(18.dp))
                                    .border(1.dp, NotiaBorder, RoundedCornerShape(18.dp))
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "No hay estudiantes inscritos en esta materia.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NotiaTextSecondary,
                                )
                            }
                        } else {
                            StudentsCard(
                                students = state.students,
                                inputs = state.inputs,
                                parsed = state.parsed,
                                onValueChange = { studentId, text ->
                                    state.inputs[studentId] = sanitizeGradeText(text)
                                }
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NotiaSurface, RoundedCornerShape(18.dp))
                                .border(1.dp, NotiaBorder, RoundedCornerShape(18.dp))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Esta materia aún no tiene actividades. Usa el botón \"Nueva actividad\" de abajo para crear una.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NotiaTextSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                BottomActions(
                    hasActivity = selectedActivity != null,
                    hasStudents = state.students.isNotEmpty(),
                    canAddActivity = state.canAddActivity,
                    totalPercentage = state.totalPercentage,
                    isPublished = state.isPublished,
                    isDirty = state.isDirty,
                    hasErrors = state.hasErrors,
                    isSaving = state.isSaving,
                    missingCount = state.missingCount,
                    onCreateActivity = { dialog = PendingDialog.CreateActivity },
                    onSaveDraft = { scope.launch { state.saveDraft() } },
                    onPublish = { dialog = PendingDialog.Publish },
                    onSavePublished = { dialog = PendingDialog.SavePublished }
                )
            }
        }
    }

    when (val current = dialog) {
        PendingDialog.CreateActivity -> ActivityFormDialog(
            title = "Nueva actividad",
            confirmLabel = "Crear",
            initialName = "",
            initialPercentage = "",
            initialStatus = ActivityStatus.DRAFT,
            maxAllowedPercentage = state.remainingPercentage,
            currentTotalPercentage = state.totalPercentage,
            onConfirm = { name, percentage, status ->
                dialog = null
                scope.launch { state.createActivity(name, percentage, status) }
            },
            onDismiss = { dialog = null }
        )
        is PendingDialog.EditActivity -> {
            val maxForEdited = (MAX_TOTAL_PERCENTAGE - (state.totalPercentage - current.activity.percentage))
                .coerceIn(0, MAX_TOTAL_PERCENTAGE)
            ActivityFormDialog(
                title = "Editar actividad",
                confirmLabel = "Guardar",
                initialName = current.activity.name,
                initialPercentage = current.activity.percentage.toString(),
                initialStatus = current.activity.status,
                maxAllowedPercentage = maxForEdited,
                currentTotalPercentage = state.totalPercentage - current.activity.percentage,
                onConfirm = { name, percentage, status ->
                    dialog = null
                    scope.launch { state.updateActivity(current.activity.id, name, percentage, status) }
                },
                onDismiss = { dialog = null }
            )
        }
        PendingDialog.Publish -> ConfirmDialog(
            title = "¿Publicar ${state.selected?.name.orEmpty()}?",
            text = buildString {
                if (state.missingCount > 0) append("${missingText(state.missingCount)} sin nota. ")
                append("Los estudiantes verán sus notas y recibirán una notificación.")
            },
            confirmLabel = "Publicar",
            onConfirm = {
                dialog = null
                scope.launch { state.publish() }
            },
            onDismiss = { dialog = null }
        )
        PendingDialog.SavePublished -> ConfirmDialog(
            title = "¿Guardar cambios?",
            text = "Estas notas ya están publicadas: los estudiantes verán el cambio de inmediato, " +
                    "recibirán una notificación y el cambio quedará en el historial.",
            confirmLabel = "Guardar",
            onConfirm = {
                dialog = null
                scope.launch { state.savePublishedChanges() }
            },
            onDismiss = { dialog = null }
        )
        is PendingDialog.DiscardAndSwitch -> ConfirmDialog(
            title = "¿Descartar cambios?",
            text = "Hay cambios sin guardar en ${state.selected?.name.orEmpty()}.",
            confirmLabel = "Descartar",
            onConfirm = {
                state.loadActivityInputs(current.activityId)
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        null -> Unit
    }
}

@Composable
private fun ActivityFormDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    initialPercentage: String,
    initialStatus: ActivityStatus,
    maxAllowedPercentage: Int,
    currentTotalPercentage: Int,
    onConfirm: (name: String, percentage: Int, status: ActivityStatus) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var percentageText by remember { mutableStateOf(initialPercentage) }
    var status by remember { mutableStateOf(initialStatus) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val parsedPct = percentageText.trim().toIntOrNull()
    val projectedTotal = currentTotalPercentage + (parsedPct ?: 0)
    val exceedsLimit = parsedPct != null && parsedPct > maxAllowedPercentage

    fun submit() {
        val cleanName = name.trim()
        when {
            cleanName.isEmpty() ->
                validationError = "Escribe un nombre para la actividad."
            parsedPct == null || parsedPct !in 1..100 ->
                validationError = "El porcentaje debe ser un número entre 1 y 100."
            maxAllowedPercentage <= 0 ->
                validationError = "Ya alcanzaste el 100 % entre las demás actividades."
            parsedPct > maxAllowedPercentage ->
                validationError = "La suma total superaría el 100 % ($projectedTotal %). Máximo disponible: $maxAllowedPercentage %."
            else -> {
                validationError = null
                onConfirm(cleanName, parsedPct, status)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = NotiaSurface,
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge, color = NotiaInk) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Porcentaje disponible: $maxAllowedPercentage % (acumulado: $currentTotalPercentage % / 100 %)",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (exceedsLimit) GradeError else NotiaTextSecondary,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationError = null
                    },
                    singleLine = true,
                    label = { Text("Nombre de la actividad") },
                    placeholder = { Text("Ej. Parcial 1") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = percentageText,
                    onValueChange = { raw ->
                        percentageText = raw.filter { it.isDigit() }.take(3)
                        validationError = null
                    },
                    singleLine = true,
                    isError = exceedsLimit || validationError != null,
                    label = { Text("Porcentaje (1 - $maxAllowedPercentage)") },
                    placeholder = { Text("Ej. 25") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Estado",
                    style = MaterialTheme.typography.labelMedium,
                    color = NotiaTextSecondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = status == ActivityStatus.DRAFT,
                        onClick = { status = ActivityStatus.DRAFT },
                        label = { Text("Borrador") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimarySoft,
                            selectedLabelColor = NotiaPrimary,
                        ),
                    )
                    FilterChip(
                        selected = status == ActivityStatus.PUBLISHED,
                        onClick = { status = ActivityStatus.PUBLISHED },
                        label = { Text("Publicada") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimarySoft,
                            selectedLabelColor = NotiaPrimary,
                        ),
                    )
                }
                val liveWarning = validationError
                    ?: if (exceedsLimit) {
                        "La suma de las actividades llegaría a $projectedTotal % y no puede superar el 100 %."
                    } else {
                        null
                    }
                if (liveWarning != null) {
                    Text(
                        text = liveWarning,
                        style = MaterialTheme.typography.labelSmall,
                        color = GradeError,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = ::submit,
                enabled = !exceedsLimit && maxAllowedPercentage > 0,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = confirmLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (!exceedsLimit && maxAllowedPercentage > 0) NotiaPrimary else NotiaTextSecondary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                Text(text = "Cancelar", style = MaterialTheme.typography.titleMedium, color = NotiaTextSecondary)
            }
        }
    )
}

private fun missingText(count: Int) = if (count == 1) "Falta 1 nota" else "Faltan $count notas"

private fun saveStatusText(
    isPublished: Boolean,
    isDirty: Boolean,
    isSaving: Boolean,
    lastSavedAt: Long?,
    now: Long,
): String {
    if (isSaving) return "Guardando…"
    if (isDirty) return if (isPublished) "Cambios sin guardar" else "Sin guardar"
    if (lastSavedAt == null) return if (isPublished) "Visible para estudiantes" else "Oculto para estudiantes"
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = NotiaInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = NotiaTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ActivityCard(
    activities: List<GradeActivity>,
    selected: GradeActivity,
    saveStatus: String,
    onSelect: (String) -> Unit,
    onEdit: () -> Unit,
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "${selected.name} · ${selected.percentage} %",
                    style = MaterialTheme.typography.titleMedium,
                    color = NotiaInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_pencil),
                        contentDescription = "Editar actividad",
                        tint = NotiaPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Editar",
                        style = MaterialTheme.typography.labelLarge,
                        color = NotiaPrimary,
                        maxLines = 1,
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = "Cambiar actividad",
                    tint = NotiaInk,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            ActivityStatusChip(status = selected.status, long = true)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = saveStatus,
                style = MaterialTheme.typography.bodySmall,
                color = NotiaTextSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
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
            text = if (long) "Borrador · solo tú" else "Borrador",
            background = ChipDraftBg,
            content = ChipDraftText
        )
        ActivityStatus.PUBLISHED -> StatusChip(
            icon = R.drawable.ic_eye_small,
            text = if (long) "Publicada" else "Publicada",
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
            color = content,
            maxLines = 1,
        )
    }
}

@Composable
private fun StudentsHeader(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
            .padding(horizontal = 14.dp, vertical = 4.dp)
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
            .heightIn(min = 68.dp)
            .padding(vertical = 8.dp),
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
                color = NotiaPrimary,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = student.name,
                style = MaterialTheme.typography.titleMedium,
                color = NotiaInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = student.code,
                style = MaterialTheme.typography.bodySmall,
                color = NotiaTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
            .width(68.dp)
            .height(44.dp)
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (text.isEmpty() && !focused) {
                    Text(
                        text = "—",
                        style = TextStyle(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                        ),
                        color = EmptyDash,
                        modifier = Modifier.fillMaxWidth(),
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
        modifier = Modifier.padding(start = 48.dp, bottom = 10.dp),
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
    hasActivity: Boolean,
    hasStudents: Boolean,
    canAddActivity: Boolean,
    totalPercentage: Int,
    isPublished: Boolean,
    isDirty: Boolean,
    hasErrors: Boolean,
    isSaving: Boolean,
    missingCount: Int,
    onCreateActivity: () -> Unit,
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit,
    onSavePublished: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        NotiaSecondaryButton(
            text = if (canAddActivity) "Nueva actividad" else "Nueva actividad (100%)",
            onClick = onCreateActivity,
            enabled = canAddActivity && !isSaving,
            modifier = Modifier.fillMaxWidth(),
        )

        if (hasActivity && hasStudents) {
            if (isPublished) {
                NotiaPrimaryButton(
                    text = "Guardar cambios",
                    onClick = onSavePublished,
                    enabled = isDirty && !hasErrors && !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NotiaSecondaryButton(
                        text = "Guardar borrador",
                        onClick = onSaveDraft,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    )
                    NotiaPrimaryButton(
                        text = "Publicar notas",
                        onClick = onPublish,
                        modifier = Modifier.weight(1f),
                        enabled = !hasErrors && !isSaving,
                        leadingIcon = painterResource(R.drawable.ic_eye),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    )
                }
            }
        }

        Text(
            text = when {
                totalPercentage > MAX_TOTAL_PERCENTAGE ->
                    "Aviso: las actividades suman $totalPercentage % (superan el 100 %). Ajusta sus porcentajes."
                !canAddActivity ->
                    "Las actividades ya suman 100 %. Reduce un porcentaje para agregar otra actividad."
                hasErrors ->
                    "Corrige las notas marcadas en rojo antes de continuar."
                !hasActivity ->
                    "Crea al menos una actividad para comenzar a registrar notas."
                !hasStudents ->
                    "Cuando haya estudiantes inscritos en la materia podrás registrar sus notas aquí."
                isPublished ->
                    "Las notas están publicadas: los cambios se ven de inmediato y quedan en el historial."
                missingCount > 0 ->
                    "${missingText(missingCount)}. Al publicar, los estudiantes reciben una notificación."
                else ->
                    "Al publicar, los estudiantes reciben una notificación."
            },
            style = MaterialTheme.typography.bodySmall,
            color = when {
                totalPercentage > MAX_TOTAL_PERCENTAGE || hasErrors -> GradeError
                else -> NotiaTextSecondary
            },
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
    val previewRepo = remember {
        object : TeacherRepository {
            override suspend fun getSubjects() = listOf(
                TeacherSubject("sub-1", "Programación Móvil", "Univalle", "2026-II")
            )
            override suspend fun loadSubjectWorkspace(subjectId: String) = TeacherSubjectWorkspace(
                subject = TeacherSubject(subjectId, "Programación Móvil", "Univalle", "2026-II"),
                students = listOf(
                    EnrolledStudent("u1", "Ana Castaño", "2380412"),
                    EnrolledStudent("u2", "Brayan Mejía", "2380377"),
                ),
                activities = listOf(
                    GradeActivity("a1", "Parcial 1", 30, ActivityStatus.DRAFT)
                ),
                gradesByActivity = mapOf("a1" to mapOf("u1" to BigDecimal("4.2"))),
            )
            override suspend fun createActivity(
                subjectId: String,
                name: String,
                percentage: Int,
                status: ActivityStatus,
            ) = GradeActivity("a2", name, percentage, status)
            override suspend fun updateActivity(
                subjectId: String,
                activityId: String,
                name: String,
                percentage: Int,
                status: ActivityStatus,
            ) = GradeActivity(activityId, name, percentage, status)
            override suspend fun saveDraft(subjectId: String, activityId: String, values: Map<String, BigDecimal?>) = Unit
            override suspend fun publishActivity(subjectId: String, activityId: String, values: Map<String, BigDecimal?>) = Unit
            override suspend fun updatePublishedGrades(subjectId: String, activityId: String, values: Map<String, BigDecimal?>) = Unit
        }
    }
    NotiaTheme {
        Surface(color = NotiaPaper) {
            GradeEntryScreen(subjectId = "sub-1", onBack = {}, repository = previewRepo)
        }
    }
}