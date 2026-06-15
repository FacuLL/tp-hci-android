package tp3.grupo1.hci.itba.edu.ar.ui.screens.statistics

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
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceType
import tp3.grupo1.hci.itba.edu.ar.data.model.Home
import tp3.grupo1.hci.itba.edu.ar.data.model.Room
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiException
import tp3.grupo1.hci.itba.edu.ar.domain.devicesForHome
import tp3.grupo1.hci.itba.edu.ar.domain.isDeviceActive
import tp3.grupo1.hci.itba.edu.ar.ui.luminaContainer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max

enum class StatsPeriod(val days: Int) {
    HOY(1), SEMANA(7), MES(30);
}

data class HourPoint(val hour: Int, val kWh: Double)

data class RoomUsage(val roomName: String, val watts: Int)

data class StatisticsUiState(
    val loading: Boolean = true,
    val isRefreshing: Boolean = false,
    @field:StringRes val errorRes: Int? = null,
    val currentHome: Home? = null,
    val period: StatsPeriod = StatsPeriod.HOY,
    val totalKwh: Double = 0.0,
    val estimatedCost: Double = 0.0,
    val mostActiveRoom: RoomUsage? = null,
    val activeDevices: Int = 0,
    val totalDevices: Int = 0,
    val deltaPct: Int = 0,
    val hourly: List<HourPoint> = emptyList(),
)

// La API no expone mediciones de energia, asi que este ViewModel deriva los KPI desde
// `DevicesRepository.devices` (cantidad activa + powerUsage de su DeviceType). La serie
// horaria es una curva sintetica determinista escalada por el total del periodo: datos demo.
class StatisticsViewModel(container: AppContainer) : ViewModel() {

    private val homesRepository = container.homesRepository
    private val roomsRepository = container.roomsRepository
    private val devicesRepository = container.devicesRepository
    private val deviceTypesRepository = container.deviceTypesRepository

    private val refreshing = MutableStateFlow(true)
    private val manualRefreshing = MutableStateFlow(false)
    private val errorRes = MutableStateFlow<Int?>(null)
    private val periodFlow = MutableStateFlow(StatsPeriod.HOY)

    val uiState: StateFlow<StatisticsUiState> = combine(
        combine(
            homesRepository.currentHome,
            roomsRepository.rooms,
            devicesRepository.devices,
            deviceTypesRepository.types,
        ) { home, rooms, devices, types ->
            Sources(home, rooms, devices, types)
        },
        periodFlow,
        combine(refreshing, manualRefreshing) { loading, manual -> loading to manual },
        errorRes,
    ) { sources, period, loadingState, error ->
        val scopedDevices = devicesForHome(sources.devices, sources.rooms)
        val active = scopedDevices.filter(::isDeviceActive)
        val activeWatts = active.sumOf { (sources.types[it.type.id]?.powerUsage ?: 0.0) }
        val hoursPerDay = HOURS_ON_PER_DAY
        val totalKwh = activeWatts * hoursPerDay * period.days / 1000.0
        val estimatedCost = totalKwh * COST_PER_KWH_ARS
        val mostActive = mostActiveRoom(sources.rooms, scopedDevices, sources.types)
        StatisticsUiState(
            loading = loadingState.first,
            isRefreshing = loadingState.second,
            errorRes = error,
            currentHome = sources.home,
            period = period,
            totalKwh = totalKwh,
            estimatedCost = estimatedCost,
            mostActiveRoom = mostActive,
            activeDevices = active.size,
            totalDevices = scopedDevices.size,
            deltaPct = syntheticDelta(period, active.size),
            hourly = syntheticHourly(period, totalKwh),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    init { refresh() }

    // [manual] es true en pull-to-refresh, mantiene el contenido visible.
    fun refresh(manual: Boolean = false) {
        viewModelScope.launch {
            if (manual) manualRefreshing.value = true else refreshing.value = true
            errorRes.value = null
            try {
                homesRepository.refresh()
                devicesRepository.refresh()
                deviceTypesRepository.ensureLoaded()
            } catch (e: ApiException) {
                errorRes.value = e.userMessageRes
            }
            if (manual) manualRefreshing.value = false else refreshing.value = false
        }
    }

    fun setPeriod(period: StatsPeriod) { periodFlow.value = period }

    companion object {
        // Promedio aproximado de horas encendido por dispositivo activo al dia. La API no
        // expone ciclos de uso, asi que aplicamos la misma heuristica que el dashboard web
        // (8 h) para mantener ambas superficies alineadas.
        private const val HOURS_ON_PER_DAY = 8.0

        // Tarifa demo. La API no expone las tarifas reales por agencia.
        private const val COST_PER_KWH_ARS = 200.0

        val Factory = viewModelFactory {
            initializer { StatisticsViewModel(luminaContainer()) }
        }

        private fun mostActiveRoom(
            rooms: List<Room>,
            devices: List<Device>,
            types: Map<String, DeviceType>,
        ): RoomUsage? {
            if (rooms.isEmpty()) return null
            val byRoom = rooms.associate { room ->
                val watts = devices
                    .filter { it.room?.id == room.id && isDeviceActive(it) }
                    .sumOf { types[it.type.id]?.powerUsage ?: 0.0 }
                room.name to watts.toInt()
            }
            val (name, watts) = byRoom.maxByOrNull { it.value } ?: return null
            return if (watts > 0) RoomUsage(name, watts) else null
        }

        // Serie sintetica determinista para que el grafico sea plausible sin un backend
        // de medicion. Mas alta en la manana y la tarde, mas baja de noche.
        private fun syntheticHourly(period: StatsPeriod, totalKwh: Double): List<HourPoint> {
            val daily = if (period == StatsPeriod.HOY) totalKwh else totalKwh / period.days
            return (0 until 24).map { hour ->
                // Curva de dos picos: maximos ~ 8h y ~ 20h, valle ~ 3h.
                val morning = cos((hour - 8) * 2 * PI / 24).coerceAtLeast(0.0)
                val evening = cos((hour - 20) * 2 * PI / 24).coerceAtLeast(0.0)
                val baseline = 0.15
                val shape = baseline + 0.85 * (morning + evening) / 2.0
                val kWh = max(0.0, daily * shape / 12.0)
                HourPoint(hour, kWh)
            }
        }

        private fun syntheticDelta(period: StatsPeriod, activeCount: Int): Int {
            // Delta demo estable por periodo; no es una medicion real.
            val seed = period.days * 7 + activeCount
            return ((seed % 11) - 5).let { if (it == 0) 5 else it }
        }
    }

    private data class Sources(
        val home: Home?,
        val rooms: List<Room>,
        val devices: List<Device>,
        val types: Map<String, DeviceType>,
    )
}
