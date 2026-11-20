package co.edu.univalle.notia.features.coordinator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.edu.univalle.notia.ui.theme.ErrorBg
import co.edu.univalle.notia.ui.theme.ErrorText
import co.edu.univalle.notia.ui.theme.NotiaAccent
import co.edu.univalle.notia.ui.theme.NotiaBorder
import co.edu.univalle.notia.ui.theme.SuccessBg
import co.edu.univalle.notia.ui.theme.SuccessText
import co.edu.univalle.notia.ui.theme.WarningBg
import co.edu.univalle.notia.ui.theme.WarningText
import kotlinx.coroutines.launch

private data class Member(val initials: String, val name: String, val code: String, val role: String)
private data class Period(val id: Int, val name: String, val start: String, val end: String)
private data class Subject(
    val id: Int,
    val name: String,
    val code: String,
    val description: String,
    val periodId: Int,
    val teachers: List<String>,
    val students: Int
)

private val fakeMembers = listOf(
    Member("AM", "Ana Castaño", "2380177", "Estudiante"),
    Member("BM", "Brayan Mejía", "2380472", "Estudiante"),
    Member("LG", "Laura Gómez", "2380128", "Docente"),
    Member("GR", "Gustavo Restrepo", "2380618", "Docente"),
    Member("EV", "Esteban Vargas", "2380288", "Estudiante")
)

@Composable
fun CoordinatorScreen() {
    var bottomSection by remember { mutableIntStateOf(0) }
    var tabSection by remember { mutableIntStateOf(0) }
    var members by remember { mutableStateOf(fakeMembers) }
    var periods by remember { mutableStateOf(listOf(
        Period(1, "2026-I", "01/01/2026", "30/06/2026"),
        Period(2, "2026-II", "01/07/2026", "30/11/2026")
    )) }
    var subjects by remember { mutableStateOf(listOf(
        Subject(1, "Programación Móvil", "NOT-4F7K", "Curso de programación móvil con Kotlin.", 2, listOf("Gustavo Restrepo"), 28),
        Subject(2, "Desarrollo Web", "WEB-201", "Arquitecturas y desarrollo de aplicaciones web.", 2, listOf("Laura Gómez"), 32),
        Subject(3, "Bases de Datos", "BD-102", "Modelado y gestión de bases de datos.", 1, listOf("Laura Gómez"), 24)
    )) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                val destinations = listOf(
                    Triple("Panel", Icons.Default.PieChart, 0),
                    Triple("Reportes", Icons.AutoMirrored.Filled.List, 1),
                    Triple("Historial", Icons.Default.History, 2),
                    Triple("Perfil", Icons.Default.Person, 3)
                )
                destinations.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = bottomSection == index,
                        onClick = { bottomSection = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (bottomSection) {
                0 -> CoordinatorPanel(
                    tabSection = tabSection,
                    onTabChange = { tabSection = it },
                    members = members,
                    periods = periods,
                    subjects = subjects,
                    onMembersChange = { members = it },
                    onPeriodsChange = { periods = it },
                    onSubjectsChange = { subjects = it },
                    onMessage = { scope.launch { snackbarHostState.showSnackbar(it) } }
                )
                1 -> ReportsSection(subjects)
                2 -> HistorySection()
                3 -> ProfileSection()
            }
        }
    }
}

