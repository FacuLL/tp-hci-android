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

/** Generic `{ "id": ... }` reference used by the API to link entities. */
@Serializable
data class EntityRef(
    val id: String,
)
