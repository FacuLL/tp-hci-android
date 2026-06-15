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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import tp3.grupo1.hci.itba.edu.ar.AppContainer
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.data.model.Device
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceType
import tp3.grupo1.hci.itba.edu.ar.data.model.Room
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiException
import tp3.grupo1.hci.itba.edu.ar.data.repository.ROOM_META_DEVICE_ORDER
import tp3.grupo1.hci.itba.edu.ar.domain.PowerAtom
import tp3.grupo1.hci.itba.edu.ar.domain.Validators
import tp3.grupo1.hci.itba.edu.ar.domain.deviceControls
import tp3.grupo1.hci.itba.edu.ar.domain.devicesInRoom
import tp3.grupo1.hci.itba.edu.ar.domain.unassignedDevices
import tp3.grupo1.hci.itba.edu.ar.ui.luminaContainer

sealed interface RoomDetailDialog {
    data object Rename : RoomDetailDialog
    data object ConfirmDelete : RoomDetailDialog
    data object AddDevice : RoomDetailDialog
    data object CreateDevice : RoomDetailDialog
}

data class RoomDetailUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val room: Room? = null,
    val rooms: List<Room> = emptyList(),
    // Devices de la room, ordenados segun el orden guardado (ids desconocidos al final).
    val roomDevices: List<Device> = emptyList(),
    val unassignedDevices: List<Device> = emptyList(),
    val types: Map<String, DeviceType> = emptyMap(),
    val pendingDeviceIds: Set<String> = emptySet(),
    val dialog: RoomDetailDialog? = null,
    val editMode: Boolean = false,
    // Copia de trabajo mostrada mientras se edita. Vacia cuando no esta en editMode.
    val draftOrder: List<Device> = emptyList(),
    val savingOrder: Boolean = false,
    val creatingDevice: Boolean = false,
    @field:StringRes val createDeviceErrorRes: Int? = null,
    val nameInput: String = "",
    @field:StringRes val nameErrorRes: Int? = null,
    val submitAttempted: Boolean = false,
    val saving: Boolean = false,
    @field:StringRes val dialogErrorRes: Int? = null,
    // Se marca cuando la room se borra para que la pantalla pueda volver atras.
    val deleted: Boolean = false,
    @field:StringRes val snackbarMessageRes: Int? = null,
)

private data class RoomSnapshot(
    val rooms: List<Room>,
    val devices: List<Device>,
    val types: Map<String, DeviceType>,
)

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
                // Los flows observados mantienen la pantalla actualizada; un fallo aca solo deja una lista inicial vacia.
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
                val room = snapshot.rooms.firstOrNull { it.id == roomId }
                val ordered = sortByPreferredOrder(
                    devices = devicesInRoom(snapshot.devices, roomId),
                    preferred = preferredOrderFrom(room),
                )
                _uiState.update {
                    it.copy(
                        room = room,
                        rooms = snapshot.rooms,
                        roomDevices = ordered,
                        // En edit mode se sincroniza el draft contra el set vivo para reflejar altas/bajas sin perder el orden en progreso.
                        draftOrder = if (it.editMode) syncDraft(it.draftOrder, ordered) else emptyList(),
                        unassignedDevices = unassignedDevices(snapshot.devices),
                        types = snapshot.types,
                    )
                }
            }
        }
    }

    private fun preferredOrderFrom(room: Room?): List<String> {
        val raw = room?.metadata?.get(ROOM_META_DEVICE_ORDER) as? JsonArray ?: return emptyList()
        return raw.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
    }

    private fun sortByPreferredOrder(devices: List<Device>, preferred: List<String>): List<Device> {
        if (preferred.isEmpty()) return devices
        val byId = devices.associateBy { it.id }
        val seen = mutableSetOf<String>()
        val ordered = preferred.mapNotNull { id ->
            seen += id
            byId[id]
        }
        val tail = devices.filter { it.id !in seen }
        return ordered + tail
    }

    private fun syncDraft(draft: List<Device>, current: List<Device>): List<Device> {
        val currentById = current.associateBy { it.id }
        val kept = draft.mapNotNull { currentById[it.id] }
        val keptIds = kept.map { it.id }.toSet()
        val newcomers = current.filter { it.id !in keptIds }
        return kept + newcomers
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true) }
            try {
                container.homesRepository.currentHome.value?.id?.let { homeId ->
                    roomsRepository.refreshForHome(homeId)
                }
                devicesRepository.refresh()
                deviceTypesRepository.ensureLoaded()
            } catch (e: ApiException) {
                _uiState.update { it.copy(snackbarMessageRes = e.userMessageRes) }
            } finally {
                _uiState.update { it.copy(refreshing = false) }
            }
        }
    }

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

    fun openCreateDeviceDialog() {
        _uiState.update { it.copy(dialog = RoomDetailDialog.CreateDevice, createDeviceErrorRes = null) }
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

    fun deleteRoom() {
        _uiState.update { it.copy(dialog = null) }
        viewModelScope.launch {
            try {
                roomsRepository.delete(roomId)
                // Los devices de la room borrada se reasignan del lado del servidor.
                devicesRepository.refresh()
                _uiState.update { it.copy(deleted = true) }
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

    fun assignDevice(deviceId: String) {
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

    fun enterReorderMode() {
        val current = _uiState.value
        if (current.editMode || current.roomDevices.isEmpty()) return
        _uiState.update { it.copy(editMode = true, draftOrder = current.roomDevices.toList()) }
    }

    fun cancelReorder() {
        if (_uiState.value.savingOrder) return
        _uiState.update { it.copy(editMode = false, draftOrder = emptyList()) }
    }

    fun moveDevice(from: Int, to: Int) {
        val draft = _uiState.value.draftOrder
        if (from == to || from !in draft.indices || to !in draft.indices) return
        val updated = draft.toMutableList().apply { add(to, removeAt(from)) }
        _uiState.update { it.copy(draftOrder = updated) }
    }

    fun saveOrder() {
        val current = _uiState.value
        val room = current.room
        if (!current.editMode || current.savingOrder || room == null) return
        val ids = current.draftOrder.map { it.id }
        viewModelScope.launch {
            _uiState.update { it.copy(savingOrder = true) }
            try {
                roomsRepository.setDeviceOrder(room, ids)
                _uiState.update {
                    it.copy(
                        savingOrder = false,
                        editMode = false,
                        draftOrder = emptyList(),
                    )
                }
            } catch (e: ApiException) {
                _uiState.update {
                    it.copy(savingOrder = false, snackbarMessageRes = e.userMessageRes)
                }
            }
        }
    }

    companion object {
        fun Factory(roomId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer { RoomDetailViewModel(luminaContainer(), roomId) }
        }
    }
}
