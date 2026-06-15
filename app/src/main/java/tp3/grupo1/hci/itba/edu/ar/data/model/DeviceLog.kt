package tp3.grupo1.hci.itba.edu.ar.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

// Entrada del historial de acciones que expone la API en /devices/logs. `params` y `result`
// son heterogeneos: `params` puede ser un array o un escalar (ej. setLocation manda un string),
// y `result` un booleano o un valor. Por eso ambos se dejan como JsonElement crudo.
@Serializable
data class DeviceLog(
    val id: String,
    val timestamp: String,
    val deviceId: String,
    val actionName: String,
    val params: JsonElement? = null,
    val result: JsonElement? = null,
) {
    // Exito de la accion: false solo si la API lo reporta explicitamente; cualquier otro valor
    // devuelto (un numero, un string, una lista) cuenta como exito. null sin resultado -> sin icono.
    val resultBool: Boolean?
        get() = when (val r = result) {
            null, JsonNull -> null
            is JsonPrimitive -> r.booleanOrNull ?: true
            else -> true
        }
}
