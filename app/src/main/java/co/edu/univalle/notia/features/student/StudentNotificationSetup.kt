package co.edu.univalle.notia.features.student

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

internal data class NotificationSetup(
    val enabled: Boolean,
    val onEnableClick: () -> Unit,
)

/**
 * Estado de notificaciones + acción para activarlas.
 * Primer toque en Android 13+: pide el permiso; si ya se pidió o falta activarlas en ajustes: abre ajustes.
 */
@Composable
internal fun rememberNotificationSetup(notifier: StudentNoticeNotifier): NotificationSetup {
    val context = LocalContext.current
    var enabled by remember(notifier) { mutableStateOf(notifier.canNotify()) }
    var requestedOnce by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        enabled = notifier.canNotify()
    }

    return NotificationSetup(enabled = enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifier.hasPermission() && !requestedOnce) {
            requestedOnce = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openNotificationSettings(context)
        }
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Sin pantalla de ajustes disponible: no hay nada más que hacer.
    }
}
