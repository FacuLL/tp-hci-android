package tp3.grupo1.hci.itba.edu.ar.ui.screens.rooms

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
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
import tp3.grupo1.hci.itba.edu.ar.data.model.Home
import tp3.grupo1.hci.itba.edu.ar.data.model.Room
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiException
import tp3.grupo1.hci.itba.edu.ar.domain.PowerAtom
import tp3.grupo1.hci.itba.edu.ar.domain.Validators
import tp3.grupo1.hci.itba.edu.ar.domain.deviceControls
import tp3.grupo1.hci.itba.edu.ar.ui.luminaContainer

/** Modal flows of the rooms tab (dialogs and confirmations). */
sealed interface RoomsDialog {
    data object Create : RoomsDialog
    data class Rename(val room: Room) : RoomsDialog
    data class ConfirmDeleteRoom(val room: Room) : RoomsDialog
    data class AddDevice(val room: Room) : RoomsDialog
}

data class RoomsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    @field:StringRes val loadErrorRes: Int? = null,
    val rooms: List<Room> = emptyList(),
    val devices: List<Device> = emptyList(),
    val types: Map<String, DeviceType> = emptyMap(),
    val currentHome: Home? = null,
    /** Room selected in the two-pane side panel (large tablets only). */
    val selectedRoomId: String? = null,
    /** One-shot: a newly created room the screen should open. */
    val roomToOpen: String? = null,
    val dialog: RoomsDialog? = null,
    // Create / rename form
    val nameInput: String = "",
    @field:StringRes val nameErrorRes: Int? = null,
    val submitAttempted: Boolean = false,
    val saving: Boolean = false,
    @field:StringRes val dialogErrorRes: Int? = null,
    /** Devices with an action in flight, to disable their row controls. */
    val pendingDeviceIds: Set<String> = emptySet(),
    @field:StringRes val snackbarMessageRes: Int? = null,
)

private data class RepositorySnapshot(
    val rooms: List<Room>,
    val devices: List<Device>,
    val types: Map<String, DeviceType>,
    val currentHome: Home?,
)

class RoomsViewModel(container: AppContainer) : ViewModel() {

    private val homesRepository = container.homesRepository
    private val roomsRepository = container.roomsRepository
    private val devicesRepository = container.devicesRepository
    private val deviceTypesRepository = container.deviceTypesRepository

