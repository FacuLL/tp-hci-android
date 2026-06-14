package tp3.grupo1.hci.itba.edu.ar.ui.screens.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tp3.grupo1.hci.itba.edu.ar.AppContainer
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.data.AppLanguage
import tp3.grupo1.hci.itba.edu.ar.data.ThemeMode
import tp3.grupo1.hci.itba.edu.ar.data.model.User
import tp3.grupo1.hci.itba.edu.ar.data.notifications.NotificationCategory
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiException
import tp3.grupo1.hci.itba.edu.ar.domain.Validators
import tp3.grupo1.hci.itba.edu.ar.ui.luminaContainer

data class SettingsUiState(
    val loading: Boolean = true,
    @StringRes val loadError: Int? = null,
    @StringRes val snackbarMessage: Int? = null,
    val nameDialogOpen: Boolean = false,
    val nameDraft: String = "",
    @StringRes val nameError: Int? = null,
    @StringRes val nameApiError: Int? = null,
    val nameSaving: Boolean = false,
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    @StringRes val oldPasswordError: Int? = null,
    @StringRes val newPasswordError: Int? = null,
    @StringRes val confirmPasswordError: Int? = null,
    @StringRes val passwordApiError: Int? = null,
    val passwordSaving: Boolean = false,
    val loggingOut: Boolean = false,
)

class SettingsViewModel(container: AppContainer) : ViewModel() {

    private val authRepository = container.authRepository
    private val preferences = container.preferences

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = authRepository.currentUser

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val language: StateFlow<AppLanguage> = preferences.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppLanguage.SYSTEM)

    val enabledNotificationCategories: StateFlow<Set<NotificationCategory>> =
        preferences.enabledNotificationCategories
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                NotificationCategory.entries.toSet(),
            )

    // La revalidacion en vivo solo se activa tras el primer intento de envio de cada formulario.
    private var nameSubmitted = false
    private var passwordSubmitted = false

    init {
        viewModelScope.launch {
            try {
                authRepository.loadProfile()
                _uiState.update { it.copy(loading = false) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(loading = false, loadError = e.userMessageRes) }
            }
        }
    }

    fun openNameDialog() {
        nameSubmitted = false
        _uiState.update {
            it.copy(
                nameDialogOpen = true,
                nameDraft = currentUser.value?.name.orEmpty(),
                nameError = null,
                nameApiError = null,
            )
        }
    }

    fun dismissNameDialog() {
        if (_uiState.value.nameSaving) return
        _uiState.update { it.copy(nameDialogOpen = false) }
    }

    fun onNameDraftChange(value: String) {
        _uiState.update {
            it.copy(
                nameDraft = value,
                nameError = if (nameSubmitted) Validators.name(value) else it.nameError,
            )
        }
    }

    fun submitName() {
        nameSubmitted = true
        val draft = _uiState.value.nameDraft
        val error = Validators.name(draft)
        _uiState.update { it.copy(nameError = error) }
        if (error != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(nameSaving = true, nameApiError = null) }
            try {
                authRepository.updateName(draft.trim())
                _uiState.update {
                    it.copy(
                        nameSaving = false,
                        nameDialogOpen = false,
                        snackbarMessage = R.string.settings_profile_updated,
                    )
                }
            } catch (e: ApiException) {
                _uiState.update { it.copy(nameSaving = false, nameApiError = e.userMessageRes) }
            }
        }
    }

    fun onOldPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                oldPassword = value,
                oldPasswordError = if (passwordSubmitted) Validators.required(value) else it.oldPasswordError,
            )
        }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                newPassword = value,
                newPasswordError = if (passwordSubmitted) Validators.password(value) else it.newPasswordError,
                confirmPasswordError = if (passwordSubmitted) {
                    Validators.passwordConfirmation(value, it.confirmPassword)
                } else {
                    it.confirmPasswordError
                },
            )
        }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                confirmPassword = value,
                confirmPasswordError = if (passwordSubmitted) {
                    Validators.passwordConfirmation(it.newPassword, value)
                } else {
                    it.confirmPasswordError
                },
            )
        }
    }

    fun submitPasswordChange() {
        passwordSubmitted = true
        val state = _uiState.value
        val oldError = Validators.required(state.oldPassword)
        val newError = Validators.password(state.newPassword)
        val confirmError = Validators.passwordConfirmation(state.newPassword, state.confirmPassword)
        _uiState.update {
            it.copy(
                oldPasswordError = oldError,
                newPasswordError = newError,
                confirmPasswordError = confirmError,
            )
        }
        if (oldError != null || newError != null || confirmError != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(passwordSaving = true, passwordApiError = null) }
            try {
                authRepository.changePassword(state.oldPassword, state.newPassword)
                passwordSubmitted = false
                _uiState.update {
                    it.copy(
                        passwordSaving = false,
                        oldPassword = "",
                        newPassword = "",
                        confirmPassword = "",
                        snackbarMessage = R.string.settings_password_changed,
                    )
                }
            } catch (e: ApiException) {
                _uiState.update { it.copy(passwordSaving = false, passwordApiError = e.userMessageRes) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { preferences.setLanguage(language) }
    }

    fun setNotificationCategoryEnabled(category: NotificationCategory, enabled: Boolean) {
        viewModelScope.launch { preferences.setNotificationCategoryEnabled(category, enabled) }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(loggingOut = true) }
            // Nunca lanza: el repositorio limpia la sesion local aunque falle la llamada al servidor.
            authRepository.logout()
        }
    }

    fun consumeSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { SettingsViewModel(luminaContainer()) }
        }
    }
}
