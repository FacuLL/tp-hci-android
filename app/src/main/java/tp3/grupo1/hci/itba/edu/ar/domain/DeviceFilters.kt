package tp3.grupo1.hci.itba.edu.ar.domain

import tp3.grupo1.hci.itba.edu.ar.data.model.Device
import tp3.grupo1.hci.itba.edu.ar.data.model.Room

// La API devuelve todos los dispositivos del usuario; esto acota al hogar elegido: los de sus habitaciones mas los sin asignar (globales a la cuenta).
fun devicesForHome(devices: List<Device>, homeRooms: List<Room>): List<Device> {
    val roomIds = homeRooms.mapTo(mutableSetOf()) { it.id }
    return devices.filter { it.room == null || it.room.id in roomIds }
}

fun devicesInRoom(devices: List<Device>, roomId: String): List<Device> =
    devices.filter { it.room?.id == roomId }

fun unassignedDevices(devices: List<Device>): List<Device> =
    devices.filter { it.room == null }
