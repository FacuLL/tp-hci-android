package tp3.grupo1.hci.itba.edu.ar.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Room(
    val id: String,
    val name: String,
    val home: EntityRef? = null,
    val metadata: JsonObject? = null,
)

@Serializable
data class RoomCreateRequest(
    val name: String,
    val home: EntityRef? = null,
    val metadata: JsonObject? = null,
)

@Serializable
data class RoomUpdateRequest(
    val name: String,
    val home: EntityRef? = null,
    val metadata: JsonObject? = null,
)

// Referencia generica { "id": ... } que usa la API para vincular entidades
@Serializable
data class EntityRef(
    val id: String,
)
