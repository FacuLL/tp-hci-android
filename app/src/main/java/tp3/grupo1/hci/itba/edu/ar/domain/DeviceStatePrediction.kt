package tp3.grupo1.hci.itba.edu.ar.domain

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceState

private fun firstString(params: List<JsonElement>): String? =
    (params.firstOrNull() as? JsonPrimitive)?.contentOrNull

private fun firstInt(params: List<JsonElement>): Int? =
    (params.firstOrNull() as? JsonPrimitive)?.intOrNull
        ?: (params.firstOrNull() as? JsonPrimitive)?.doubleOrNull?.toInt()

private fun firstDouble(params: List<JsonElement>): Double? =
    (params.firstOrNull() as? JsonPrimitive)?.doubleOrNull

/**
 * Predicts the device state after an action succeeds, so the UI updates
 * immediately without re-querying the API. WebSocket events deliver the
 * authoritative state right after.
 */
fun predictStateChange(state: DeviceState, action: String, params: List<JsonElement>): DeviceState =
    when (action) {
        "turnOn" -> state.copy(status = "on")
        "turnOff" -> state.copy(status = "off")
        "open", "up" -> state.copy(status = "opened")
        "close", "down" -> state.copy(status = "closed")
        "lock" -> state.copy(lock = "locked")
        "unlock" -> state.copy(lock = "unlocked")
        "armStay" -> state.copy(status = "armedStay")
        "armAway" -> state.copy(status = "armedAway")
        "disarm" -> state.copy(status = "disarmed")
        "play", "resume" -> state.copy(status = "playing")
        "pause" -> state.copy(status = "paused")
        "stop" -> state.copy(status = "stopped")
        "start" -> state.copy(status = "active")
        "dock" -> state.copy(status = "docked")
        "setBrightness" -> state.copy(brightness = firstInt(params) ?: state.brightness)
        "setTemperature" -> state.copy(temperature = firstDouble(params) ?: state.temperature)
        "setFreezerTemperature" -> state.copy(freezerTemperature = firstDouble(params) ?: state.freezerTemperature)
        "setColor" -> state.copy(color = firstString(params) ?: state.color)
        "setLevel" -> state.copy(level = firstInt(params) ?: state.level)
        "setMode" -> state.copy(mode = firstString(params) ?: state.mode)
        "setFanSpeed" -> state.copy(fanSpeed = firstString(params) ?: state.fanSpeed)
        "setVerticalSwing" -> state.copy(verticalSwing = firstString(params) ?: state.verticalSwing)
        "setHorizontalSwing" -> state.copy(horizontalSwing = firstString(params) ?: state.horizontalSwing)
        "setHeat" -> state.copy(heat = firstString(params) ?: state.heat)
        "setGrill" -> state.copy(grill = firstString(params) ?: state.grill)
        "setConvection" -> state.copy(convection = firstString(params) ?: state.convection)
        "setVolume" -> state.copy(volume = firstInt(params) ?: state.volume)
        "setGenre" -> state.copy(genre = firstString(params) ?: state.genre)
        else -> state
    }

/**
 * Applies the `args` of a WebSocket `deviceEvent` ("newStatus", "newBrightness",
 * etc.) onto the current state.
 */
fun DeviceState.applyEventArgs(args: JsonObject): DeviceState {
    var state = this
    for ((key, value) in args) {
        val primitive = value as? JsonPrimitive ?: continue
        state = when (key) {
            "newStatus" -> state.copy(status = primitive.contentOrNull ?: state.status)
            "newBrightness" -> state.copy(brightness = primitive.intOrNull ?: state.brightness)
            "newColor" -> state.copy(color = primitive.contentOrNull ?: state.color)
            "newTemperature" -> state.copy(temperature = primitive.doubleOrNull ?: state.temperature)
            "newFreezerTemperature" -> state.copy(freezerTemperature = primitive.doubleOrNull ?: state.freezerTemperature)
            "newMode" -> state.copy(mode = primitive.contentOrNull ?: state.mode)
            "newFanSpeed" -> state.copy(fanSpeed = primitive.contentOrNull ?: state.fanSpeed)
            "newVerticalSwing" -> state.copy(verticalSwing = primitive.contentOrNull ?: state.verticalSwing)
            "newHorizontalSwing" -> state.copy(horizontalSwing = primitive.contentOrNull ?: state.horizontalSwing)
            "newLevel" -> state.copy(level = primitive.intOrNull ?: state.level)
            "newLock" -> state.copy(lock = primitive.contentOrNull ?: state.lock)
            "newHeat" -> state.copy(heat = primitive.contentOrNull ?: state.heat)
            "newGrill" -> state.copy(grill = primitive.contentOrNull ?: state.grill)
            "newConvection" -> state.copy(convection = primitive.contentOrNull ?: state.convection)
            "newVolume" -> state.copy(volume = primitive.intOrNull ?: state.volume)
            "newGenre" -> state.copy(genre = primitive.contentOrNull ?: state.genre)
            else -> state
        }
    }
    return state
}
