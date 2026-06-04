package tp3.grupo1.hci.itba.edu.ar.domain

import androidx.annotation.StringRes
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.data.model.Device
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceState
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceType
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceTypeAction

/**
 * Dynamic control "atoms": given a device type's action catalog and the
 * current device state, this module decides which controls the detail screen
 * shows. Direct port of the web app's deviceHelpers.ts so both clients behave
 * identically.
 */
sealed interface ControlAtom

data class PowerAtom(
    val onAction: String,
    val offAction: String,
    val active: Boolean,
    @field:StringRes val activeLabelRes: Int,
    @field:StringRes val inactiveLabelRes: Int,
    @field:StringRes val sectionLabelRes: Int? = null,
) : ControlAtom

data class SliderAtom(
    val action: String,
    val paramName: String,
    val min: Int,
    val max: Int,
    val value: Int,
    @field:StringRes val labelRes: Int?,
    val rawName: String,
    val unit: String,
) : ControlAtom

enum class SelectKind { MODE, FAN_SPEED, SWING_VERTICAL, SWING_HORIZONTAL, GENERIC }

data class SelectAtom(
    val action: String,
    val paramName: String,
    val options: List<String>,
    val value: String,
    @field:StringRes val labelRes: Int?,
    val rawName: String,
    val kind: SelectKind,
) : ControlAtom

data class ButtonAtom(
    val action: String,
    @field:StringRes val labelRes: Int?,
    val rawName: String,
) : ControlAtom

data class ColorAtom(
    val action: String,
    val paramName: String,
    val value: String,
) : ControlAtom

data class LampPreviewAtom(
    val color: String,
    val brightness: Int,
    val active: Boolean,
) : ControlAtom

data class AlarmStatusAtom(
    val currentStatus: String?,
) : ControlAtom

data class AlarmArmAction(
    val action: String,
    @field:StringRes val labelRes: Int,
)

/** Alarm arm/disarm selector. Every transition requires the security code. */
data class AlarmAtom(
    val armActions: List<AlarmArmAction>,
    val disarmAction: String,
) : ControlAtom

data object ChangeCodeAtom : ControlAtom

data class DispenseAtom(
    val action: String,
    val quantityParam: String,
    val unitParam: String,
    val units: List<String>,
    val quantityMin: Int,
    val quantityMax: Int,
) : ControlAtom

data class PlaybackAtom(
    val play: String?,
    val stop: String?,
    val pause: String?,
    val resume: String?,
    val next: String?,
    val prev: String?,
    val status: String?,
) : ControlAtom

data class PlaylistAtom(
    val action: String,
) : ControlAtom

/**
 * Vacuum "send to room" action: a string param without supported values that
 * is populated with the rooms of the current home at render time.
 */
data class SetLocationAtom(
    val action: String,
    val paramName: String,
) : ControlAtom

// ── Detection tables ─────────────────────────────────────────────────────────

private val POWER_PAIRS: List<Pair<String, String>> = listOf(
    "turnOn" to "turnOff",
    "open" to "close",
    "lock" to "unlock",
    "play" to "stop",
    "up" to "down",
    "start" to "pause",
    "start" to "stop",
)

private val TOGGLE_LABELS: Map<String, Pair<Int, Int>> = mapOf(
    "turnOn" to (R.string.device_state_on to R.string.device_state_off),
    "lock" to (R.string.device_state_locked to R.string.device_state_unlocked),
    "open" to (R.string.device_state_opened to R.string.device_state_closed),
    "up" to (R.string.device_state_up to R.string.device_state_down),
    "play" to (R.string.device_state_playing to R.string.device_state_stopped),
    "start" to (R.string.device_state_on to R.string.device_state_off),
)

// Semantically equivalent pairs: when one is detected the others are consumed
private val REDUNDANT_PAIRS: Map<String, List<String>> = mapOf(
    "turnOn" to listOf("start", "play"),
    "start" to listOf("turnOn", "play"),
    "play" to listOf("turnOn", "start"),
    "open" to listOf("up"),
    "up" to listOf("open"),
)

// Param-less actions that are handled by dedicated atoms, not generic buttons
private val SKIP_AS_BUTTON = setOf("getPlaylist")

private fun findAction(actions: List<DeviceTypeAction>, name: String): DeviceTypeAction? =
    actions.firstOrNull { it.name == name }

private fun stateValue(state: DeviceState, key: String): String? = when (key) {
    "mode" -> state.mode
    "fanSpeed" -> state.fanSpeed
    "verticalSwing" -> state.verticalSwing
    "horizontalSwing" -> state.horizontalSwing
    "heat" -> state.heat
    "grill" -> state.grill
    "convection" -> state.convection
    "genre" -> state.genre
    "color" -> state.color
    "status" -> state.status
    else -> null
}

