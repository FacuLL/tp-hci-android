package tp3.grupo1.hci.itba.edu.ar.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

// Tarifa por defecto (ARS por kWh) cuando el hogar todavia no la configuro en su metadata.
const val DEFAULT_TARIFF: Double = 100.0

@Serializable
data class Home(
    val id: String,
    val name: String,
    val metadata: JsonObject? = null,
    val sharedWith: List<HomeSharedUser> = emptyList(),
) {
    // Precio por kWh guardado en metadata["tariff"]; cae al default si falta o es invalido.
    val tariff: Double
        get() = runCatching { metadata?.get("tariff")?.jsonPrimitive?.doubleOrNull }
            .getOrNull() ?: DEFAULT_TARIFF
}

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
