package tp3.grupo1.hci.itba.edu.ar.domain

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Blinds
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.DoorFront
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.data.model.Device

object DeviceTypeIds {
    const val AC = "go46xmbqeomjrsjr"
    const val LAMP = "eu0v2xgprrhhg41g"
    const val ALARM = "im77xxyulpegfmv8"
    const val BLINDS = "mxztsyjzsrq7iaqc"
    const val DOOR = "c89qmhhzm3bcpoie"
    const val FAUCET = "dbrlsh0juhf3dhf0"
    const val OVEN = "lsf78ly0eqrjbz91"
    const val REFRIGERATOR = "rnizejqr2di0okho"
    const val SPEAKER = "fud5vmuy0fkh6zt9"
    const val VACUUM = "ofglvd9gqx8yfl3l"

    val CREATABLE = listOf(LAMP, AC, ALARM, BLINDS, DOOR, FAUCET, OVEN, REFRIGERATOR, SPEAKER, VACUUM)
}

@StringRes
fun deviceTypeNameRes(typeId: String): Int? = when (typeId) {
    DeviceTypeIds.AC -> R.string.device_type_ac
    DeviceTypeIds.LAMP -> R.string.device_type_lamp
    DeviceTypeIds.ALARM -> R.string.device_type_alarm
    DeviceTypeIds.BLINDS -> R.string.device_type_blinds
    DeviceTypeIds.DOOR -> R.string.device_type_door
    DeviceTypeIds.FAUCET -> R.string.device_type_faucet
    DeviceTypeIds.OVEN -> R.string.device_type_oven
    DeviceTypeIds.REFRIGERATOR -> R.string.device_type_refrigerator
    DeviceTypeIds.SPEAKER -> R.string.device_type_speaker
    DeviceTypeIds.VACUUM -> R.string.device_type_vacuum
    else -> null
}

fun deviceTypeName(context: Context, typeId: String, fallback: String = ""): String =
    deviceTypeNameRes(typeId)?.let { context.getString(it) } ?: fallback

fun deviceTypeIcon(typeId: String): ImageVector = when (typeId) {
    DeviceTypeIds.AC -> Icons.Outlined.AcUnit
    DeviceTypeIds.LAMP -> Icons.Outlined.Lightbulb
    DeviceTypeIds.ALARM -> Icons.Outlined.NotificationsActive
    DeviceTypeIds.BLINDS -> Icons.Outlined.Blinds
    DeviceTypeIds.DOOR -> Icons.Outlined.DoorFront
    DeviceTypeIds.FAUCET -> Icons.Outlined.WaterDrop
    DeviceTypeIds.OVEN -> Icons.Outlined.LocalFireDepartment
    DeviceTypeIds.REFRIGERATOR -> Icons.Outlined.Kitchen
    DeviceTypeIds.SPEAKER -> Icons.Outlined.Speaker
    DeviceTypeIds.VACUUM -> Icons.Outlined.CleaningServices
    else -> Icons.Outlined.DevicesOther
}

fun deviceTypeColor(typeId: String): Color = when (typeId) {
    DeviceTypeIds.AC -> Color(0xFF90D4FB)
    DeviceTypeIds.LAMP -> Color(0xFF10A2F7)
    DeviceTypeIds.ALARM -> Color(0xFFC9331C)
    DeviceTypeIds.BLINDS -> Color(0xFFA89060)
    DeviceTypeIds.DOOR -> Color(0xFF5F6F78)
    DeviceTypeIds.FAUCET -> Color(0xFF2D3C9F)
    DeviceTypeIds.OVEN -> Color(0xFFF97316)
    DeviceTypeIds.REFRIGERATOR -> Color(0xFF14B8A6)
    DeviceTypeIds.SPEAKER -> Color(0xFF7C4FBF)
    DeviceTypeIds.VACUUM -> Color(0xFF9DE84D)
    else -> Color(0xFF5F6F78)
}

@StringRes
fun deviceActionNameRes(action: String): Int? = when (action) {
    "turnOn" -> R.string.device_action_turn_on
    "turnOff" -> R.string.device_action_turn_off
    "setTemperature" -> R.string.device_action_set_temperature
    "setFreezerTemperature" -> R.string.device_action_set_freezer_temperature
    "setBrightness" -> R.string.device_action_set_brightness
    "setColor" -> R.string.device_action_set_color
    "setLevel" -> R.string.device_action_set_level
    "open" -> R.string.device_action_open
    "close" -> R.string.device_action_close
    "lock" -> R.string.device_action_lock
    "unlock" -> R.string.device_action_unlock
    "armStay" -> R.string.device_action_arm_stay
    "armAway" -> R.string.device_action_arm_away
    "disarm" -> R.string.device_action_disarm
    "changeSecurityCode" -> R.string.device_action_change_security_code
    "setMode" -> R.string.device_action_set_mode
    "setFanSpeed" -> R.string.device_action_set_fan_speed
    "setVerticalSwing" -> R.string.device_action_set_vertical_swing
    "setHorizontalSwing" -> R.string.device_action_set_horizontal_swing
    "setConvection" -> R.string.device_action_set_convection
    "setGrill" -> R.string.device_action_set_grill
    "setHeat" -> R.string.device_action_set_heat
    "play" -> R.string.device_action_play
    "pause" -> R.string.device_action_pause
    "stop" -> R.string.device_action_stop
    "resume" -> R.string.device_action_resume
    "nextSong" -> R.string.device_action_next_song
    "previousSong" -> R.string.device_action_previous_song
    "setVolume" -> R.string.device_action_set_volume
    "setGenre" -> R.string.device_action_set_genre
    "start" -> R.string.device_action_start
    "dock" -> R.string.device_action_dock
    "dispense" -> R.string.device_action_dispense
    "getPlaylist" -> R.string.device_action_get_playlist
    else -> null
}

