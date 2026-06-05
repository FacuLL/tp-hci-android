package tp3.grupo1.hci.itba.edu.ar.domain

import tp3.grupo1.hci.itba.edu.ar.data.model.Device
import tp3.grupo1.hci.itba.edu.ar.data.model.Room

/**
 * The API returns every device of the user, with rooms referenced only by id.
 * These helpers scope the list to the selected home: devices in one of its
 * rooms plus the unassigned ones (which are global to the account).
 */
fun devicesForHome(devices: List<Device>, homeRooms: List<Room>): List<Device> {
    val roomIds = homeRooms.mapTo(mutableSetOf()) { it.id }
    return devices.filter { it.room == null || it.room.id in roomIds }
}

fun devicesInRoom(devices: List<Device>, roomId: String): List<Device> =
    devices.filter { it.room?.id == roomId }

fun unassignedDevices(devices: List<Device>): List<Device> =
    devices.filter { it.room == null }