private fun numericStateValue(state: DeviceState, key: String): Int? = when (key) {
    "temperature" -> state.temperature?.toInt()
    "freezerTemperature" -> state.freezerTemperature?.toInt()
    "brightness" -> state.brightness
    "level" -> state.level
    "volume" -> state.volume
    else -> null
}

// ── Detectors (same order and semantics as the web app) ─────────────────────

private fun detectAlarm(actions: List<DeviceTypeAction>, device: Device): List<ControlAtom>? {
    val armStay = findAction(actions, "armStay")
    val armAway = findAction(actions, "armAway")
    val disarm = findAction(actions, "disarm")
    if (armStay == null && armAway == null && disarm == null) return null
    val atoms = mutableListOf<ControlAtom>(
        AlarmStatusAtom(device.state.status),
        AlarmAtom(
            armActions = listOfNotNull(
                armStay?.let { AlarmArmAction("armStay", R.string.device_action_arm_stay) },
                armAway?.let { AlarmArmAction("armAway", R.string.device_action_arm_away) },
            ),
            disarmAction = disarm?.name ?: "disarm",
        ),
    )
    if (findAction(actions, "changeSecurityCode") != null) atoms.add(ChangeCodeAtom)
    return atoms
}

private fun detectLampPreview(device: Device): LampPreviewAtom? {
    val brightness = device.state.brightness ?: return null
    val color = device.state.color ?: return null
    return LampPreviewAtom(color = color, brightness = brightness, active = device.state.status == "on")
}

private fun detectPlayback(
    actions: List<DeviceTypeAction>,
    device: Device,
    used: MutableSet<String>,
): PlaybackAtom? {
    if (findAction(actions, "nextSong") == null && findAction(actions, "previousSong") == null) return null
    val names = listOf("play", "stop", "pause", "resume", "nextSong", "previousSong")
    names.forEach { if (findAction(actions, it) != null) used.add(it) }
    return PlaybackAtom(
        play = "play".takeIf { findAction(actions, it) != null },
        stop = "stop".takeIf { findAction(actions, it) != null },
        pause = "pause".takeIf { findAction(actions, it) != null },
        resume = "resume".takeIf { findAction(actions, it) != null },
        next = "nextSong".takeIf { findAction(actions, it) != null },
        prev = "previousSong".takeIf { findAction(actions, it) != null },
        status = device.state.status,
    )
}

private fun detectPower(
    actions: List<DeviceTypeAction>,
    device: Device,
    used: MutableSet<String>,
): PowerAtom? {
    for ((on, off) in POWER_PAIRS) {
        if (used.contains(on)) continue
        if (findAction(actions, on) == null || findAction(actions, off) == null) continue
        used.add(on)
        used.add(off)
        for (redundantOn in REDUNDANT_PAIRS[on].orEmpty()) {
            val redundantOff = POWER_PAIRS.firstOrNull { it.first == redundantOn }?.second
            if (findAction(actions, redundantOn) != null) used.add(redundantOn)
            if (redundantOff != null && findAction(actions, redundantOff) != null) used.add(redundantOff)
        }
        // Doors invert the toggle: "active" means safely closed
        if (on == "open" && device.type.id == DeviceTypeIds.DOOR) {
            return PowerAtom(
                onAction = "close",
                offAction = "open",
                active = device.state.status == "closed",
                activeLabelRes = R.string.device_state_closed,
                inactiveLabelRes = R.string.device_state_opened,
            )
        }
        val isLock = on == "lock"
        val active = if (isLock) {
            device.state.lock == "locked"
        } else {
            device.state.status in listOf("on", "opened", "active")
        }
        val labels = TOGGLE_LABELS[on] ?: (R.string.device_state_active to R.string.device_state_off)
        return PowerAtom(
            onAction = on,
            offAction = off,
            active = active,
            activeLabelRes = labels.first,
            inactiveLabelRes = labels.second,
        )
    }
    return null
}

private fun detectLock(
    actions: List<DeviceTypeAction>,
    device: Device,
    used: MutableSet<String>,
): PowerAtom? {
    if (used.contains("lock")) return null
    if (findAction(actions, "lock") == null || findAction(actions, "unlock") == null) return null
    used.add("lock")
    used.add("unlock")
    return PowerAtom(
        onAction = "lock",
        offAction = "unlock",
        active = device.state.lock == "locked",
        activeLabelRes = R.string.device_state_locked,
        inactiveLabelRes = R.string.device_state_unlocked,
        sectionLabelRes = R.string.device_action_lock,
    )
}

private fun detectDispense(actions: List<DeviceTypeAction>, used: MutableSet<String>): DispenseAtom? {
    val action = findAction(actions, "dispense") ?: return null
    used.add("dispense")
    val quantityParam = action.params.firstOrNull { it.isNumeric }
    val unitParam = action.params.firstOrNull { it.type == "string" && it.supportedValues != null }
    return DispenseAtom(
        action = "dispense",
        quantityParam = quantityParam?.name ?: "quantity",
        unitParam = unitParam?.name ?: "unit",
        units = unitParam?.supportedValuesList().orEmpty(),
        quantityMin = quantityParam?.minNumber?.toInt() ?: 1,
        quantityMax = quantityParam?.maxNumber?.toInt() ?: 100,
    )
}

