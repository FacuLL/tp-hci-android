package tp3.grupo1.hci.itba.edu.ar.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

@Serializable
data class DeviceType(
    val id: String,
    val name: String,
    val powerUsage: Double? = null,
    val actions: List<DeviceTypeAction> = emptyList(),
)

@Serializable
data class DeviceTypeAction(
    val name: String,
    val params: List<DeviceTypeActionParam> = emptyList(),
)

@Serializable
data class DeviceTypeActionParam(
    val name: String? = null,
    val type: String,
    // minValue/maxValue are numbers for sliders but hex strings for setColor,
    // so they are kept raw and exposed through the numeric accessors below.
    val minValue: JsonPrimitive? = null,
    val maxValue: JsonPrimitive? = null,
    // The API sends either a space-separated string or an array of strings
    val supportedValues: JsonElement? = null,
    val description: String? = null,
) {
    fun supportedValuesList(): List<String> = when (supportedValues) {
        is JsonArray -> supportedValues.mapNotNull { (it as? JsonPrimitive)?.content }
        is JsonPrimitive -> supportedValues.content.split(' ').filter { it.isNotBlank() }
        else -> emptyList()
    }

    val minNumber: Double? get() = minValue?.doubleOrNull
    val maxNumber: Double? get() = maxValue?.doubleOrNull

    val isNumeric: Boolean
        get() = type == "integer" || type == "number"
}
