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
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceLog
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

    // Historial de acciones paginado. No se cachea: la pantalla Registros lo pide bajo demanda.
    suspend fun logs(limit: Int, offset: Int): List<DeviceLog> =
        apiCall { api.devices.getAllLogs(limit, offset) }

    suspend fun create(name: String, typeId: String, roomId: String?): Device {
        val request = DeviceCreateRequest(
            name = name,
            type = EntityRef(typeId),
            room = roomId?.let { EntityRef(it) },
        )
        val device = apiCall { api.devices.create(request) }
        // Suprime la notificacion del evento deviceCreated que esto dispara.
        selfActions.record(SelfActionTracker.deviceCreated(device.id))
        // El socket puede emitir deviceCreated antes de esta respuesta, por eso se inserta de forma defensiva para evitar duplicados.
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

    // Actualiza el estado local de forma optimista en vez de reconsultar el dispositivo; los eventos WebSocket reconcilian el estado real despues.
    suspend fun execute(deviceId: String, action: String, params: List<JsonElement> = emptyList()) {
        // Suprime notificaciones del deviceEvent que esta accion va a reenviar.
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

    // Ejecuta una accion cuya respuesta importa (ej. getPlaylist) y la devuelve sin procesar.
    suspend fun executeForResult(deviceId: String, action: String, params: List<JsonElement> = emptyList()): JsonElement =
        apiCall { api.devices.executeAction(deviceId, action, JsonArray(params)) }

    // La API exige desvincular el dispositivo de su habitacion actual antes de asignarlo a otra; si la reasignacion falla se restaura la original.
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
        // El endpoint no devuelve cuerpo, por eso se actualiza el dispositivo cacheado limpiando su habitacion.
        apiCall { api.rooms.removeDevice(deviceId).close() }
        val updated = _devices.value.firstOrNull { it.id == deviceId }?.copy(room = null)
        if (updated != null) replaceDevice(updated)
        return updated ?: throw ApiException.network()
    }

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
