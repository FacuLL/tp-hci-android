package tp3.grupo1.hci.itba.edu.ar.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Device(
    val id: String,
    val name: String,
    val type: EntityRef,
    val state: DeviceState = DeviceState(),
    val room: RoomSummary? = null,
    val metadata: JsonObject? = null,
)

/** Room data embedded in a device response. */
@Serializable
data class RoomSummary(
    val id: String,
    val name: String? = null,
    val home: HomeSummary? = null,
)

/** Home data embedded in a room response. */
@Serializable
data class HomeSummary(
    val id: String,
    val name: String? = null,
)

@Serializable
data class DeviceState(
    val status: String? = null,
    val brightness: Int? = null,
    val color: String? = null,
    val temperature: Double? = null,
    val freezerTemperature: Double? = null,
    val mode: String? = null,
    val verticalSwing: String? = null,
    val horizontalSwing: String? = null,
    val fanSpeed: String? = null,
    val level: Int? = null,
    val lock: String? = null,
    val heat: String? = null,
    val grill: String? = null,
    val convection: String? = null,
    val volume: Int? = null,
    val genre: String? = null,
    val batteryLevel: Int? = null,
    val securityCode: String? = null,
)

@Serializable
data class DeviceCreateRequest(
    val name: String,
    val type: EntityRef,
    val room: EntityRef? = null,
    val metadata: JsonObject? = null,
)

@Serializable
data class DeviceUpdateRequest(
    val name: String,
    val metadata: JsonObject? = null,
)
