package tp3.grupo1.hci.itba.edu.ar.ui.screens.dashboard

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tp3.grupo1.hci.itba.edu.ar.AppContainer
import tp3.grupo1.hci.itba.edu.ar.data.model.Device
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceType
import tp3.grupo1.hci.itba.edu.ar.data.model.Home
import tp3.grupo1.hci.itba.edu.ar.data.model.Room
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiException
import tp3.grupo1.hci.itba.edu.ar.domain.PowerAtom
import tp3.grupo1.hci.itba.edu.ar.domain.devicesForHome
import tp3.grupo1.hci.itba.edu.ar.ui.luminaContainer

data class DashboardUiState(
    val loading: Boolean = true,
    val isRefreshing: Boolean = false,
    @field:StringRes val errorRes: Int? = null,
    val loaded: Boolean = false,
    val homes: List<Home> = emptyList(),
    val currentHome: Home? = null,
    val rooms: List<Room> = emptyList(),
    // Devices ya filtrados al home actual (sus rooms mas los sin asignar).
    val devices: List<Device> = emptyList(),
    val deviceTypes: Map<String, DeviceType> = emptyMap(),
    val userName: String? = null,
)

class DashboardViewModel(container: AppContainer) : ViewModel() {

    private val homesRepository = container.homesRepository
    private val roomsRepository = container.roomsRepository
    private val devicesRepository = container.devicesRepository
    private val deviceTypesRepository = container.deviceTypesRepository
    private val authRepository = container.authRepository
    private val notificationStore = container.notificationStore

    val unreadNotifications: StateFlow<Int> = notificationStore.unreadCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val refreshing = MutableStateFlow(true)
    private val manualRefreshing = MutableStateFlow(false)
    private val errorRes = MutableStateFlow<Int?>(null)

    private val _toggleErrorRes = MutableStateFlow<Int?>(null)

    val toggleErrorRes: StateFlow<Int?> = _toggleErrorRes.asStateFlow()

    val uiState: StateFlow<DashboardUiState> = combine(
        combine(
            homesRepository.homes,
            homesRepository.currentHome,
            homesRepository.loaded,
            roomsRepository.rooms,
            devicesRepository.devices,
        ) { homes, currentHome, loaded, rooms, devices ->
            DashboardSources(homes, currentHome, loaded, rooms, devices)
        },
        combine(deviceTypesRepository.types, authRepository.currentUser) { types, user ->
            TypesAndUser(types, user?.name)
        },
        combine(refreshing, manualRefreshing) { loading, manual -> loading to manual },
        errorRes,
    ) { sources, typesAndUser, loadingState, error ->
        DashboardUiState(
            loading = loadingState.first,
            isRefreshing = loadingState.second,
            errorRes = error,
            loaded = sources.loaded,
            homes = sources.homes,
            currentHome = sources.currentHome,
            rooms = sources.rooms,
            devices = devicesForHome(sources.devices, sources.rooms),
            deviceTypes = typesAndUser.types,
            userName = typesAndUser.userName,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    init {
        refresh()
    }

    // manual = true es pull-to-refresh: mantiene el contenido visible y muestra el indicador en vez del loader.
    fun refresh(manual: Boolean = false) {
        viewModelScope.launch {
            if (manual) manualRefreshing.value = true else refreshing.value = true
            errorRes.value = null
            try {
                coroutineScope {
                    launch { homesRepository.refresh() }
                    launch { devicesRepository.refresh() }
                    launch { deviceTypesRepository.ensureLoaded() }
                }
            } catch (e: ApiException) {
                errorRes.value = e.userMessageRes
            }
            if (manual) manualRefreshing.value = false else refreshing.value = false
        }
    }

    fun selectHome(homeId: String) {
        viewModelScope.launch { homesRepository.selectHome(homeId) }
    }

    fun togglePower(deviceId: String, atom: PowerAtom) {
        viewModelScope.launch {
            try {
                devicesRepository.execute(deviceId, if (atom.active) atom.offAction else atom.onAction)
            } catch (e: ApiException) {
                _toggleErrorRes.value = e.userMessageRes
            }
        }
    }

    fun clearToggleError() {
        _toggleErrorRes.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { DashboardViewModel(luminaContainer()) }
        }
    }
}

private data class DashboardSources(
    val homes: List<Home>,
    val currentHome: Home?,
    val loaded: Boolean,
    val rooms: List<Room>,
    val devices: List<Device>,
)

private data class TypesAndUser(
    val types: Map<String, DeviceType>,
    val userName: String?,
)
