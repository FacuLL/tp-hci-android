package tp3.grupo1.hci.itba.edu.ar.ui.screens.devices

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tp3.grupo1.hci.itba.edu.ar.AppContainer
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.data.model.Device
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceType
import tp3.grupo1.hci.itba.edu.ar.data.model.Room
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiException
import tp3.grupo1.hci.itba.edu.ar.domain.PowerAtom
import tp3.grupo1.hci.itba.edu.ar.domain.deviceControls
import tp3.grupo1.hci.itba.edu.ar.domain.devicesForHome
import tp3.grupo1.hci.itba.edu.ar.ui.luminaContainer

data class DevicesUiState(
    val loading: Boolean = true,
    @StringRes val loadErrorRes: Int? = null,
    val query: String = "",
    val selectedTypeId: String? = null,
    val creating: Boolean = false,
    @StringRes val createErrorRes: Int? = null,
    val assigning: Boolean = false,
    @StringRes val assignErrorRes: Int? = null,
    @StringRes val snackbarRes: Int? = null,
)

/** Devices visible with the current filters, plus the unfiltered total. */
data class DeviceListState(
    val devices: List<Device> = emptyList(),
    val totalCount: Int = 0,
)

class DevicesViewModel(container: AppContainer) : ViewModel() {

    private val devicesRepository = container.devicesRepository
    private val deviceTypesRepository = container.deviceTypesRepository

    private val _uiState = MutableStateFlow(DevicesUiState())
    val uiState: StateFlow<DevicesUiState> = _uiState.asStateFlow()

    val deviceTypes: StateFlow<Map<String, DeviceType>> = deviceTypesRepository.types

    /** Rooms of the selected home, defensively filtered like the web app. */
    val rooms: StateFlow<List<Room>> =
        combine(container.roomsRepository.rooms, container.homesRepository.currentHome) { rooms, home ->
            if (home == null) rooms else rooms.filter { it.home == null || it.home.id == home.id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val listState: StateFlow<DeviceListState> =
        combine(devicesRepository.devices, rooms, _uiState) { devices, homeRooms, state ->
            val scoped = devicesForHome(devices, homeRooms)
            val query = state.query.trim()
            val filtered = scoped.filter { device ->
                (state.selectedTypeId == null || device.type.id == state.selectedTypeId) &&
                    (query.isEmpty() || device.name.contains(query, ignoreCase = true))
            }
            DeviceListState(devices = filtered, totalCount = scoped.size)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeviceListState())

    init {
        viewModelScope.launch {
            try {
                deviceTypesRepository.ensureLoaded()
                devicesRepository.refresh()
                _uiState.update { it.copy(loading = false) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(loading = false, loadErrorRes = e.userMessageRes) }
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onTypeSelected(typeId: String?) {
        _uiState.update { it.copy(selectedTypeId = typeId) }
    }

    /** Quick power toggle from the list, using the same atom as the dashboard. */
    fun toggleDevice(device: Device) {
        val type = deviceTypes.value[device.type.id] ?: return
        val power = deviceControls(type, device).filterIsInstance<PowerAtom>().firstOrNull() ?: return
        viewModelScope.launch {
            try {
                devicesRepository.execute(device.id, if (power.active) power.offAction else power.onAction)
            } catch (e: ApiException) {
                _uiState.update { it.copy(snackbarRes = e.userMessageRes) }
            }
        }
    }

    fun createDevice(name: String, typeId: String, roomId: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(creating = true, createErrorRes = null) }
            try {
                devicesRepository.create(name.trim(), typeId, roomId)
                _uiState.update { it.copy(creating = false, snackbarRes = R.string.devices_create_success) }
                onSuccess()
            } catch (e: ApiException) {
                _uiState.update { it.copy(creating = false, createErrorRes = e.userMessageRes) }
            }
        }
    }

    fun clearCreateError() {
        _uiState.update { it.copy(createErrorRes = null) }
    }

    fun assignDevice(device: Device, roomId: String?, onSuccess: () -> Unit) {
        if (roomId == device.room?.id) {
            onSuccess()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(assigning = true, assignErrorRes = null) }
            try {
                devicesRepository.moveToRoom(device, roomId)
                _uiState.update { it.copy(assigning = false, snackbarRes = R.string.devices_assign_success) }
                onSuccess()
            } catch (e: ApiException) {
                _uiState.update { it.copy(assigning = false, assignErrorRes = e.userMessageRes) }
            }
        }
    }

    fun clearAssignError() {
        _uiState.update { it.copy(assignErrorRes = null) }
    }

    fun removeFromRoom(device: Device) {
        viewModelScope.launch {
            try {
                devicesRepository.removeFromRoom(device.id)
                _uiState.update { it.copy(snackbarRes = R.string.devices_remove_room_success) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(snackbarRes = e.userMessageRes) }
            }
        }
    }

    fun deleteDevice(device: Device) {
        viewModelScope.launch {
            try {
                devicesRepository.delete(device.id)
                _uiState.update { it.copy(snackbarRes = R.string.devices_delete_success) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(snackbarRes = e.userMessageRes) }
            }
        }
    }

    fun snackbarShown() {
        _uiState.update { it.copy(snackbarRes = null) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { DevicesViewModel(luminaContainer()) }
        }
    }
}
