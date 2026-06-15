package tp3.grupo1.hci.itba.edu.ar.data.notifications

import androidx.annotation.StringRes
import kotlinx.serialization.Serializable
import tp3.grupo1.hci.itba.edu.ar.R

/**
 * User-selectable notification categories (Settings). Each [NotificationType]
 * belongs to exactly one. A category disabled in preferences is neither posted
 * as a system notification nor stored in the in-app list.
 */
enum class NotificationCategory(@StringRes val labelRes: Int, @StringRes val descriptionRes: Int) {
    CRITICAL(R.string.notif_cat_critical, R.string.notif_cat_critical_desc),
    SIMPLE(R.string.notif_cat_simple, R.string.notif_cat_simple_desc),
    DEVICE_LIFECYCLE(R.string.notif_cat_device_lifecycle, R.string.notif_cat_device_lifecycle_desc),
    HOME(R.string.notif_cat_home, R.string.notif_cat_home_desc),
}

/**
 * Every kind of notification the app can raise, mapping the real-time event to
 * its category and localized strings. [hasArg] marks the bodies that expect a
 * device/home name argument.
 */
enum class NotificationType(
    val category: NotificationCategory,
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    val hasArg: Boolean,
) {
    DOOR_OPENED(NotificationCategory.CRITICAL, R.string.notif_door_opened_title, R.string.notif_door_opened_body, true),
    DOOR_UNLOCKED(NotificationCategory.CRITICAL, R.string.notif_door_unlocked_title, R.string.notif_door_unlocked_body, true),
    ALARM_TRIGGERED(NotificationCategory.CRITICAL, R.string.notif_alarm_triggered_title, R.string.notif_alarm_triggered_body, true),
    ALARM_ARMED(NotificationCategory.CRITICAL, R.string.notif_alarm_armed_title, R.string.notif_alarm_armed_body, true),
    ALARM_DISARMED(NotificationCategory.CRITICAL, R.string.notif_alarm_disarmed_title, R.string.notif_alarm_disarmed_body, true),
    CYCLE_FINISHED(NotificationCategory.SIMPLE, R.string.notif_cycle_finished_title, R.string.notif_cycle_finished_body, true),
    DEVICE_CREATED(NotificationCategory.DEVICE_LIFECYCLE, R.string.notif_device_created_title, R.string.notif_device_created_body, true),
    DEVICE_DELETED(NotificationCategory.DEVICE_LIFECYCLE, R.string.notif_device_deleted_title, R.string.notif_device_deleted_body, true),
    HOME_SHARED(NotificationCategory.HOME, R.string.notif_home_shared_title, R.string.notif_home_shared_body, false),
    HOME_UNSHARED(NotificationCategory.HOME, R.string.notif_home_unshared_title, R.string.notif_home_unshared_body, false),
}

/**
 * A notification persisted in the local history shown on the Notifications
 * screen. [arg] is the device/home name when the type expects one.
 */
@Serializable
data class StoredNotification(
    val id: String,
    val type: NotificationType,
    val arg: String? = null,
    val timestamp: Long,
    val read: Boolean = false,
)
