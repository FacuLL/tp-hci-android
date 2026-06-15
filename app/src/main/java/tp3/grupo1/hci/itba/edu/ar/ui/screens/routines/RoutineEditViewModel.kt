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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import tp3.grupo1.hci.itba.edu.ar.AppContainer
import tp3.grupo1.hci.itba.edu.ar.data.model.Device
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceType
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceTypeAction
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceTypeActionParam
import tp3.grupo1.hci.itba.edu.ar.data.model.EntityRef
import tp3.grupo1.hci.itba.edu.ar.data.model.RoutineAction
import tp3.grupo1.hci.itba.edu.ar.data.model.RoutineUpsertRequest
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiException
import tp3.grupo1.hci.itba.edu.ar.ui.luminaContainer

/** One row of the "Actions" section: a device, one of its actions and its params. */
data class ActionForm(
    val deviceId: String = "",
    val actionName: String = "",
    val params: List<String> = emptyList(),
)

data class RoutineEditUiState(
    val loading: Boolean = true,
    val isEditing: Boolean = false,
    val name: String = "",
    // false = ejecutar manualmente, true = programar horario
    val scheduled: Boolean = false,
    val time: String = "08:00",
    val days: List<String> = emptyList(),
    val actions: List<ActionForm> = emptyList(),
    val devices: List<Device> = emptyList(),
    val typesById: Map<String, DeviceType> = emptyMap(),
    val submitted: Boolean = false,
    val saving: Boolean = false,
    @StringRes val apiErrorRes: Int? = null,
    val saved: Boolean = false,
) {
    val canSave: Boolean
        get() = name.isNotBlank() &&
            actions.isNotEmpty() &&
            actions.all { it.deviceId.isNotBlank() && it.actionName.isNotBlank() } &&
            (!scheduled || days.isNotEmpty())
}