fun deviceActionName(context: Context, action: String): String =
    deviceActionNameRes(action)?.let { context.getString(it) } ?: action

@StringRes
fun deviceValueRes(value: String): Int? = when (value) {
    "cool" -> R.string.device_value_cool
    "heat" -> R.string.device_value_heat
    "fan" -> R.string.device_value_fan
    "dry" -> R.string.device_value_dry
    "auto" -> R.string.device_value_auto
    "conventional" -> R.string.device_value_conventional
    "bottom" -> R.string.device_value_bottom
    "top" -> R.string.device_value_top
    "large" -> R.string.device_value_large
    "off" -> R.string.device_value_off
    "normal" -> R.string.device_value_normal
    "eco" -> R.string.device_value_eco
    "default" -> R.string.device_value_default
    "vacation" -> R.string.device_value_vacation
    "party" -> R.string.device_value_party
    "vacuum" -> R.string.device_value_vacuum
    "mop" -> R.string.device_value_mop
    "classical" -> R.string.device_value_classical
    "country" -> R.string.device_value_country
    "dance" -> R.string.device_value_dance
    "latina" -> R.string.device_value_latina
    "pop" -> R.string.device_value_pop
    "rock" -> R.string.device_value_rock
    "low" -> R.string.device_value_low
    "medium" -> R.string.device_value_medium
    "high" -> R.string.device_value_high
    else -> null
}

fun deviceValueLabel(context: Context, value: String): String =
    deviceValueRes(value)?.let { context.getString(it) } ?: value

// Indica si el dispositivo debe presentarse como "activo" en listas y resumenes.
fun isDeviceActive(device: Device): Boolean {
    val status = device.state.status
    if (status == "armedStay" || status == "armedAway") return true
    if (status == "disarmed") return false
    if (device.type.id == DeviceTypeIds.DOOR) return status == "closed"
    if (status == "opened") return true
    if (status == "closed") return false
    if (status == "on" || status == "active" || status == "playing") return true
    if (status == "off") return false
    if (device.state.lock != null) return device.state.lock == "locked"
    return status == null
}

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

fun deviceStateText(context: Context, device: Device): String {
    val state = device.state
    when (state.status) {
        "armedStay" -> return context.getString(R.string.device_state_armed_stay)
        "armedAway" -> return context.getString(R.string.device_state_armed_away)
        "disarmed" -> return context.getString(R.string.device_state_disarmed)
        "opened" -> return if (state.level != null) {
            context.getString(R.string.device_state_opened_level, state.level)
        } else {
            context.getString(R.string.device_state_opened)
        }
        "closed" -> return context.getString(R.string.device_state_closed)
    }
    if (state.lock != null) {
        return context.getString(
            if (state.lock == "locked") R.string.device_state_locked else R.string.device_state_unlocked
        )
    }
    if (state.temperature != null) {
        val temp = formatNumber(state.temperature)
        if (device.type.id == DeviceTypeIds.REFRIGERATOR) {
            return if (state.freezerTemperature != null) {
                context.getString(R.string.device_state_fridge, temp, formatNumber(state.freezerTemperature))
            } else {
                context.getString(R.string.device_state_temp, temp)
            }
        }
        if (state.status != "on") return context.getString(R.string.device_state_off)
        if (device.type.id == DeviceTypeIds.OVEN) {
            return context.getString(R.string.device_state_oven_on, temp)
        }
        if (state.mode != null) {
            return context.getString(R.string.device_state_temp_mode, temp, deviceValueLabel(context, state.mode))
        }
        return context.getString(R.string.device_state_temp, temp)
    }
    if (state.brightness != null) {
        return if (state.status == "on") {
            context.getString(R.string.device_state_brightness, state.brightness)
        } else {
            context.getString(R.string.device_state_off_feminine)
        }
    }
    if (state.level != null) {
        return context.getString(R.string.device_state_level_open, state.level)
    }
    if (state.volume != null) {
        if (state.status != "on" && state.status != "playing" && state.status != "active") {
            return context.getString(R.string.device_state_off)
        }
        return if (state.genre != null) {
            context.getString(
                R.string.device_state_playing_genre_volume,
                deviceValueLabel(context, state.genre),
                state.volume,
            )
        } else {
            context.getString(R.string.device_state_playing_volume, state.volume)
        }
    }
    if (state.status == "on" || state.status == "active") {
        return if (state.mode != null) {
            context.getString(R.string.device_state_active_mode, deviceValueLabel(context, state.mode))
        } else {
            context.getString(R.string.device_state_active)
        }
    }
    return context.getString(R.string.device_state_off)
}
