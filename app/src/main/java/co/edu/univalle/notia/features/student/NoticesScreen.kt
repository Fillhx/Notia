package co.edu.univalle.notia.features.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal fun NoticeType.label(): String = when (this) {
    NoticeType.GRADE_PUBLISHED -> "Nota publicada"
    NoticeType.GRADE_EDITED -> "Nota editada"
}

internal fun NoticeType.icon(): ImageVector = when (this) {
    NoticeType.GRADE_PUBLISHED -> Icons.Filled.CheckCircle
    NoticeType.GRADE_EDITED -> Icons.Filled.Edit
}

internal fun formatNoticeDate(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale.forLanguageTag("es-CO"))
        .withZone(ZoneId.systemDefault())
        .format(instant)

/** Avisos de notas publicadas o editadas (RF-18). Tocar un aviso lo marca como leído y abre la materia. */
@Composable
fun NoticesScreen(
    state: StudentLoadState<List<StudentNotice>>,
    notificationsEnabled: Boolean,
    onEnableNotifications: () -> Unit,
    onNoticeClick: (StudentNotice) -> Unit,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is StudentLoadState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator()
                Text("Cargando avisos…", style = MaterialTheme.typography.bodyMedium)
            }
        }

        is StudentLoadState.Error -> Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            MessageRow(Icons.Filled.Warning, state.message)
        }

        is StudentLoadState.Success -> {
            val unread = state.data.count { !it.isRead }
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Avisos",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            if (unread == 0) "Todo al día" else "$unread sin leer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (!notificationsEnabled) {
                    item { EnableNotificationsCard(onEnableNotifications) }
                }
                item {
                    OutlinedButton(
                        onClick = onMarkAllRead,
                        enabled = unread > 0,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Filled.Done, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Marcar todos como leídos")
                    }
                }
                if (state.data.isEmpty()) {
                    item { MessageRow(Icons.Filled.Info, "No tienes avisos por ahora.") }
                } else {
                    items(state.data, key = { it.id }) { notice ->
                        NoticeCard(
                            notice = notice,
                            onClick = { onNoticeClick(notice) },
                            onMarkRead = { onMarkRead(notice.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnableNotificationsCard(onEnable: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MessageRow(
                Icons.Filled.Warning,
                "Las notificaciones están desactivadas. Actívalas para enterarte cuando publiquen o editen una nota.",
            )
            Button(onClick = onEnable, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Filled.Notifications, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Activar notificaciones")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoticeCard(notice: StudentNotice, onClick: () -> Unit, onMarkRead: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(notice.type.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    notice.type.label(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                ReadStateLabel(notice.isRead)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${notice.subjectName} · ${notice.activityName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (notice.isRead) FontWeight.Normal else FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
            Text(
                formatNoticeDate(notice.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!notice.isRead) {
                TextButton(onClick = onMarkRead, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Marcar como leído")
                }
            }
        }
    }
}

/** Estado de lectura siempre con icono + texto (RNF-09). */
@Composable
private fun ReadStateLabel(isRead: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
            if (isRead) Icons.Filled.Done else Icons.Filled.Notifications,
            contentDescription = null,
            tint = if (isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
        )
        Text(if (isRead) "Leído" else "Sin leer", style = MaterialTheme.typography.labelMedium)
    }
}
