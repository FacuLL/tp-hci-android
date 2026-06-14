package tp3.grupo1.hci.itba.edu.ar.ui.screens.rooms

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tp3.grupo1.hci.itba.edu.ar.AppContainer
import tp3.grupo1.hci.itba.edu.ar.data.model.Device
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceType
import tp3.grupo1.hci.itba.edu.ar.data.model.Room
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiException
import tp3.grupo1.hci.itba.edu.ar.domain.PowerAtom
import tp3.grupo1.hci.itba.edu.ar.domain.Validators
import tp3.grupo1.hci.itba.edu.ar.domain.deviceControls
import tp3.grupo1.hci.itba.edu.ar.domain.devicesInRoom
import tp3.grupo1.hci.itba.edu.ar.domain.unassignedDevices
import tp3.grupo1.hci.itba.edu.ar.ui.luminaContainer

/** Dialogs of the room detail screen. */
sealed interface RoomDetailDialog {
    data object Rename : RoomDetailDialog
    data object ConfirmDelete : RoomDetailDialog
    data object AddDevice : RoomDetailDialog
    data class ConfirmRemoveDevice(val device: Device) : RoomDetailDialog
}

data class RoomDetailUiState(
    val loading: Boolean = true,
    val room: Room? = null,
    val roomDevices: List<Device> = emptyList(),
    val unassignedDevices: List<Device> = emptyList(),
    val types: Map<String, DeviceType> = emptyMap(),
    val pendingDeviceIds: Set<String> = emptySet(),
    val dialog: RoomDetailDialog? = null,
    // Rename form
    val nameInput: String = "",
    @field:StringRes val nameErrorRes: Int? = null,
    val submitAttempted: Boolean = false,
    val saving: Boolean = false,
    @field:StringRes val dialogErrorRes: Int? = null,
    /** Set once the room is deleted so the screen can navigate back. */
    val deleted: Boolean = false,
    @field:StringRes val snackbarMessageRes: Int? = null,
)

private data class RoomSnapshot(
    val rooms: List<Room>,
    val devices: List<Device>,
    val types: Map<String, DeviceType>,
)

/** Scoped to a single room; mirrors the actions available in the rooms tab. */
class RoomDetailViewModel(
    private val container: AppContainer,
    private val roomId: String,
) : ViewModel() {

    private val roomsRepository = container.roomsRepository
    private val devicesRepository = container.devicesRepository
    private val deviceTypesRepository = container.deviceTypesRepository

    private val _uiState = MutableStateFlow(RoomDetailUiState())
    val uiState: StateFlow<RoomDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                deviceTypesRepository.ensureLoaded()
                if (devicesRepository.devices.value.isEmpty()) devicesRepository.refresh()
            } catch (_: ApiException) {
                // The observed flows keep the screen updated; a load failure
                // here only means an empty initial list, surfaced as such.
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
        viewModelScope.launch {
            combine(
                roomsRepository.rooms,
                devicesRepository.devices,
                deviceTypesRepository.types,
                ::RoomSnapshot,
            ).collect { snapshot ->
                _uiState.update {
                    it.copy(
                        room = snapshot.rooms.firstOrNull { room -> room.id == roomId },
                        roomDevices = devicesInRoom(snapshot.devices, roomId),
                        unassignedDevices = unassignedDevices(snapshot.devices),
                        types = snapshot.types,
                    )
                }
            }
        }
    }

    // ── Dialogs ──

    fun openRenameDialog() {
        _uiState.update {
            it.copy(
                dialog = RoomDetailDialog.Rename,
                nameInput = it.room?.name.orEmpty(),
                nameErrorRes = null,
                submitAttempted = false,
                saving = false,
                dialogErrorRes = null,
            )
        }
    }

    fun openDeleteDialog() {
        _uiState.update { it.copy(dialog = RoomDetailDialog.ConfirmDelete) }
    }

    fun openAddDeviceDialog() {
        _uiState.update { it.copy(dialog = RoomDetailDialog.AddDevice) }
    }

    fun openRemoveDeviceDialog(device: Device) {
        _uiState.update { it.copy(dialog = RoomDetailDialog.ConfirmRemoveDevice(device)) }
    }

    fun dismissDialog() {
        if (_uiState.value.saving) return
        _uiState.update { it.copy(dialog = null) }
    }

    // ── Rename ──

    fun onNameChange(value: String) {
        _uiState.update {
            it.copy(
                nameInput = value,
                nameErrorRes = if (it.submitAttempted) Validators.name(value) else null,
            )
        }
    }

    fun submitRename() {
        val state = _uiState.value
        val room = state.room ?: return
        val errorRes = Validators.name(state.nameInput)
        _uiState.update { it.copy(submitAttempted = true, nameErrorRes = errorRes) }
        if (errorRes != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, dialogErrorRes = null) }
            try {
                roomsRepository.rename(room, state.nameInput.trim())
                _uiState.update { it.copy(saving = false, dialog = null) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(saving = false, dialogErrorRes = e.userMessageRes) }
            }
        }
    }

    // ── Delete ──

    fun deleteRoom() {
        _uiState.update { it.copy(dialog = null) }
        viewModelScope.launch {
            try {
                roomsRepository.delete(roomId)
                // Devices of the deleted room are reassigned server side.
                devicesRepository.refresh()
                _uiState.update { it.copy(deleted = true) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(snackbarMessageRes = e.userMessageRes) }
            }
        }
    }

    // ── Device actions ──

    fun toggleDevice(device: Device) {
        val type = _uiState.value.types[device.type.id] ?: return
        val power = deviceControls(type, device).filterIsInstance<PowerAtom>().firstOrNull() ?: return
        runDeviceAction(device.id) {
            devicesRepository.execute(device.id, if (power.active) power.offAction else power.onAction)
        }
    }

    fun assignDevice(deviceId: String) {
        runDeviceAction(deviceId) { devicesRepository.assignToRoom(deviceId, roomId) }
    }

    fun removeDeviceFromRoom(deviceId: String) {
        _uiState.update { it.copy(dialog = null) }
        runDeviceAction(deviceId) { devicesRepository.removeFromRoom(deviceId) }
    }

    private fun runDeviceAction(deviceId: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(pendingDeviceIds = it.pendingDeviceIds + deviceId) }
            try {
                action()
            } catch (e: ApiException) {
                _uiState.update { it.copy(snackbarMessageRes = e.userMessageRes) }
            } finally {
                _uiState.update { it.copy(pendingDeviceIds = it.pendingDeviceIds - deviceId) }
            }
        }
    }

    fun onSnackbarShown() {
        _uiState.update { it.copy(snackbarMessageRes = null) }
    }

    companion object {
        fun Factory(roomId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer { RoomDetailViewModel(luminaContainer(), roomId) }
        }
    }
}
