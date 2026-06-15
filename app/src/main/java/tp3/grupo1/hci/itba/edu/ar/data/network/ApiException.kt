package tp3.grupo1.hci.itba.edu.ar.data.network

import androidx.annotation.StringRes
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import tp3.grupo1.hci.itba.edu.ar.R
import java.io.IOException

// Lleva la descripcion cruda del backend (en ingles) y un recurso localizado para que la UI no mezcle idiomas
class ApiException(
    val httpStatus: Int,
    val apiCode: Int? = null,
    val apiDescription: String? = null,
    @field:StringRes val userMessageRes: Int = R.string.error_generic,
) : Exception(apiDescription ?: "HTTP $httpStatus") {

    companion object {
        fun network(): ApiException =
            ApiException(0, null, "Network error", R.string.error_network)
    }
}

private val errorTranslations: List<Pair<Regex, Int>> = listOf(
    Regex("invalid (email|user(name)?) or password|invalid credentials|wrong password", RegexOption.IGNORE_CASE)
        to R.string.error_invalid_credentials,
    Regex("not verified|account.*not.*verified|user.*not.*verified|email.*not.*verified", RegexOption.IGNORE_CASE)
        to R.string.error_not_verified,
    Regex("already verified", RegexOption.IGNORE_CASE)
        to R.string.error_already_verified,
    Regex("already (registered|exists|in use)|email.*already|user.*already", RegexOption.IGNORE_CASE)
        to R.string.error_email_taken,
    Regex("user( with email .*)? not found|no user|account not found|email not found", RegexOption.IGNORE_CASE)
        to R.string.error_user_not_found,
    Regex("(invalid|wrong) (otp|code|verification code|reset code)", RegexOption.IGNORE_CASE)
        to R.string.error_invalid_code,
    Regex("(otp|code).*expired|expired (otp|code)", RegexOption.IGNORE_CASE)
        to R.string.error_code_expired,
    Regex("(old|current) password.*incorrect|incorrect (old|current) password", RegexOption.IGNORE_CASE)
        to R.string.error_wrong_old_password,
    Regex("password.*too short|password must.*characters", RegexOption.IGNORE_CASE)
        to R.string.error_password_short,
    Regex("unauthorized|invalid token|token expired|session expired|no token provided", RegexOption.IGNORE_CASE)
        to R.string.error_session_expired,
    Regex("forbidden|not allowed|no permission", RegexOption.IGNORE_CASE)
        to R.string.error_forbidden,
    Regex("action '?\\w+'? is not valid for device", RegexOption.IGNORE_CASE)
        to R.string.error_action_unavailable,
    Regex("device.*not found", RegexOption.IGNORE_CASE)
        to R.string.error_device_not_found,
    Regex("room.*not found", RegexOption.IGNORE_CASE)
        to R.string.error_room_not_found,
    Regex("home.*not found", RegexOption.IGNORE_CASE)
        to R.string.error_home_not_found,
    Regex("routine.*not found", RegexOption.IGNORE_CASE)
        to R.string.error_routine_not_found,
    Regex("validation error|invalid (input|data|request|body)|bad request", RegexOption.IGNORE_CASE)
        to R.string.error_invalid_data,
    Regex("internal server error|server error", RegexOption.IGNORE_CASE)
        to R.string.error_server,
    Regex("too many requests|rate limit", RegexOption.IGNORE_CASE)
        to R.string.error_rate_limit,
)

@StringRes
fun translateApiError(description: String?): Int {
    if (description.isNullOrBlank()) return R.string.error_generic
    return errorTranslations.firstOrNull { it.first.containsMatchIn(description) }?.second
        ?: R.string.error_generic
}

private val errorJson = Json { ignoreUnknownKeys = true }

fun HttpException.toApiException(): ApiException {
    val status = code()
    var apiCode: Int? = null
    var description: String? = null
    try {
        val body = response()?.errorBody()?.string()
        if (!body.isNullOrBlank()) {
            val error = errorJson.parseToJsonElement(body).jsonObject["error"] as? JsonObject
            apiCode = error?.get("code")?.jsonPrimitive?.intOrNull
            description = error?.get("description")?.jsonPrimitive?.contentOrNull
        }
    } catch (_: Exception) {
        // El body no era el JSON esperado; se usa el status HTTP como fallback
    }
    return ApiException(status, apiCode, description ?: "HTTP $status", translateApiError(description))
}

// Mapea cualquier fallo a ApiException para que los ViewModels manejen un solo tipo de error
suspend fun <T> apiCall(block: suspend () -> T): T = try {
    block()
} catch (e: ApiException) {
    throw e
} catch (e: HttpException) {
    throw e.toApiException()
} catch (e: SerializationException) {
    throw ApiException(0, null, e.message, R.string.error_generic)
} catch (e: IOException) {
    throw ApiException.network()
}
