package tp3.grupo1.hci.itba.edu.ar.ui.screens.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tp3.grupo1.hci.itba.edu.ar.AppContainer
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiException
import tp3.grupo1.hci.itba.edu.ar.domain.Validators
import tp3.grupo1.hci.itba.edu.ar.ui.luminaContainer

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmation: String = "",
    @field:StringRes val nameError: Int? = null,
    @field:StringRes val emailError: Int? = null,
    @field:StringRes val passwordError: Int? = null,
    @field:StringRes val confirmationError: Int? = null,
    @field:StringRes val apiErrorRes: Int? = null,
    val submitting: Boolean = false,
    val registeredEmail: String? = null,
)

class RegisterViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private var submitAttempted = false

    fun onNameChange(value: String) {
        _uiState.update {
            it.copy(name = value, nameError = if (submitAttempted) Validators.name(value) else null)
        }
    }

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(email = value, emailError = if (submitAttempted) Validators.email(value) else null)
        }
    }

    fun onPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                passwordError = if (submitAttempted) Validators.password(value) else null,
                confirmationError = if (submitAttempted) {
                    Validators.passwordConfirmation(value, it.confirmation)
                } else {
                    it.confirmationError
                },
            )
        }
    }

    fun onConfirmationChange(value: String) {
        _uiState.update {
            it.copy(
                confirmation = value,
                confirmationError = if (submitAttempted) {
                    Validators.passwordConfirmation(it.password, value)
                } else {
                    null
                },
            )
        }
    }

    fun submit() {
        submitAttempted = true
        val state = _uiState.value
        val nameError = Validators.name(state.name)
        val emailError = Validators.email(state.email)
        val passwordError = Validators.password(state.password)
        val confirmationError = Validators.passwordConfirmation(state.password, state.confirmation)
        _uiState.update {
            it.copy(
                nameError = nameError,
                emailError = emailError,
                passwordError = passwordError,
                confirmationError = confirmationError,
                apiErrorRes = null,
            )
        }
        if (nameError != null || emailError != null || passwordError != null || confirmationError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true) }
            try {
                // The API already emails the verification code as part of the
                // register call, so no extra send-verification request is made.
                container.authRepository.register(state.name.trim(), state.email.trim(), state.password)
                _uiState.update { it.copy(submitting = false, registeredEmail = state.email.trim()) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(submitting = false, apiErrorRes = e.userMessageRes) }
            }
        }
    }

    fun onRegisteredHandled() {
        _uiState.update { it.copy(registeredEmail = null) }
    }

    companion object {
        val Factory = viewModelFactory { initializer { RegisterViewModel(luminaContainer()) } }
    }
}