@Composable
private fun CoordinatorPanel(
    tabSection: Int,
    onTabChange: (Int) -> Unit,
    members: List<Member>,
    periods: List<Period>,
    subjects: List<Subject>,
    onMembersChange: (List<Member>) -> Unit,
    onPeriodsChange: (List<Period>) -> Unit,
    onSubjectsChange: (List<Subject>) -> Unit,
    onMessage: (String) -> Unit
) {
    var showSubjectDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<Subject?>(null) }
    var showPeriodDialog by remember { mutableStateOf(false) }
    var editingPeriod by remember { mutableStateOf<Period?>(null) }
    var subjectForAssignments by remember { mutableStateOf<Subject?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Text("Coordinación", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Academia Matemática", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            InvitationCard(onMessage)
            Spacer(Modifier.height(16.dp))
        }
        item {
            TabRow(selectedTabIndex = tabSection, containerColor = MaterialTheme.colorScheme.background) {
                listOf("Materias", "Miembros", "Periodos").forEachIndexed { index, title ->
                    Tab(selected = tabSection == index, onClick = { onTabChange(index) }, text = { Text(title) })
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        when (tabSection) {
            0 -> {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { editingSubject = null; showSubjectDialog = true }) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Nueva materia")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                items(subjects, key = { it.id }) { subject ->
                    SubjectCard(
                        subject,
                        periods.firstOrNull { it.id == subject.periodId }?.name ?: "Sin periodo",
                        onEdit = { editingSubject = subject; showSubjectDialog = true },
                        onAssignments = { subjectForAssignments = subject },
                        onDelete = {
                            if (subject.students > 0) onMessage("No se puede eliminar una materia con estudiantes inscritos")
                            else onSubjectsChange(subjects.filter { it.id != subject.id })
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            1 -> items(members, key = { it.code }) { member ->
                MemberCard(member, members) { newRole ->
                    val coordinators = members.count { it.role == "Coordinación" }
                    if (member.role == "Coordinación" && newRole != "Coordinación" && coordinators == 1) {
                        onMessage("La organización debe tener al menos un Coordinación")
                    } else {
                        onMembersChange(members.map { if (it.code == member.code) it.copy(role = newRole) else it })
                        onMessage("Rol actualizado a $newRole")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            2 -> {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { editingPeriod = null; showPeriodDialog = true }) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Nuevo periodo")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                items(periods, key = { it.id }) { period ->
                    PeriodCard(period, subjects.count { it.periodId == period.id },
                        onEdit = { editingPeriod = period; showPeriodDialog = true },
                        onDelete = {
                            if (subjects.any { it.periodId == period.id }) onMessage("No se puede eliminar un periodo con materias")
                            else onPeriodsChange(periods.filter { it.id != period.id })
                        })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showSubjectDialog) {
        SubjectDialog(
            subject = editingSubject,
            periods = periods,
            existingSubjects = subjects,
            onDismiss = { showSubjectDialog = false },
            onSave = { saved ->
                val nextId = (subjects.maxOfOrNull { it.id } ?: 0) + 1
                if (editingSubject == null) onSubjectsChange(subjects + saved.copy(id = nextId))
                else onSubjectsChange(subjects.map { if (it.id == saved.id) saved else it })
                showSubjectDialog = false
                onMessage("Materia guardada")
            }
        )
    }
    if (showPeriodDialog) {
        PeriodDialog(
            period = editingPeriod,
            periods = periods,
            onDismiss = { showPeriodDialog = false },
            onSave = { saved ->
                val nextId = (periods.maxOfOrNull { it.id } ?: 0) + 1
                if (editingPeriod == null) onPeriodsChange(periods + saved.copy(id = nextId))
                else onPeriodsChange(periods.map { if (it.id == saved.id) saved else it })
                showPeriodDialog = false
                onMessage("Periodo guardado")
            }
        )
    }
    subjectForAssignments?.let { subject ->
        AssignmentsDialog(
            subject = subject,
            members = members,
            onDismiss = { subjectForAssignments = null },
            onSave = { teachers, students ->
                onSubjectsChange(subjects.map { if (it.id == subject.id) it.copy(teachers = teachers, students = students) else it })
                subjectForAssignments = null
                onMessage("Asignaciones guardadas")
            }
        )
    }
}

@Composable
private fun InvitationCard(onMessage: (String) -> Unit) {
    val context = LocalContext.current
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Código de invitación", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f), style = MaterialTheme.typography.labelSmall)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("NOT-4F7K", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Código de invitación", "NOT-4F7K"))
                    onMessage("Código copiado")
                }, modifier = Modifier.background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))) {
                    Icon(Icons.Default.ContentCopy, "Copiar código", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun MemberCard(member: Member, members: List<Member>, onRoleChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, NotiaBorder)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Text(member.initials, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(member.name, fontWeight = FontWeight.Bold)
                Text(member.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                OutlinedButton(onClick = { expanded = true }, contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Text(member.role, maxLines = 1, style = MaterialTheme.typography.labelMedium)
                }
                androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("Coordinación", "Docente", "Estudiante").forEach { role ->
                        DropdownMenuItem(text = { Text(role) }, onClick = { expanded = false; onRoleChange(role) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectCard(subject: Subject, periodName: String, onEdit: () -> Unit, onAssignments: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, NotiaBorder)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Book, null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(subject.name, fontWeight = FontWeight.Bold)
                    Text("${subject.code} · $periodName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${subject.students}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAssignments, modifier = Modifier.weight(1f)) { Text("Asignar") }
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("Editar") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Close, "Eliminar", tint = ErrorText) }
            }
        }
    }
}

@Composable
private fun PeriodCard(period: Period, subjectCount: Int, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, NotiaBorder)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(period.name, fontWeight = FontWeight.Bold)
                Text("${period.start} — ${period.end}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$subjectCount materias", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onEdit) { Text("Editar") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Close, "Eliminar", tint = ErrorText) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectDialog(subject: Subject?, periods: List<Period>, existingSubjects: List<Subject>, onDismiss: () -> Unit, onSave: (Subject) -> Unit) {
    var name by remember { mutableStateOf(subject?.name ?: "") }
    var code by remember { mutableStateOf(subject?.code ?: "") }
    var description by remember { mutableStateOf(subject?.description ?: "") }
    var periodId by remember { mutableIntStateOf(subject?.periodId ?: (periods.firstOrNull()?.id ?: 0)) }
    var periodExpanded by remember { mutableStateOf(false) }
    val nameError = name.isBlank()
    val codeError = code.isBlank() || existingSubjects.any { it.id != subject?.id && it.code.equals(code.trim(), ignoreCase = true) && it.periodId == periodId }

    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (subject == null) "Nueva materia" else "Editar materia") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, isError = nameError, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(code, { code = it.uppercase() }, label = { Text("Código único") }, isError = codeError, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it }, label = { Text("Descripción (opcional)") }, modifier = Modifier.fillMaxWidth())
            if (periods.isNotEmpty()) {
                ExposedDropdownMenuBox(expanded = periodExpanded, onExpandedChange = { periodExpanded = it }) {
                    OutlinedTextField(
                        value = periods.firstOrNull { it.id == periodId }?.name ?: "",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Periodo") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(periodExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = periodExpanded, onDismissRequest = { periodExpanded = false }) {
                        periods.forEach { period -> DropdownMenuItem(text = { Text(period.name) }, onClick = { periodId = period.id; periodExpanded = false }) }
                    }
                }
            }
            if (subject != null && subject.students > 0) Text("El periodo no puede cambiarse mientras existan notas.", style = MaterialTheme.typography.bodySmall, color = WarningText)
        }
    }, confirmButton = { TextButton(enabled = !nameError && !codeError && periods.isNotEmpty() && (subject == null || subject.students == 0 || periodId == subject.periodId), onClick = {
        onSave(Subject(subject?.id ?: 0, name.trim(), code.trim().uppercase(), description.trim(), periodId, subject?.teachers ?: emptyList(), subject?.students ?: 0))
    }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun PeriodDialog(period: Period?, periods: List<Period>, onDismiss: () -> Unit, onSave: (Period) -> Unit) {
    var name by remember { mutableStateOf(period?.name ?: "") }
    var start by remember { mutableStateOf(period?.start ?: "") }
    var end by remember { mutableStateOf(period?.end ?: "") }
    val nameError = name.isBlank() || periods.any { it.id != period?.id && it.name.equals(name.trim(), ignoreCase = true) }
    fun dateValue(raw: String): Int? {
        val parts = raw.split("/")
        if (parts.size != 3) return null
        val day = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val year = parts[2].toIntOrNull() ?: return null
        if (year < 1000 || month !in 1..12 || day !in 1..31) return null
        val leap = year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)
        val days = listOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        if (day > days[month - 1]) return null
        return year * 10000 + month * 100 + day
    }
    val dateError = dateValue(start) == null || dateValue(end) == null || (dateValue(start)!! >= dateValue(end)!!)

    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (period == null) "Nuevo periodo" else "Editar periodo") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, isError = nameError, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(start, { start = it }, label = { Text("Inicio (dd/mm/aaaa)") }, isError = dateError, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(end, { end = it }, label = { Text("Fin (dd/mm/aaaa)") }, isError = dateError, modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(enabled = !nameError && !dateError, onClick = { onSave(Period(period?.id ?: 0, name.trim(), start.trim(), end.trim())) }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun AssignmentsDialog(subject: Subject, members: List<Member>, onDismiss: () -> Unit, onSave: (List<String>, Int) -> Unit) {
    var selectedTeachers by remember { mutableStateOf(subject.teachers) }
    var studentCount by remember { mutableIntStateOf(subject.students) }
    val teacherMembers = members.filter { it.role == "Docente" }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(subject.name) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Docentes", fontWeight = FontWeight.Bold)
            teacherMembers.forEach { member ->
                val selected = member.name in selectedTeachers
                Row(Modifier.fillMaxWidth().clickable {
                    selectedTeachers = if (selected) selectedTeachers - member.name else selectedTeachers + member.name
                }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (selected) Icons.Outlined.Check else Icons.Outlined.Book, null, tint = if (selected) SuccessText else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp)); Text(member.name)
                }
            }
            OutlinedTextField(studentCount.toString(), { studentCount = it.toIntOrNull()?.coerceIn(0, 500) ?: 0 }, label = { Text("Estudiantes inscritos") }, modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(onClick = { onSave(selectedTeachers, studentCount) }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun ReportsSection(subjects: List<Subject>) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Reportes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Resumen académico por materia", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(4.dp)) }
        items(subjects) { subject ->
            ReportCard(subject.name)
        }
    }
}

@Composable
private fun ReportCard(name: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, NotiaBorder)) {
        Column(Modifier.padding(14.dp)) {
            Text(name, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ReportChip("Aprobado", "14", SuccessBg, SuccessText, Modifier.weight(1f))
                ReportChip("En curso", "5", WarningBg, WarningText, Modifier.weight(1f))
                ReportChip("Sin notas", "3", MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Promedio acumulado", style = MaterialTheme.typography.bodySmall)
                Text("3.84", fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Promedio publicado", style = MaterialTheme.typography.bodySmall)
                Text("3.62", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReportChip(label: String, value: String, background: Color, textColor: Color, modifier: Modifier) {
    Column(modifier.background(background, RoundedCornerShape(10.dp)).padding(8.dp)) {
        Text(value, color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(label, color = textColor, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HistorySection() {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Historial", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Cambios publicados en las materias", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        listOf("Ana Castaño" to "3.4 → 4.0", "Brayan Mejía" to "2.8 → 3.2", "Camila Rojas" to "— → 3.4").forEach { (name, change) ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, NotiaBorder)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(name, fontWeight = FontWeight.Bold); Text(change, style = MaterialTheme.typography.bodySmall) }
                        Text("Hoy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSection() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Top) {
        Text("Perfil", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, NotiaBorder), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp)); Text("Santiago Hernández", fontWeight = FontWeight.Bold); Text("Coordinación", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
