package tp3.grupo1.hci.itba.edu.ar.ui.screens.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tp3.grupo1.hci.itba.edu.ar.AppContainer
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiException
import tp3.grupo1.hci.itba.edu.ar.domain.Validators
import tp3.grupo1.hci.itba.edu.ar.ui.luminaContainer

data class VerifyAccountUiState(
    val code: String = "",
    @field:StringRes val codeError: Int? = null,
    @field:StringRes val apiErrorRes: Int? = null,
    val submitting: Boolean = false,
    val verified: Boolean = false,
    val resending: Boolean = false,
    val resendCooldownSeconds: Int = 0,
    val resendSuccess: Boolean = false,
)

class VerifyAccountViewModel(
    private val container: AppContainer,
    private val email: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerifyAccountUiState())
    val uiState: StateFlow<VerifyAccountUiState> = _uiState.asStateFlow()

    private var submitAttempted = false
    private var cooldownJob: Job? = null

    fun onCodeChange(value: String) {
        _uiState.update {
            it.copy(code = value, codeError = if (submitAttempted) Validators.code(value) else null)
        }
    }

    fun submit() {
        submitAttempted = true
        val codeError = Validators.code(_uiState.value.code)
        _uiState.update { it.copy(codeError = codeError, apiErrorRes = null) }
        if (codeError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true) }
            try {
                container.authRepository.verifyAccount(_uiState.value.code.trim())
                _uiState.update { it.copy(submitting = false, verified = true) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(submitting = false, apiErrorRes = e.userMessageRes) }
            }
        }
    }

    fun resend() {
        val state = _uiState.value
        if (state.resending || state.resendCooldownSeconds > 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(resending = true, resendSuccess = false, apiErrorRes = null) }
            try {
                container.authRepository.resendVerification(email)
                _uiState.update { it.copy(resending = false, resendSuccess = true) }
                startCooldown()
            } catch (e: ApiException) {
                _uiState.update { it.copy(resending = false, apiErrorRes = e.userMessageRes) }
            }
        }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            var remaining = RESEND_COOLDOWN_SECONDS
            while (remaining > 0) {
                _uiState.update { it.copy(resendCooldownSeconds = remaining) }
                delay(1_000)
                remaining--
            }
            _uiState.update { it.copy(resendCooldownSeconds = 0) }
        }
    }

    companion object {
        private const val RESEND_COOLDOWN_SECONDS = 30

        fun factory(email: String) = viewModelFactory {
            initializer { VerifyAccountViewModel(luminaContainer(), email) }
        }
    }
}
