package tp3.grupo1.hci.itba.edu.ar.ui.screens.devices

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import tp3.grupo1.hci.itba.edu.ar.AppContainer
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.data.model.Device
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceType
import tp3.grupo1.hci.itba.edu.ar.data.model.Room
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiException
import tp3.grupo1.hci.itba.edu.ar.domain.ControlAtom
import tp3.grupo1.hci.itba.edu.ar.domain.deviceControls
import tp3.grupo1.hci.itba.edu.ar.ui.luminaContainer

sealed interface DeviceDetailDialog {
    data object Rename : DeviceDetailDialog
    data object AssignRoom : DeviceDetailDialog
    data object Delete : DeviceDetailDialog
    data class SecurityCode(val action: String, @field:StringRes val labelRes: Int) : DeviceDetailDialog
    data object ChangeCode : DeviceDetailDialog
    data object Playlist : DeviceDetailDialog
}

data class PlaylistEntry(
    val title: String,
    val subtitle: String?,
    val duration: String?,
)

sealed interface PlaylistUiState {
    data object Loading : PlaylistUiState
    data object Error : PlaylistUiState
    data class Loaded(val songs: List<PlaylistEntry>) : PlaylistUiState
}

data class DeviceDetailUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val device: Device? = null,
    val type: DeviceType? = null,
    val atoms: List<ControlAtom> = emptyList(),
    val rooms: List<Room> = emptyList(),
    val dialog: DeviceDetailDialog? = null,
    val dialogBusy: Boolean = false,
    @field:StringRes val dialogErrorRes: Int? = null,
    val dispensing: Boolean = false,
    val playlist: PlaylistUiState = PlaylistUiState.Loading,
)

private val ALARM_ACTION_STATUS = mapOf(
    "armStay" to "armedStay",
    "armAway" to "armedAway",
    "disarm" to "disarmed",
)

// Aviso al usuario tras una accion puntual exitosa (la cerradura de la puerta no tiene feedback visible inmediato).
private val ACTION_FEEDBACK = mapOf(
    "lock" to R.string.device_detail_door_locked,
    "unlock" to R.string.device_detail_door_unlocked,
)

