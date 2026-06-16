package tp3.grupo1.hci.itba.edu.ar.ui.screens.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tp3.grupo1.hci.itba.edu.ar.AppContainer
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.data.AppLanguage
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiException
import tp3.grupo1.hci.itba.edu.ar.domain.Validators
import tp3.grupo1.hci.itba.edu.ar.ui.luminaContainer

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    @field:StringRes val emailError: Int? = null,
    @field:StringRes val passwordError: Int? = null,
    @field:StringRes val apiErrorRes: Int? = null,
    val submitting: Boolean = false,
    val loggedIn: Boolean = false,
    val needsVerificationEmail: String? = null,
    // En el login solo se puede ajustar el idioma — la configuracion de
    // API (URL + key) se removio del flujo de autenticacion (queda
    // configurada via DEFAULT_BASE_URL / DEFAULT_API_KEY en AppPreferences).
    val showLanguageDialog: Boolean = false,
)

class LoginViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // Exponemos el idioma actual para que el dialog marque la opcion seleccionada.
    val language: StateFlow<AppLanguage> = container.preferences.language
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.SYSTEM)

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

    fun openLanguageDialog() {
        _uiState.update { it.copy(showLanguageDialog = true) }
    }

    fun closeLanguageDialog() {
        _uiState.update { it.copy(showLanguageDialog = false) }
    }

    fun setLanguage(value: AppLanguage) {
        viewModelScope.launch {
            container.preferences.setLanguage(value)
            _uiState.update { it.copy(showLanguageDialog = false) }
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { LoginViewModel(luminaContainer()) } }
    }
}
