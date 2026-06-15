package tp3.grupo1.hci.itba.edu.ar.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tp3.grupo1.hci.itba.edu.ar.data.SessionManager
import tp3.grupo1.hci.itba.edu.ar.data.model.ChangePasswordRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.EmailRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.LoginRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.RegisterRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.ResetPasswordRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.UpdateProfileRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.User
import tp3.grupo1.hci.itba.edu.ar.data.model.VerifyAccountRequest
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiException
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiProvider
import tp3.grupo1.hci.itba.edu.ar.data.network.apiCall

class AuthRepository(
    private val api: ApiProvider,
    private val session: SessionManager,
) {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    suspend fun login(email: String, password: String, rememberSession: Boolean) {
        val response = apiCall { api.users.login(LoginRequest(email, password)) }
        session.startSession(response.token, rememberSession)
        _currentUser.value = response.user
    }

    // El registro ya dispara el envio del codigo de verificacion, por eso no se hace un request extra.
    suspend fun register(name: String, email: String, password: String) {
        apiCall { api.users.register(RegisterRequest(name, email, password)) }
    }

    suspend fun verifyAccount(code: String) {
        apiCall { api.users.verifyAccount(VerifyAccountRequest(code)) }
    }

    // Devuelve el codigo OTP (la API de testing lo reenvia), o null.
    suspend fun resendVerification(email: String): String? =
        apiCall { api.users.sendVerification(EmailRequest(email)).code }

    suspend fun forgotPassword(email: String) {
        apiCall { api.users.forgotPassword(EmailRequest(email)) }
    }

    suspend fun resetPassword(code: String, newPassword: String) {
        apiCall { api.users.resetPassword(ResetPasswordRequest(code, newPassword)) }
    }

    suspend fun changePassword(oldPassword: String, newPassword: String) {
        apiCall { api.users.changePassword(ChangePasswordRequest(oldPassword, newPassword)) }
    }

    suspend fun loadProfile(): User {
        val user = apiCall { api.users.getProfile() }
        _currentUser.value = user
        return user
    }

    suspend fun updateName(name: String): User {
        val user = apiCall { api.users.updateProfile(UpdateProfileRequest(name = name)) }
        _currentUser.value = user
        return user
    }

    suspend fun logout() {
        try {
            apiCall { api.users.logout() }
        } catch (_: ApiException) {
            // Se limpia la sesion local aunque falle la llamada al servidor.
        } finally {
            session.clearSession()
            _currentUser.value = null
        }
    }
}
