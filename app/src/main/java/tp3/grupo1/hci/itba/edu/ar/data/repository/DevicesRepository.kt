package tp3.grupo1.hci.itba.edu.ar.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import tp3.grupo1.hci.itba.edu.ar.data.model.Device
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceCreateRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceUpdateRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.EntityRef
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiException
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiProvider
import tp3.grupo1.hci.itba.edu.ar.data.network.apiCall
import tp3.grupo1.hci.itba.edu.ar.data.notifications.SelfActionTracker
import tp3.grupo1.hci.itba.edu.ar.domain.applyEventArgs
import tp3.grupo1.hci.itba.edu.ar.domain.predictStateChange

class DevicesRepository(
    private val api: ApiProvider,
    private val selfActions: SelfActionTracker,
) {

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    suspend fun refresh() {
        _devices.value = apiCall { api.devices.getAll() }
    }

    fun clear() {
        _devices.value = emptyList()
    }

    suspend fun create(name: String, typeId: String, roomId: String?): Device {
        val request = DeviceCreateRequest(
            name = name,
            type = EntityRef(typeId),
            room = roomId?.let { EntityRef(it) },
        )
        val device = apiCall { api.devices.create(request) }
        // Suppress the notification for the deviceCreated event this triggers.
        selfActions.record(SelfActionTracker.deviceCreated(device.id))
        // The socket may broadcast deviceCreated before this response arrives,
        // so insert defensively to avoid duplicating the device in the list.
        upsertDevice(device)
        return device
    }

    suspend fun rename(deviceId: String, name: String): Device {
        val device = apiCall { api.devices.update(deviceId, DeviceUpdateRequest(name)) }
        replaceDevice(device)
        return device
    }

    suspend fun delete(deviceId: String) {
        selfActions.record(SelfActionTracker.deviceDeleted(deviceId))
        apiCall { api.devices.delete(deviceId).close() }
        _devices.update { list -> list.filterNot { it.id == deviceId } }
    }

    /**
     * Executes an action and updates the local state optimistically instead
     * of re-querying the device (the web version was marked down for issuing
     * extra state requests after each action). WebSocket events reconcile the
     * real state shortly after.
     */
    suspend fun execute(deviceId: String, action: String, params: List<JsonElement> = emptyList()) {
        // Suppress notifications for the deviceEvent this action will echo back.
        selfActions.record(SelfActionTracker.deviceState(deviceId))
        apiCall { api.devices.executeAction(deviceId, action, JsonArray(params)) }
        _devices.update { list ->
            list.map { device ->
                if (device.id == deviceId) {
                    device.copy(state = predictStateChange(device.state, action, params))
                } else {
                    device
                }
            }
        }
    }

    /**
     * Executes an action whose response payload matters (e.g. getPlaylist)
     * and returns it raw.
     */
    suspend fun executeForResult(deviceId: String, action: String, params: List<JsonElement> = emptyList()): JsonElement =
        apiCall { api.devices.executeAction(deviceId, action, JsonArray(params)) }

    /**
     * Moves a device between rooms enforcing the API rule that a device must
     * be detached from its current room before joining another one. If the
     * re-assignment fails after a successful detach, the original room is
     * restored on a best-effort basis before rethrowing.
     */
    suspend fun moveToRoom(device: Device, roomId: String?): Device {
        val currentRoomId = device.room?.id
        if (roomId == currentRoomId) return device
        if (roomId == null) return removeFromRoom(device.id)
        if (currentRoomId == null) return assignToRoom(device.id, roomId)
        removeFromRoom(device.id)
        return try {
            assignToRoom(device.id, roomId)
        } catch (e: ApiException) {
            runCatching { assignToRoom(device.id, currentRoomId) }
            throw e
        }
    }

    suspend fun assignToRoom(deviceId: String, roomId: String): Device {
        val device = apiCall { api.rooms.addDevice(roomId, deviceId) }
        replaceDevice(device)
        return device
    }

    suspend fun removeFromRoom(deviceId: String): Device {
        // The endpoint returns void, so the cached device is updated with its
        // room cleared instead of relying on the (empty) response body.
        apiCall { api.rooms.removeDevice(deviceId).close() }
        val updated = _devices.value.firstOrNull { it.id == deviceId }?.copy(room = null)
        if (updated != null) replaceDevice(updated)
        return updated ?: throw ApiException.network()
    }

    // ── WebSocket reconciliation ──

    fun applyDeviceCreated(device: Device) {
        upsertDevice(device)
    }

    private fun upsertDevice(device: Device) {
        _devices.update { list ->
            if (list.any { it.id == device.id }) list.map { if (it.id == device.id) device else it }
            else list + device
        }
    }

    fun applyDeviceUpdated(device: Device) {
        replaceDevice(device)
    }

    fun applyDeviceDeleted(deviceId: String) {
        _devices.update { list -> list.filterNot { it.id == deviceId } }
    }

    fun applyStateEvent(deviceId: String, args: JsonObject) {
        _devices.update { list ->
            list.map { device ->
                if (device.id == deviceId) device.copy(state = device.state.applyEventArgs(args))
                else device
            }
        }
    }

    private fun replaceDevice(device: Device) {
        _devices.update { list -> list.map { if (it.id == device.id) device else it } }
    }
}