class DeviceDetailViewModel(
    private val container: AppContainer,
    private val deviceId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceDetailUiState())
    val uiState: StateFlow<DeviceDetailUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val messages: SharedFlow<Int> = _messages.asSharedFlow()

    // Colores custom recientes de la lampara, persistidos localmente (mas reciente primero).
    val recentColors: StateFlow<List<String>> = container.preferences.recentColors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            try {
                container.deviceTypesRepository.ensureLoaded()
                // Solo pedimos si el cache esta vacio (entrada directa); los repos se mantienen actualizados solos.
                if (container.devicesRepository.devices.value.isEmpty()) {
                    container.devicesRepository.refresh()
                }
                if (!container.homesRepository.loaded.value) {
                    container.homesRepository.refresh()
                }
            } catch (e: ApiException) {
                _messages.emit(e.userMessageRes)
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
        viewModelScope.launch {
            combine(
                container.devicesRepository.devices,
                container.deviceTypesRepository.types,
                container.roomsRepository.rooms,
            ) { devices, types, rooms ->
                Triple(devices.firstOrNull { it.id == deviceId }, types, rooms)
            }.collect { (device, types, rooms) ->
                val type = device?.let { types[it.type.id] }
                _uiState.update {
                    it.copy(
                        device = device,
                        type = type,
                        atoms = if (device != null && type != null) deviceControls(type, device) else emptyList(),
                        rooms = rooms,
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true) }
            try {
                container.devicesRepository.refresh()
                container.deviceTypesRepository.ensureLoaded()
            } catch (e: ApiException) {
                _messages.emit(e.userMessageRes)
            } finally {
                _uiState.update { it.copy(refreshing = false) }
            }
        }
    }

    fun execute(action: String, params: List<JsonElement> = emptyList()) {
        viewModelScope.launch {
            try {
                container.devicesRepository.execute(deviceId, action, params)
                ACTION_FEEDBACK[action]?.let { _messages.emit(it) }
            } catch (e: ApiException) {
                _messages.emit(e.userMessageRes)
            }
        }
    }

    // Color custom de la lampara: lo persiste en recientes y lo despacha como setColor (mismo formato "#RRGGBB" que los swatches).
    fun onPickCustomColor(action: String, hex: String) {
        viewModelScope.launch {
            container.preferences.addRecentColor(hex)
        }
        execute(action, listOf(JsonPrimitive(hex)))
    }

    fun dispense(action: String, quantity: Int, unit: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(dispensing = true) }
            try {
                container.devicesRepository.execute(
                    deviceId,
                    action,
                    listOf(JsonPrimitive(quantity), JsonPrimitive(unit)),
                )
            } catch (e: ApiException) {
                _messages.emit(e.userMessageRes)
            } finally {
                _uiState.update { it.copy(dispensing = false) }
            }
        }
    }

    fun openDialog(dialog: DeviceDetailDialog) {
        _uiState.update { it.copy(dialog = dialog, dialogErrorRes = null) }
        if (dialog == DeviceDetailDialog.Playlist) loadPlaylist()
    }

    fun closeDialog() {
        _uiState.update { it.copy(dialog = null, dialogBusy = false, dialogErrorRes = null) }
    }

    fun rename(name: String) {
        runDialogAction { container.devicesRepository.rename(deviceId, name.trim()) }
    }

    fun assignRoom(roomId: String?) {
        val device = _uiState.value.device ?: return
        if (roomId == device.room?.id) {
            closeDialog()
            return
        }
        runDialogAction { container.devicesRepository.moveToRoom(device, roomId) }
    }

    fun deleteDevice(onDeleted: () -> Unit) {
        runDialogAction(onSuccess = onDeleted) { container.devicesRepository.delete(deviceId) }
    }

    // La API responde {"result": false} (no error HTTP) cuando el codigo es incorrecto, asi que inspeccionamos el resultado.
    fun executeWithCode(action: String, code: String) {
        runCodeAction(action, listOf(JsonPrimitive(code)), ALARM_ACTION_STATUS[action])
    }

    fun changeSecurityCode(currentCode: String, newCode: String) {
        runCodeAction("changeSecurityCode", listOf(JsonPrimitive(currentCode), JsonPrimitive(newCode)))
    }

    private fun runCodeAction(action: String, params: List<JsonElement>, newStatus: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(dialogBusy = true, dialogErrorRes = null) }
            try {
                val payload = container.devicesRepository.executeForResult(deviceId, action, params)
                if (resultIsFalse(payload)) {
                    _uiState.update { it.copy(dialogBusy = false, dialogErrorRes = R.string.error_invalid_code) }
                } else {
                    if (newStatus != null) {
                        container.devicesRepository.applyStateEvent(
                            deviceId,
                            JsonObject(mapOf("newStatus" to JsonPrimitive(newStatus))),
                        )
                    }
                    closeDialog()
                }
            } catch (e: ApiException) {
                _uiState.update { it.copy(dialogBusy = false, dialogErrorRes = e.userMessageRes) }
            }
        }
    }

    private fun runDialogAction(onSuccess: () -> Unit = {}, block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(dialogBusy = true, dialogErrorRes = null) }
            try {
                block()
                closeDialog()
                onSuccess()
            } catch (e: ApiException) {
                _uiState.update { it.copy(dialogBusy = false, dialogErrorRes = e.userMessageRes) }
            }
        }
    }

    private fun loadPlaylist() {
        viewModelScope.launch {
            _uiState.update { it.copy(playlist = PlaylistUiState.Loading) }
            val playlist = try {
                val payload = container.devicesRepository.executeForResult(deviceId, "getPlaylist")
                parsePlaylist(payload)?.let { PlaylistUiState.Loaded(it) } ?: PlaylistUiState.Error
            } catch (_: ApiException) {
                PlaylistUiState.Error
            }
            _uiState.update { it.copy(playlist = playlist) }
        }
    }

    private fun resultIsFalse(payload: JsonElement): Boolean =
        ((payload as? JsonObject)?.get("result") as? JsonPrimitive)?.booleanOrNull == false

    // Parseo defensivo: acepta {"result": [...]} o un array pelado.
    private fun parsePlaylist(payload: JsonElement): List<PlaylistEntry>? {
        val array = when (payload) {
            is JsonArray -> payload
            is JsonObject -> payload["result"] as? JsonArray
            else -> null
        } ?: return null
        return array.mapNotNull { item ->
            when (item) {
                is JsonObject -> {
                    val fields = item.entries
                        .mapNotNull { (key, value) -> (value as? JsonPrimitive)?.contentOrNull?.let { key to it } }
                        .toMap()
                    // La API nombra el campo como "song"; "title" queda como fallback.
                    val title = fields["song"] ?: fields["title"] ?: fields.values.firstOrNull()
                        ?: return@mapNotNull null
                    val subtitle = listOfNotNull(fields["artist"], fields["album"])
                        .joinToString(" · ")
                        .ifBlank { null }
                    PlaylistEntry(title = title, subtitle = subtitle, duration = fields["duration"])
                }
                is JsonPrimitive -> PlaylistEntry(title = item.content, subtitle = null, duration = null)
                else -> null
            }
        }
    }

    companion object {
        fun Factory(deviceId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer { DeviceDetailViewModel(luminaContainer(), deviceId) }
        }
    }
}