class RoutineEditViewModel(
    private val container: AppContainer,
    private val routineId: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineEditUiState(isEditing = routineId != null))
    val uiState: StateFlow<RoutineEditUiState> = _uiState.asStateFlow()

    // Preserved across an edit so toggling the schedule never silently
    // reactivates a routine the user had disabled from the web.
    private var activa: Boolean = true

    init {
        viewModelScope.launch {
            try {
                if (container.devicesRepository.devices.value.isEmpty()) {
                    container.devicesRepository.refresh()
                }
                val types = container.deviceTypesRepository.ensureLoaded()
                val devices = container.devicesRepository.devices.value
                val editing = routineId?.let { id ->
                    container.routinesRepository.routines.value.firstOrNull { it.id == id }
                }

                _uiState.update { state ->
                    if (editing == null) {
                        state.copy(loading = false, devices = devices, typesById = types)
                    } else {
                        val schedule = editing.schedule
                        activa = schedule.enabled
                        state.copy(
                            loading = false,
                            name = editing.name,
                            scheduled = schedule.isScheduled,
                            time = schedule.time ?: "08:00",
                            days = schedule.days,
                            actions = editing.actions.map { action ->
                                ActionForm(
                                    deviceId = action.device.id,
                                    actionName = action.actionName,
                                    params = action.params.map { (it as? JsonPrimitive)?.content ?: "" },
                                )
                            },
                            devices = devices,
                            typesById = types,
                        )
                    }
                }
            } catch (e: ApiException) {
                _uiState.update { it.copy(loading = false, apiErrorRes = e.userMessageRes) }
            }
        }
    }

    // ── Catalog helpers ──────────────────────────────────────────────────────

    fun actionsForDevice(deviceId: String): List<DeviceTypeAction> {
        val device = _uiState.value.devices.firstOrNull { it.id == deviceId } ?: return emptyList()
        return _uiState.value.typesById[device.type.id]?.actions ?: emptyList()
    }

    fun paramsForAction(deviceId: String, actionName: String): List<DeviceTypeActionParam> =
        actionsForDevice(deviceId).firstOrNull { it.name == actionName }?.params ?: emptyList()

    private fun defaultParams(deviceId: String, actionName: String): List<String> =
        paramsForAction(deviceId, actionName).map { def ->
            when {
                def.supportedValuesList().isNotEmpty() -> def.supportedValuesList().first()
                def.type == "integer" -> (def.minNumber ?: 0.0).toInt().toString()
                def.type == "number" -> (def.minNumber ?: 0.0).toString()
                def.type == "boolean" -> "false"
                else -> ""
            }
        }

    // ── Form mutations ───────────────────────────────────────────────────────

    fun setName(value: String) = _uiState.update { it.copy(name = value) }

    fun setScheduled(value: Boolean) = _uiState.update { it.copy(scheduled = value) }

    fun setTime(value: String) = _uiState.update { it.copy(time = value) }

    fun toggleDay(day: String) = _uiState.update { state ->
        val days = if (day in state.days) state.days - day else state.days + day
        state.copy(days = days)
    }

    fun addAction() = _uiState.update { it.copy(actions = it.actions + ActionForm()) }

    fun removeAction(index: Int) = _uiState.update {
        it.copy(actions = it.actions.filterIndexed { i, _ -> i != index })
    }

    fun setActionDevice(index: Int, deviceId: String) = updateAction(index) {
        it.copy(deviceId = deviceId, actionName = "", params = emptyList())
    }

    fun setActionName(index: Int, actionName: String) = updateAction(index) { form ->
        form.copy(actionName = actionName, params = defaultParams(form.deviceId, actionName))
    }

    fun setParam(index: Int, paramIndex: Int, value: String) = updateAction(index) { form ->
        val params = form.params.toMutableList()
        while (params.size <= paramIndex) params.add("")
        params[paramIndex] = value
        form.copy(params = params)
    }

    private inline fun updateAction(index: Int, transform: (ActionForm) -> ActionForm) {
        _uiState.update { state ->
            state.copy(actions = state.actions.mapIndexed { i, form ->
                if (i == index) transform(form) else form
            })
        }
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    fun save() {
        val state = _uiState.value
        // Evita el doble alta: una vez que se esta guardando o ya se guardo, se
        // ignoran taps repetidos (la animacion de salida deja el boton clickeable
        // un instante y permitia crear la rutina dos veces).
        if (state.saving || state.saved) return
        _uiState.update { it.copy(submitted = true) }
        if (!state.canSave) return
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, apiErrorRes = null) }
            try {
                val actions = state.actions.map { form ->
                    val defs = paramsForAction(form.deviceId, form.actionName)
                    val params = buildJsonArray {
                        defs.forEachIndexed { i, def ->
                            val raw = form.params.getOrElse(i) { "" }.trim()
                            when (def.type) {
                                "integer" -> add(raw.toIntOrNull() ?: 0)
                                "number" -> add(raw.toDoubleOrNull() ?: 0.0)
                                "boolean" -> add(raw.equals("true", ignoreCase = true))
                                else -> add(raw)
                            }
                        }
                    }
                    RoutineAction(EntityRef(form.deviceId), form.actionName, params)
                }
                val metadata = buildJsonObject {
                    put("activa", activa)
                    put("tipo", if (state.scheduled) "programada" else "manual")
                    if (state.scheduled) put("hora", state.time) else put("hora", JsonNull)
                    putJsonArray("dias") { if (state.scheduled) state.days.forEach { add(it) } }
                }
                val request = RoutineUpsertRequest(state.name.trim(), actions, metadata)
                if (routineId != null) {
                    container.routinesRepository.update(routineId, request)
                } else {
                    container.routinesRepository.create(request)
                }
                _uiState.update { it.copy(saving = false, saved = true) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(saving = false, apiErrorRes = e.userMessageRes) }
            }
        }
    }

    companion object {
        fun factory(routineId: String?) = viewModelFactory {
            initializer { RoutineEditViewModel(luminaContainer(), routineId) }
        }
    }
}
