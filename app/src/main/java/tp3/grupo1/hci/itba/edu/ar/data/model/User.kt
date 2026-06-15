package tp3.grupo1.hci.itba.edu.ar.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class User(
    val id: String,
    val email: String,
    val name: String? = null,
    val isVerified: Boolean? = null,
    val createdAt: String? = null,
    val metadata: JsonObject? = null,
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: User? = null,
)

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class UpdateProfileRequest(
    val name: String? = null,
    val metadata: JsonObject? = null,
)

@Serializable
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String,
)

@Serializable
data class VerifyAccountRequest(
    val code: String,
)

@Serializable
data class EmailRequest(
    val email: String,
)

// send-verification devuelve el codigo OTP
@Serializable
data class CodeResponse(
    val code: String? = null,
)

@Serializable
data class ResetPasswordRequest(
    val code: String,
    val password: String,
)
