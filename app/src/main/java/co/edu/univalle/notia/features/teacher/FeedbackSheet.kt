package co.edu.univalle.notia.features.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.univalle.notia.R
import co.edu.univalle.notia.core.ui.NotiaPrimaryButton
import co.edu.univalle.notia.ui.theme.InterFontFamily
import co.edu.univalle.notia.ui.theme.NotiaBorder
import co.edu.univalle.notia.ui.theme.NotiaInk
import co.edu.univalle.notia.ui.theme.NotiaPaper
import co.edu.univalle.notia.ui.theme.NotiaPrimary
import co.edu.univalle.notia.ui.theme.NotiaSurface
import co.edu.univalle.notia.ui.theme.NotiaTextSecondary
import kotlinx.coroutines.launch

/** RF-13 · Figma "9. Docente · Retroalimentación". `onSave(null)` quita el comentario. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeedbackSheet(
    student: EnrolledStudent,
    gradeText: String,
    activity: GradeActivity,
    existing: String?,
    onSave: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf(existing.orEmpty()) }
    val changed = normalizeFeedback(text) != existing

    fun close(then: () -> Unit) {
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
            Text(text = "Retroalimentación", style = MaterialTheme.typography.titleLarge, color = NotiaInk)

            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StudentAvatar(initials = student.initials)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = student.name, style = MaterialTheme.typography.titleMedium, color = NotiaInk)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(text = student.code, style = MaterialTheme.typography.bodySmall, color = NotiaTextSecondary)
                }
                Column(
                    modifier = Modifier
                        .size(width = 64.dp, height = 44.dp)
                        .background(NotiaPaper, RoundedCornerShape(12.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = gradeText.ifEmpty { "—" },
                        style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = if (gradeText.isEmpty()) EmptyDash else NotiaInk
                    )
                    Text(
                        text = "Nota",
                        style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 10.sp),
                        color = NotiaTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActivityStatusChip(status = activity.status, long = false)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${activity.name} · ${activity.percentage} %",
                    style = MaterialTheme.typography.bodySmall,
                    color = NotiaTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Comentario", style = MaterialTheme.typography.labelSmall, color = NotiaTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            FeedbackField(text = text, onValueChange = { text = it.take(FEEDBACK_MAX_LENGTH) })
            Spacer(modifier = Modifier.height(6.dp))
            Row {
                Text(
                    text = "Opcional",
                    style = MaterialTheme.typography.bodySmall,
                    color = NotiaTextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${text.length} / $FEEDBACK_MAX_LENGTH",
                    style = MaterialTheme.typography.bodySmall,
                    color = NotiaTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimarySoft, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_eye_small),
                    contentDescription = null,
                    tint = NotiaPrimary,
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(14.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = feedbackVisibilityText(student.name, activity.name, activity.status),
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 17.sp,
                    color = NotiaPrimary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            NotiaPrimaryButton(
                text = "Guardar comentario",
                onClick = { close { onSave(normalizeFeedback(text)) } },
                enabled = changed
            )

            if (existing != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { close { onSave(null) } },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_trash),
                        contentDescription = null,
                        tint = GradeError,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Quitar comentario", style = MaterialTheme.typography.titleMedium, color = GradeError)
                }
            }
        }
    }
}

@Composable
private fun FeedbackField(text: String, onValueChange: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    BasicTextField(
        value = text,
        onValueChange = onValueChange,
        cursorBrush = SolidColor(NotiaPrimary),
        textStyle = TextStyle(fontFamily = InterFontFamily, fontSize = 15.sp, lineHeight = 21.sp, color = NotiaInk),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 124.dp)
            .background(NotiaSurface, shape)
            .border(if (focused) 2.dp else 1.dp, if (focused) NotiaPrimary else NotiaBorder, shape)
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = "Comentario" },
        decorationBox = { inner ->
            Box(modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
                if (text.isEmpty()) {
                    Text(
                        text = "Escribe qué hizo bien y qué puede mejorar",
                        style = TextStyle(fontFamily = InterFontFamily, fontSize = 15.sp, lineHeight = 21.sp),
                        color = EmptyDash
                    )
                }
                inner()
            }
        }
    )
}

/** Avatar con iniciales de la guía de estilo. */
@Composable
internal fun StudentAvatar(initials: String) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(PrimarySoft, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp),
            color = NotiaPrimary
        )
    }
}