private fun sliderUnit(actionName: String): String = when {
    actionName.contains("Temperature") -> "°C"
    actionName.contains("Brightness") || actionName.contains("Level") -> "%"
    else -> ""
}

private fun detectSliders(
    actions: List<DeviceTypeAction>,
    device: Device,
    used: MutableSet<String>,
): List<SliderAtom> = actions.mapNotNull { action ->
    if (used.contains(action.name)) return@mapNotNull null
    val param = action.params.firstOrNull() ?: return@mapNotNull null
    val min = param.minNumber
    val max = param.maxNumber
    if (!param.isNumeric || min == null || max == null) return@mapNotNull null
    used.add(action.name)
    val stateKey = if (action.name == "setFreezerTemperature") "freezerTemperature" else param.name.orEmpty()
    SliderAtom(
        action = action.name,
        paramName = param.name.orEmpty(),
        min = min.toInt(),
        max = max.toInt(),
        value = numericStateValue(device.state, stateKey) ?: min.toInt(),
        labelRes = deviceActionNameRes(action.name),
        rawName = action.name,
        unit = sliderUnit(action.name),
    )
}

private fun detectColor(
    actions: List<DeviceTypeAction>,
    device: Device,
    used: MutableSet<String>,
): ColorAtom? {
    val action = findAction(actions, "setColor") ?: return null
    val param = action.params.firstOrNull()
    if (param == null || param.type != "string") return null
    used.add("setColor")
    return ColorAtom(
        action = "setColor",
        paramName = param.name ?: "color",
        value = device.state.color ?: "#FFFFFF",
    )
}

private fun selectKind(actionName: String): SelectKind = when (actionName) {
    "setFanSpeed" -> SelectKind.FAN_SPEED
    "setVerticalSwing" -> SelectKind.SWING_VERTICAL
    "setHorizontalSwing" -> SelectKind.SWING_HORIZONTAL
    "setMode" -> SelectKind.MODE
    else -> SelectKind.GENERIC
}

private fun detectSelects(
    actions: List<DeviceTypeAction>,
    device: Device,
    used: MutableSet<String>,
): List<SelectAtom> = actions.mapNotNull { action ->
    if (used.contains(action.name)) return@mapNotNull null
    val param = action.params.firstOrNull() ?: return@mapNotNull null
    if (param.type != "string" || param.supportedValues == null) return@mapNotNull null
    used.add(action.name)
    val options = param.supportedValuesList()
    SelectAtom(
        action = action.name,
        paramName = param.name.orEmpty(),
        options = options,
        value = stateValue(device.state, param.name.orEmpty()) ?: options.firstOrNull().orEmpty(),
        labelRes = deviceActionNameRes(action.name),
        rawName = action.name,
        kind = selectKind(action.name),
    )
}

private fun detectButtons(actions: List<DeviceTypeAction>, used: MutableSet<String>): List<ButtonAtom> =
    actions
        .filter { !used.contains(it.name) && !SKIP_AS_BUTTON.contains(it.name) && it.params.isEmpty() }
        .map { ButtonAtom(action = it.name, labelRes = deviceActionNameRes(it.name), rawName = it.name) }

private fun detectPlaylist(actions: List<DeviceTypeAction>, used: MutableSet<String>): PlaylistAtom? {
    findAction(actions, "getPlaylist") ?: return null
    used.add("getPlaylist")
    return PlaylistAtom(action = "getPlaylist")
}

private fun detectSetLocation(actions: List<DeviceTypeAction>, used: MutableSet<String>): SetLocationAtom? {
    val action = findAction(actions, "setLocation") ?: return null
    used.add("setLocation")
    return SetLocationAtom(
        action = "setLocation",
        paramName = action.params.firstOrNull()?.name ?: "roomId",
    )
}

// ── Public API ───────────────────────────────────────────────────────────────

fun deviceControls(deviceType: DeviceType, device: Device): List<ControlAtom> {
    val actions = deviceType.actions
    val used = mutableSetOf<String>()

    detectAlarm(actions, device)?.let { return it }

    return listOfNotNull(
        detectLampPreview(device),
        detectPlayback(actions, device, used),
        detectPower(actions, device, used),
        detectLock(actions, device, used),
        detectDispense(actions, used),
        detectSetLocation(actions, used),
    ) + detectSliders(actions, device, used) +
        listOfNotNull(detectColor(actions, device, used)) +
        detectSelects(actions, device, used) +
        detectButtons(actions, used) +
        listOfNotNull(detectPlaylist(actions, used))
}
