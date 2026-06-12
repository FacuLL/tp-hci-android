package tp3.grupo1.hci.itba.edu.ar.ui.screens.routines

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
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.data.model.Device
import tp3.grupo1.hci.itba.edu.ar.data.model.Routine
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiException
import tp3.grupo1.hci.itba.edu.ar.ui.luminaContainer

data class RoutinesUiState(
    val loading: Boolean = true,
    val routines: List<Routine> = emptyList(),
    val devicesById: Map<String, Device> = emptyMap(),
    val executingId: String? = null,
    @StringRes val loadErrorRes: Int? = null,
    @StringRes val snackbarMessageRes: Int? = null,
)

class RoutinesViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutinesUiState())
    val uiState: StateFlow<RoutinesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            container.routinesRepository.routines.collect { routines ->
                _uiState.update { it.copy(routines = routines) }
            }
        }
        viewModelScope.launch {
            container.devicesRepository.devices.collect { devices ->
                _uiState.update { state -> state.copy(devicesById = devices.associateBy { it.id }) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, loadErrorRes = null) }
            try {
                container.routinesRepository.refresh()
                // Devices and types are needed to render each action row.
                if (container.devicesRepository.devices.value.isEmpty()) {
                    container.devicesRepository.refresh()
                }
                container.deviceTypesRepository.ensureLoaded()
                _uiState.update { it.copy(loading = false) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(loading = false, loadErrorRes = e.userMessageRes) }
            }
        }
    }

    /**
     * Runs the routine server-side. Device state is not re-queried afterwards:
     * WebSocket events reconcile the local caches on their own.
     */
    fun execute(routineId: String) {
        if (_uiState.value.executingId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(executingId = routineId) }
            try {
                container.routinesRepository.execute(routineId)
                _uiState.update {
                    it.copy(executingId = null, snackbarMessageRes = R.string.routines_executed)
                }
            } catch (e: ApiException) {
                _uiState.update {
                    it.copy(executingId = null, snackbarMessageRes = e.userMessageRes)
                }
            }
        }
    }

    fun consumeSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessageRes = null) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { RoutinesViewModel(luminaContainer()) }
        }
    }
}
