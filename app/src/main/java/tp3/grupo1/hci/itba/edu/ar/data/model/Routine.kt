package tp3.grupo1.hci.itba.edu.ar.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class Routine(
    val id: String,
    val name: String,
    val actions: List<RoutineAction> = emptyList(),
    val metadata: JsonObject? = null,
) {
    val schedule: RoutineSchedule
        get() {
            val enabled = (metadata?.get("activa") as? JsonPrimitive)?.booleanOrNull ?: true
            val type = (metadata?.get("tipo") as? JsonPrimitive)?.contentOrNull
            val time = (metadata?.get("hora") as? JsonPrimitive)?.contentOrNull
            val days = (metadata?.get("dias") as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                ?: emptyList()
            return RoutineSchedule(
                enabled = enabled,
                isScheduled = type == "programada",
                time = time,
                days = days,
            )
        }
}

data class RoutineSchedule(
    val enabled: Boolean,
    val isScheduled: Boolean,
    val time: String?,
    val days: List<String>,
)

@Serializable
data class RoutineAction(
    val device: EntityRef,
    val actionName: String,
    val params: JsonArray = JsonArray(emptyList()),
)

@Serializable
data class RoutineActionResult(
    val device: String,
    val success: Boolean,
    val result: JsonElement? = null,
)

@Serializable
data class RoutineUpsertRequest(
    val name: String,
    val actions: List<RoutineAction>,
    val metadata: JsonObject,
)
