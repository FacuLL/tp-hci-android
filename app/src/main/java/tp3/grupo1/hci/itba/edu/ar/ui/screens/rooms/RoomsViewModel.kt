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
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.data.model.Device
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceType
import tp3.grupo1.hci.itba.edu.ar.data.model.Home
import tp3.grupo1.hci.itba.edu.ar.data.model.Room
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiException
import tp3.grupo1.hci.itba.edu.ar.domain.PowerAtom
import tp3.grupo1.hci.itba.edu.ar.domain.Validators
import tp3.grupo1.hci.itba.edu.ar.domain.deviceControls
import tp3.grupo1.hci.itba.edu.ar.ui.luminaContainer

sealed interface RoomsDialog {
    data object Create : RoomsDialog
    data class Rename(val room: Room) : RoomsDialog
    data class ConfirmDeleteRoom(val room: Room) : RoomsDialog
    data class AddDevice(val room: Room) : RoomsDialog
    data class CreateDevice(val room: Room) : RoomsDialog
}

data class RoomsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    @field:StringRes val loadErrorRes: Int? = null,
    val rooms: List<Room> = emptyList(),
    val devices: List<Device> = emptyList(),
    val types: Map<String, DeviceType> = emptyMap(),
    val currentHome: Home? = null,
    val selectedRoomId: String? = null,
    // One-shot: una room recien creada que la pantalla debe abrir.
    val roomToOpen: String? = null,
    val dialog: RoomsDialog? = null,
    val nameInput: String = "",
    @field:StringRes val nameErrorRes: Int? = null,
    val submitAttempted: Boolean = false,
    val saving: Boolean = false,
    @field:StringRes val dialogErrorRes: Int? = null,
    // Devices con una accion en curso, para deshabilitar sus controles.
    val pendingDeviceIds: Set<String> = emptySet(),
    val creatingDevice: Boolean = false,
    @field:StringRes val createDeviceErrorRes: Int? = null,
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
                        // Descarta la seleccion si la room desaparecio (p.ej. borrada en otro lado).
                        selectedRoomId = state.selectedRoomId
                            ?.takeIf { id -> snapshot.rooms.any { it.id == id } },
                    )
                }
            }
        }
    }

    fun retry() = loadInitialData()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true, loadErrorRes = null) }
            try {
                homesRepository.refresh()
                homesRepository.currentHome.value?.id?.let { roomsRepository.refreshForHome(it) }
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

    fun selectRoom(roomId: String?) {
        _uiState.update { it.copy(selectedRoomId = roomId) }
    }

    fun onRoomOpened() {
        _uiState.update { it.copy(roomToOpen = null) }
    }

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

    fun openCreateDeviceDialog(room: Room) {
        _uiState.update { it.copy(dialog = RoomsDialog.CreateDevice(room), createDeviceErrorRes = null) }
    }

    fun createDevice(name: String, typeId: String, roomId: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(creatingDevice = true, createDeviceErrorRes = null) }
            try {
                devicesRepository.create(name.trim(), typeId, roomId)
                _uiState.update {
                    it.copy(
                        creatingDevice = false,
                        dialog = null,
                        snackbarMessageRes = R.string.devices_create_success,
                    )
                }
            } catch (e: ApiException) {
                _uiState.update { it.copy(creatingDevice = false, createDeviceErrorRes = e.userMessageRes) }
            }
        }
    }

    fun dismissDialog() {
        if (_uiState.value.saving) return
        _uiState.update { it.copy(dialog = null) }
    }

    fun onNameChange(value: String) {
        _uiState.update {
            it.copy(
                nameInput = value,
                // Revalida en vivo solo despues del primer intento de submit.
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
                // Abre la room nueva enseguida para poder agregarle devices.
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

    fun deleteRoom(room: Room) {
        _uiState.update { it.copy(dialog = null) }
        viewModelScope.launch {
            try {
                roomsRepository.delete(room.id)
                // Los devices de la room borrada se reasignan en el server, hay que recargar la lista cacheada.
                devicesRepository.refresh()
            } catch (e: ApiException) {
                _uiState.update { it.copy(snackbarMessageRes = e.userMessageRes) }
            }
        }
    }

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
