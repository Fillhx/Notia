package co.edu.univalle.notia.features.student

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Notificaciones locales de avisos de notas publicadas o editadas (RF-18).
 * Solo usa el contexto de aplicación, así que no depende de una Activity.
 * Si falta el permiso (API 33+) o el usuario las desactivó, [notify] no hace nada y devuelve false.
 */
class StudentNoticeNotifier(
    context: Context,
    @param:DrawableRes private val smallIcon: Int = android.R.drawable.ic_dialog_info,
) {
    private val appContext = context.applicationContext

    /** Permiso POST_NOTIFICATIONS concedido (siempre true antes de Android 13). */
    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Se pueden mostrar notificaciones: permiso concedido y no desactivadas en ajustes. */
    fun canNotify(): Boolean = hasPermission() && NotificationManagerCompat.from(appContext).areNotificationsEnabled()

    @SuppressLint("MissingPermission") // El permiso se comprueba en canNotify().
    fun notify(notice: StudentNotice): Boolean {
        if (!canNotify()) return false
        ensureChannel()

        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                appContext,
                notice.id.hashCode(),
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(notice.type.label())
            .setContentText("${notice.subjectName}: ${notice.activityName}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .apply { if (contentIntent != null) setContentIntent(contentIntent) }
            .build()

        return try {
            NotificationManagerCompat.from(appContext).notify(notice.id.hashCode(), notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    // minSdk 26: los canales siempre existen; crear uno que ya existe no tiene efecto.
    private fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Avisos de notas", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Notas publicadas o editadas por tus docentes" }
        appContext.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "student_grade_notices"
    }
}

/**
 * Decide qué avisos son nuevos y sin leer. La primera lista recibida es la línea base
 * (no se notifica lo que ya existía al abrir la app); las siguientes notifican solo ids nunca vistos.
 */
class NewNoticeDetector {
    private var hasBaseline = false
    private val seen = mutableSetOf<String>()

    fun newUnread(notices: List<StudentNotice>): List<StudentNotice> {
        val fresh = notices.filter { it.id !in seen }
        seen += notices.map { it.id }
        if (!hasBaseline) {
            hasBaseline = true
            return emptyList()
        }
        return fresh.filter { !it.isRead }
    }
}
