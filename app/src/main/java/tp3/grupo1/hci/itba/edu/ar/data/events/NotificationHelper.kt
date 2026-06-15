package tp3.grupo1.hci.itba.edu.ar.data.events

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import tp3.grupo1.hci.itba.edu.ar.MainActivity
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.data.notifications.NotificationCategory
import tp3.grupo1.hci.itba.edu.ar.data.notifications.NotificationType
import java.util.concurrent.atomic.AtomicInteger

// Construye y publica las notificaciones locales disparadas por eventos en tiempo real (RF20).
class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_DEVICES = "devices"
        private const val CHANNEL_HOMES = "homes"
    }

    private val nextId = AtomicInteger(1000)

    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DEVICES,
                context.getString(R.string.notification_channel_devices),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_devices_description)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_HOMES,
                context.getString(R.string.notification_channel_homes),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_homes_description)
            }
        )
    }

    // Publica la notificacion del sistema para type; arg aporta el nombre del dispositivo/hogar.
    fun show(type: NotificationType, arg: String?) {
        val title = context.getString(type.titleRes)
        val body = if (type.hasArg) {
            val name = arg?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.notif_device_fallback_name)
            context.getString(type.bodyRes, name)
        } else {
            context.getString(type.bodyRes)
        }
        post(channelFor(type.category), title, body)
    }

    private fun channelFor(category: NotificationCategory): String = when (category) {
        NotificationCategory.HOME -> CHANNEL_HOMES
        else -> CHANNEL_DEVICES
    }

    private fun post(channel: String, title: String, body: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            )
            .build()
        try {
            manager.notify(nextId.getAndIncrement(), notification)
        } catch (_: SecurityException) {
            // El usuario denego el permiso de notificaciones; nada que hacer.
        }
    }
}