    private val _uiState = MutableStateFlow(RoomsUiState())
    val uiState: StateFlow<RoomsUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        viewModelScope.launch {
            combine(
                roomsRepository.rooms,
                devicesRepository.devices,
                deviceTypesRepository.types,
                homesRepository.currentHome,
                ::RepositorySnapshot,
            ).collect { snapshot ->
                _uiState.update { state ->
                    state.copy(
                        rooms = snapshot.rooms,
                        devices = snapshot.devices,
                        types = snapshot.types,
                        currentHome = snapshot.currentHome,
                        // Drop the selection if the room disappeared (e.g. deleted elsewhere)
                        selectedRoomId = state.selectedRoomId
                            ?.takeIf { id -> snapshot.rooms.any { it.id == id } },
                    )
                }
            }
        }
    }

    fun retry() = loadInitialData()

    /** Pull-to-refresh: force a re-fetch keeping current content visible. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true, loadErrorRes = null) }
            try {
                homesRepository.refresh()
                devicesRepository.refresh()
                deviceTypesRepository.ensureLoaded()
                _uiState.update { it.copy(refreshing = false) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(refreshing = false, loadErrorRes = e.userMessageRes) }
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, loadErrorRes = null) }
            try {
                if (!homesRepository.loaded.value) homesRepository.refresh()
                if (devicesRepository.devices.value.isEmpty()) devicesRepository.refresh()
                deviceTypesRepository.ensureLoaded()
                _uiState.update { it.copy(loading = false) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(loading = false, loadErrorRes = e.userMessageRes) }
            }
        }
    }

    // ── Selection ──

    fun selectRoom(roomId: String?) {
        _uiState.update { it.copy(selectedRoomId = roomId) }
    }

    fun onRoomOpened() {
        _uiState.update { it.copy(roomToOpen = null) }
    }

    // ── Dialogs ──

    fun openCreateDialog() {
        if (_uiState.value.currentHome == null) return
        openNameDialog(RoomsDialog.Create, initialName = "")
    }

    fun openRenameDialog(room: Room) {
        openNameDialog(RoomsDialog.Rename(room), initialName = room.name)
    }

    private fun openNameDialog(dialog: RoomsDialog, initialName: String) {
        _uiState.update {
            it.copy(
                dialog = dialog,
                nameInput = initialName,
                nameErrorRes = null,
                submitAttempted = false,
                saving = false,
                dialogErrorRes = null,
            )
        }
    }

    fun openDeleteRoomDialog(room: Room) {
        _uiState.update { it.copy(dialog = RoomsDialog.ConfirmDeleteRoom(room)) }
    }

    fun openAddDeviceDialog(room: Room) {
        _uiState.update { it.copy(dialog = RoomsDialog.AddDevice(room)) }
    }

    fun dismissDialog() {
        if (_uiState.value.saving) return
        _uiState.update { it.copy(dialog = null) }
    }

    // ── Create / rename form ──

    fun onNameChange(value: String) {
        _uiState.update {
            it.copy(
                nameInput = value,
                // Live revalidation only after the first submit attempt
                nameErrorRes = if (it.submitAttempted) Validators.name(value) else null,
            )
        }
    }

    fun submitName() {
        val state = _uiState.value
        val errorRes = Validators.name(state.nameInput)
        _uiState.update { it.copy(submitAttempted = true, nameErrorRes = errorRes) }
        if (errorRes != null) return
        val name = state.nameInput.trim()
        when (val dialog = state.dialog) {
            RoomsDialog.Create -> createRoom(name)
            is RoomsDialog.Rename -> renameRoom(dialog.room, name)
            else -> Unit
        }
    }

    private fun createRoom(name: String) {
        val homeId = _uiState.value.currentHome?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, dialogErrorRes = null) }
            try {
                val room = roomsRepository.create(name, homeId)
                // Open the new room right away so devices can be added to it
                _uiState.update { it.copy(saving = false, dialog = null, roomToOpen = room.id) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(saving = false, dialogErrorRes = e.userMessageRes) }
            }
        }
    }

    private fun renameRoom(room: Room, name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, dialogErrorRes = null) }
            try {
                roomsRepository.rename(room, name)
                _uiState.update { it.copy(saving = false, dialog = null) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(saving = false, dialogErrorRes = e.userMessageRes) }
            }
        }
    }

    // ── Room and device actions ──

    fun deleteRoom(room: Room) {
        _uiState.update { it.copy(dialog = null) }
        viewModelScope.launch {
            try {
                roomsRepository.delete(room.id)
                // Devices of the deleted room are reassigned server side, so the
                // cached list must be reloaded once to stay coherent.
                devicesRepository.refresh()
            } catch (e: ApiException) {
                _uiState.update { it.copy(snackbarMessageRes = e.userMessageRes) }
            }
        }
    }

    /** Quick toggle, same behavior as the other screens: PowerAtom -> execute. */
    fun toggleDevice(device: Device) {
        val type = _uiState.value.types[device.type.id] ?: return
        val power = deviceControls(type, device).filterIsInstance<PowerAtom>().firstOrNull() ?: return
        runDeviceAction(device.id) {
            devicesRepository.execute(device.id, if (power.active) power.offAction else power.onAction)
        }
    }

    fun assignDevice(deviceId: String, roomId: String) {
        runDeviceAction(deviceId) { devicesRepository.assignToRoom(deviceId, roomId) }
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
        val Factory = viewModelFactory {
            initializer { RoomsViewModel(luminaContainer()) }
        }
    }
}
