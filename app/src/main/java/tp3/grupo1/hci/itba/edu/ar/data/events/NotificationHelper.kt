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
import java.util.concurrent.atomic.AtomicInteger

/** Builds and posts the local notifications triggered by real-time events (RF20). */
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

    fun showDeviceCreated(deviceName: String) = post(
        CHANNEL_DEVICES,
        context.getString(R.string.notif_device_created_title),
        context.getString(R.string.notif_device_created_body, deviceName),
    )

    fun showDeviceDeleted(deviceName: String) = post(
        CHANNEL_DEVICES,
        context.getString(R.string.notif_device_deleted_title),
        context.getString(R.string.notif_device_deleted_body, deviceName),
    )

    fun showHomeShared() = post(
        CHANNEL_HOMES,
        context.getString(R.string.notif_home_shared_title),
        context.getString(R.string.notif_home_shared_body),
    )

    fun showHomeUnshared() = post(
        CHANNEL_HOMES,
        context.getString(R.string.notif_home_unshared_title),
        context.getString(R.string.notif_home_unshared_body),
    )

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
            // The user denied the notifications permission; nothing to do.
        }
    }
}
