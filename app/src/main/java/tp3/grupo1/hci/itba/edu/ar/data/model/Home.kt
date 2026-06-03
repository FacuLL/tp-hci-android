package tp3.grupo1.hci.itba.edu.ar.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Home(
    val id: String,
    val name: String,
    val metadata: JsonObject? = null,
    val sharedWith: List<HomeSharedUser> = emptyList(),
)

@Serializable
data class HomeSharedUser(
    val email: String,
    val id: String? = null,
    val name: String? = null,
    val sharedAt: String? = null,
)

@Serializable
data class HomeCreateRequest(
    val name: String,
    val metadata: JsonObject? = null,
)

@Serializable
data class HomeUpdateRequest(
    val name: String? = null,
    val metadata: JsonObject? = null,
)

@Serializable
data class HomeShareRequest(
    val emails: List<String>,
)
