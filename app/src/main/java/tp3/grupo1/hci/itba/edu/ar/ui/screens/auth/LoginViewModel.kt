package tp3.grupo1.hci.itba.edu.ar.ui.screens.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tp3.grupo1.hci.itba.edu.ar.AppContainer
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.data.AppPreferences
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiException
import tp3.grupo1.hci.itba.edu.ar.domain.Validators
import tp3.grupo1.hci.itba.edu.ar.ui.luminaContainer
import java.net.URI
import java.net.URISyntaxException

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    @field:StringRes val emailError: Int? = null,
    @field:StringRes val passwordError: Int? = null,
    @field:StringRes val apiErrorRes: Int? = null,
    val submitting: Boolean = false,
    val loggedIn: Boolean = false,
    val needsVerificationEmail: String? = null,
    // Server connection dialog: the API address must be changeable before
    // logging in, otherwise an unreachable server locks the user out.
    val showApiConfig: Boolean = false,
    val apiBaseUrl: String = "",
    val apiKey: String = "",
    @field:StringRes val apiUrlError: Int? = null,
)

class LoginViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var submitAttempted = false

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(email = value, emailError = if (submitAttempted) Validators.email(value) else null)
        }
    }

    fun onPasswordChange(value: String) {
        _uiState.update {
            it.copy(password = value, passwordError = if (submitAttempted) Validators.required(value) else null)
        }
    }

    fun submit() {
        submitAttempted = true
        val state = _uiState.value
        val emailError = Validators.email(state.email)
        val passwordError = Validators.required(state.password)
        _uiState.update {
            it.copy(emailError = emailError, passwordError = passwordError, apiErrorRes = null)
        }
        if (emailError != null || passwordError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true) }
            try {
                container.authRepository.login(state.email.trim(), state.password, true)
                _uiState.update { it.copy(submitting = false, loggedIn = true) }
            } catch (e: ApiException) {
                if (e.userMessageRes == R.string.error_not_verified) {
                    _uiState.update {
                        it.copy(submitting = false, needsVerificationEmail = state.email.trim())
                    }
                } else {
                    _uiState.update { it.copy(submitting = false, apiErrorRes = e.userMessageRes) }
                }
            }
        }
    }

    fun onLoggedInHandled() {
        _uiState.update { it.copy(loggedIn = false) }
    }

    fun onNeedsVerificationHandled() {
        _uiState.update { it.copy(needsVerificationEmail = null) }
    }

    // ── Server connection (API address and key) ──

    fun openApiConfig() {
        viewModelScope.launch {
            val url = container.preferences.apiBaseUrl.first()
            val key = container.preferences.apiKey.first()
            _uiState.update {
                it.copy(showApiConfig = true, apiBaseUrl = url, apiKey = key, apiUrlError = null)
            }
        }
    }

    fun closeApiConfig() {
        _uiState.update { it.copy(showApiConfig = false) }
    }

    fun onApiBaseUrlChange(value: String) {
        _uiState.update { it.copy(apiBaseUrl = value, apiUrlError = null) }
    }

    fun onApiKeyChange(value: String) {
        _uiState.update { it.copy(apiKey = value) }
    }

    fun resetApiConfigDefaults() {
        _uiState.update {
            it.copy(
                apiBaseUrl = AppPreferences.DEFAULT_BASE_URL,
                apiKey = AppPreferences.DEFAULT_API_KEY,
                apiUrlError = null,
            )
        }
    }

    fun saveApiConfig() {
        val url = _uiState.value.apiBaseUrl.trim()
        if (!isValidApiUrl(url)) {
            _uiState.update { it.copy(apiUrlError = R.string.auth_api_url_invalid) }
            return
        }
        viewModelScope.launch {
            container.preferences.setApiBaseUrl(url)
            container.preferences.setApiKey(_uiState.value.apiKey.trim())
            _uiState.update { it.copy(showApiConfig = false) }
        }
    }

    private fun isValidApiUrl(url: String): Boolean = try {
        val uri = URI(url)
        (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
    } catch (_: URISyntaxException) {
        false
    }

    companion object {
        val Factory = viewModelFactory { initializer { LoginViewModel(luminaContainer()) } }
    }
}
