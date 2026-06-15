package tp3.grupo1.hci.itba.edu.ar.ui.navigation

import java.net.URLEncoder

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val VERIFY = "verify/{email}"
    const val FORGOT_PASSWORD = "forgot_password"
    const val MAIN = "main"
    const val DEVICE_DETAIL = "device/{deviceId}"
    const val ROOM_DETAIL = "room/{roomId}"
    const val ROUTINE_NEW = "routine/new"
    const val ROUTINE_EDIT = "routine/edit/{routineId}"
    const val HOMES = "homes"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"

    // Tabs inside the main screen
    const val TAB_DASHBOARD = "tab_dashboard"
    const val TAB_DEVICES = "tab_devices"
    const val TAB_ROOMS = "tab_rooms"
    const val TAB_ROUTINES = "tab_routines"
    const val TAB_STATISTICS = "tab_statistics"

    fun verify(email: String): String = "verify/${URLEncoder.encode(email, "UTF-8")}"

    fun deviceDetail(deviceId: String): String = "device/$deviceId"

    fun roomDetail(roomId: String): String = "room/$roomId"

    fun routineEdit(routineId: String): String = "routine/edit/$routineId"
}
