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

const val FORGOT_PASSWORD_STEPS = 2

data class ForgotPasswordUiState(
    val step: Int = 1,
    val email: String = "",
    val code: String = "",
    val newPassword: String = "",
    val confirmation: String = "",
    @field:StringRes val emailError: Int? = null,
    @field:StringRes val codeError: Int? = null,
    @field:StringRes val newPasswordError: Int? = null,
    @field:StringRes val confirmationError: Int? = null,
    @field:StringRes val apiErrorRes: Int? = null,
    val submitting: Boolean = false,
    val done: Boolean = false,
)

class ForgotPasswordViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    private var emailSubmitAttempted = false
    private var resetSubmitAttempted = false

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(email = value, emailError = if (emailSubmitAttempted) Validators.email(value) else null)
        }
    }

    fun onCodeChange(value: String) {
        _uiState.update {
            it.copy(code = value, codeError = if (resetSubmitAttempted) Validators.code(value) else null)
        }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                newPassword = value,
                newPasswordError = if (resetSubmitAttempted) Validators.password(value) else null,
                confirmationError = if (resetSubmitAttempted) {
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
                confirmationError = if (resetSubmitAttempted) {
                    Validators.passwordConfirmation(it.newPassword, value)
                } else {
                    null
                },
            )
        }
    }

    fun submitEmail() {
        emailSubmitAttempted = true
        val emailError = Validators.email(_uiState.value.email)
        _uiState.update { it.copy(emailError = emailError, apiErrorRes = null) }
        if (emailError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true) }
            try {
                container.authRepository.forgotPassword(_uiState.value.email.trim())
                _uiState.update { it.copy(submitting = false, step = 2) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(submitting = false, apiErrorRes = e.userMessageRes) }
            }
        }
    }

    fun submitReset() {
        resetSubmitAttempted = true
        val state = _uiState.value
        val codeError = Validators.code(state.code)
        val newPasswordError = Validators.password(state.newPassword)
        val confirmationError = Validators.passwordConfirmation(state.newPassword, state.confirmation)
        _uiState.update {
            it.copy(
                codeError = codeError,
                newPasswordError = newPasswordError,
                confirmationError = confirmationError,
                apiErrorRes = null,
            )
        }
        if (codeError != null || newPasswordError != null || confirmationError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true) }
            try {
                container.authRepository.resetPassword(state.code.trim(), state.newPassword)
                _uiState.update { it.copy(submitting = false, done = true) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(submitting = false, apiErrorRes = e.userMessageRes) }
            }
        }
    }

    /** Goes back to the email step keeping the address, e.g. to fix a typo. */
    fun backToEmailStep() {
        _uiState.update {
            it.copy(
                step = 1,
                apiErrorRes = null,
                codeError = null,
                newPasswordError = null,
                confirmationError = null,
            )
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { ForgotPasswordViewModel(luminaContainer()) } }
    }
}
