package tp3.grupo1.hci.itba.edu.ar.ui.screens.registros

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tp3.grupo1.hci.itba.edu.ar.AppContainer
import tp3.grupo1.hci.itba.edu.ar.data.model.Device
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceLog
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiException
import tp3.grupo1.hci.itba.edu.ar.domain.parseLogTimestamp
import tp3.grupo1.hci.itba.edu.ar.ui.luminaContainer

// Fila ya resuelta para la UI: el nombre/tipo del dispositivo se cruza con el cache de devices.
data class RegistroRow(
    val id: String,
    val deviceId: String,
    val deviceName: String?,
    val typeId: String?,
    val actionName: String,
    // Epoch-millis del evento (null si el timestamp no se pudo parsear); la UI lo muestra relativo.
    val timestampMs: Long?,
    val rawTimestamp: String,
    val result: Boolean?,
)

data class RegistrosUiState(
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    @field:StringRes val errorRes: Int? = null,
    val rows: List<RegistroRow> = emptyList(),
    val endReached: Boolean = false,
)

class RegistrosViewModel(container: AppContainer) : ViewModel() {

    private val devicesRepository = container.devicesRepository

    private val rawLogs = MutableStateFlow<List<DeviceLog>>(emptyList())
    private val loadState = MutableStateFlow(LoadState())

    val uiState: StateFlow<RegistrosUiState> = combine(
        rawLogs,
        devicesRepository.devices,
        loadState,
    ) { logs, devices, load ->
        val byId = devices.associateBy(Device::id)
        RegistrosUiState(
            loading = load.loading,
            loadingMore = load.loadingMore,
            errorRes = load.errorRes,
            endReached = load.endReached,
            rows = logs.map { log ->
                val device = byId[log.deviceId]
                RegistroRow(
                    id = log.id,
                    deviceId = log.deviceId,
                    deviceName = device?.name,
                    typeId = device?.type?.id,
                    actionName = log.actionName,
                    timestampMs = parseLogTimestamp(log.timestamp),
                    rawTimestamp = log.timestamp,
                    result = log.resultBool,
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RegistrosUiState())

    init { load(reset = true) }

    fun refresh() = load(reset = true)

    fun loadMore() {
        if (loadState.value.loading || loadState.value.loadingMore || loadState.value.endReached) return
        load(reset = false)
    }

    private fun load(reset: Boolean) {
        viewModelScope.launch {
            val offset = if (reset) 0 else rawLogs.value.size
            loadState.value = loadState.value.copy(
                loading = reset,
                loadingMore = !reset,
                errorRes = null,
            )
            try {
                // El cache de devices puede estar vacio al entrar directo a Registros.
                if (devicesRepository.devices.value.isEmpty()) devicesRepository.refresh()
                val page = devicesRepository.logs(PAGE_SIZE, offset)
                rawLogs.value = if (reset) page else rawLogs.value + page
                loadState.value = loadState.value.copy(
                    loading = false,
                    loadingMore = false,
                    endReached = page.size < PAGE_SIZE,
                )
            } catch (e: ApiException) {
                loadState.value = loadState.value.copy(
                    loading = false,
                    loadingMore = false,
                    errorRes = e.userMessageRes,
                )
            }
        }
    }

    private data class LoadState(
        val loading: Boolean = true,
        val loadingMore: Boolean = false,
        @field:StringRes val errorRes: Int? = null,
        val endReached: Boolean = false,
    )

    companion object {
        private const val PAGE_SIZE = 30

        val Factory = viewModelFactory {
            initializer { RegistrosViewModel(luminaContainer()) }
        }
    }
}
