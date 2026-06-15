package tp3.grupo1.hci.itba.edu.ar.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import tp3.grupo1.hci.itba.edu.ar.data.model.EntityRef
import tp3.grupo1.hci.itba.edu.ar.data.model.Room
import tp3.grupo1.hci.itba.edu.ar.data.model.RoomCreateRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.RoomUpdateRequest
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiProvider
import tp3.grupo1.hci.itba.edu.ar.data.network.apiCall

/** Key under [Room.metadata] that holds the preferred device order. */
const val ROOM_META_DEVICE_ORDER = "deviceOrder"

/** Rooms of the currently selected home. */
class RoomsRepository(private val api: ApiProvider) {

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

    suspend fun refreshForHome(homeId: String) {
        _rooms.value = apiCall { api.homes.getRooms(homeId) }
    }

    fun clear() {
        _rooms.value = emptyList()
    }

    suspend fun create(name: String, homeId: String): Room {
        val room = apiCall { api.rooms.create(RoomCreateRequest(name, EntityRef(homeId))) }
        _rooms.update { it + room }
        return room
    }

    suspend fun rename(room: Room, name: String): Room {
        val updated = apiCall { api.rooms.update(room.id, RoomUpdateRequest(name, room.home)) }
        _rooms.update { list -> list.map { if (it.id == updated.id) updated else it } }
        return updated
    }

    suspend fun delete(roomId: String) {
        apiCall { api.rooms.delete(roomId).close() }
        _rooms.update { list -> list.filterNot { it.id == roomId } }
    }

    /**
     * Persists the user-defined device order under [Room.metadata]'s
     * [ROOM_META_DEVICE_ORDER] key. The API does not expose ordering as a
     * first-class field, so we piggyback on the same metadata mechanism the
     * web client uses for routines.
     */
    suspend fun setDeviceOrder(room: Room, deviceIds: List<String>): Room {
        val merged = mergeOrderIntoMetadata(room.metadata, deviceIds)
        val updated = apiCall {
            api.rooms.update(room.id, RoomUpdateRequest(room.name, room.home, merged))
        }
        _rooms.update { list -> list.map { if (it.id == updated.id) updated else it } }
        return updated
    }

    private fun mergeOrderIntoMetadata(
        current: JsonObject?,
        deviceIds: List<String>,
    ): JsonObject {
        val entries = current?.toMutableMap() ?: mutableMapOf()
        entries[ROOM_META_DEVICE_ORDER] = JsonArray(deviceIds.map { JsonPrimitive(it) })
        return JsonObject(entries)
    }
}
