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
import tp3.grupo1.hci.itba.edu.ar.data.model.DEFAULT_TARIFF
import tp3.grupo1.hci.itba.edu.ar.data.model.Device
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceLog
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceType
import tp3.grupo1.hci.itba.edu.ar.data.model.Home
import tp3.grupo1.hci.itba.edu.ar.data.model.Room
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiException
import tp3.grupo1.hci.itba.edu.ar.domain.PowerEvent
import tp3.grupo1.hci.itba.edu.ar.domain.deviceEnergyKwh
import tp3.grupo1.hci.itba.edu.ar.domain.isDeviceActive
import tp3.grupo1.hci.itba.edu.ar.domain.powerEventsFor
import tp3.grupo1.hci.itba.edu.ar.ui.luminaContainer
import java.time.Instant
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

enum class StatsPeriod(val days: Int) {
    HOY(1), SEMANA(7), MES(30);
}

// Punto del grafico comparativo: consumo (kWh) del periodo actual vs el anterior.
data class ComparePoint(val label: String, val current: Double, val previous: Double)

// Consumo instantaneo (W) de una habitacion.
data class RoomUsage(val roomName: String, val watts: Int)

// Variacion por habitacion: barras normalizadas (0-100) actual/anterior y delta %.
data class RoomCompare(
    val roomName: String,
    val actualPct: Int,
    val anteriorPct: Int,
    val deltaPct: Int,
)

data class StatisticsUiState(
    val loading: Boolean = true,
    val isRefreshing: Boolean = false,
    @field:StringRes val errorRes: Int? = null,
    val currentHome: Home? = null,
    val period: StatsPeriod = StatsPeriod.HOY,
    val totalKwh: Double = 0.0,
    val estimatedCost: Double = 0.0,
    val tariff: Double = DEFAULT_TARIFF,
    val deltaPct: Int? = null,
    val mostActiveRoom: RoomUsage? = null,
    val activeDevices: Int = 0,
    val totalDevices: Int = 0,
    val lineSeries: List<Double> = emptyList(),
    val lineLabels: List<String> = emptyList(),
    val roomConsumption: List<RoomUsage> = emptyList(),
    val comparison: List<ComparePoint> = emptyList(),
    val roomComparison: List<RoomCompare> = emptyList(),
)

// Deriva todos los KPI y graficos de los logs de accion reales (/devices/logs) cruzados con
// el powerUsage de cada tipo. Misma logica que el dashboard web para que ambas apps coincidan.
class StatisticsViewModel(container: AppContainer) : ViewModel() {

    private val homesRepository = container.homesRepository
    private val roomsRepository = container.roomsRepository
    private val devicesRepository = container.devicesRepository
    private val deviceTypesRepository = container.deviceTypesRepository

    private val refreshing = MutableStateFlow(true)
    private val manualRefreshing = MutableStateFlow(false)
    private val errorRes = MutableStateFlow<Int?>(null)
    private val periodFlow = MutableStateFlow(StatsPeriod.HOY)
    private val logsFlow = MutableStateFlow<List<DeviceLog>>(emptyList())

    val uiState: StateFlow<StatisticsUiState> = combine(
        combine(
            homesRepository.currentHome,
            roomsRepository.rooms,
            devicesRepository.devices,
            deviceTypesRepository.types,
            logsFlow,
        ) { home, rooms, devices, types, logs ->
            Sources(home, rooms, devices, types, logs)
        },
        periodFlow,
        combine(refreshing, manualRefreshing) { loading, manual -> loading to manual },
        errorRes,
    ) { sources, period, loadingState, error ->
        compute(sources, period, loadingState, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    init { refresh() }

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
            // Los logs son degradables: si fallan, los KPI instantaneos siguen mostrandose
            // (las curvas de energia quedan en 0). No deben romper toda la pantalla.
            logsFlow.value = try {
                devicesRepository.logs(LOG_FETCH_LIMIT, 0)
            } catch (_: ApiException) {
                emptyList()
            }
            if (manual) manualRefreshing.value = false else refreshing.value = false
        }
    }

    fun setPeriod(period: StatsPeriod) { periodFlow.value = period }

    fun setTariff(tariff: Double) {
        val homeId = uiState.value.currentHome?.id ?: return
        viewModelScope.launch {
            try {
                homesRepository.updateTariff(homeId, tariff)
            } catch (e: ApiException) {
                errorRes.value = e.userMessageRes
            }
        }
    }

    private fun compute(
        sources: Sources,
        period: StatsPeriod,
        loadingState: Pair<Boolean, Boolean>,
        error: Int?,
    ): StatisticsUiState {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val tariff = sources.home?.tariff ?: DEFAULT_TARIFF

        val roomIds = sources.rooms.mapTo(mutableSetOf()) { it.id }
        val homeDevices = sources.devices.filter { it.room?.id != null && it.room.id in roomIds }
        val energyDevices = homeDevices
            .map { device ->
                EnergyDevice(
                    device = device,
                    powerW = sources.types[device.type.id]?.powerUsage ?: 0.0,
                    events = powerEventsFor(sources.logs.filter { it.deviceId == device.id }),
                )
            }
            .filter { it.powerW > 0.0 }

        // Suma de energia (kWh) de un subconjunto de dispositivos en una ventana.
        fun sumWindow(devices: List<EnergyDevice>, start: Long, end: Long): Double =
            devices.sumOf { d ->
                deviceEnergyKwh(
                    powerW = d.powerW,
                    events = d.events,
                    windowStart = start,
                    windowEnd = end,
                    now = now,
                    alwaysOn = d.device.state.status == null && d.device.state.lock == null,
                    currentlyActive = isDeviceActive(d.device),
                )
            }

        val periodStart = periodStartMs(period, now, zone)
        val (prevStart, prevEnd) = prevRange(period, now, zone)
        val totalKwh = sumWindow(energyDevices, periodStart, now)
        val prevTotalKwh = sumWindow(energyDevices, prevStart, prevEnd)
        val deltaPct = if (prevTotalKwh < 0.01 || totalKwh < 0.01) {
            null
        } else {
            ((totalKwh - prevTotalKwh) / prevTotalKwh * 100).roundToInt()
        }

        val lineBuckets = buckets(period, BucketMode.LINE, now, zone)
        val lineSeries = lineBuckets.map { b ->
            (sumWindow(energyDevices, b.start, b.end) * 10).roundToInt() / 10.0
        }
        val currBars = buckets(period, BucketMode.BAR, now, zone)
        val prevBars = prevBuckets(period, BucketMode.BAR, now, zone)
        val comparison = currBars.mapIndexed { i, b ->
            val prev = prevBars.getOrNull(i)
            ComparePoint(
                label = b.label,
                current = (sumWindow(energyDevices, b.start, b.end) * 10).roundToInt() / 10.0,
                previous = prev?.let { (sumWindow(energyDevices, it.start, it.end) * 10).roundToInt() / 10.0 } ?: 0.0,
            )
        }

        // Consumo instantaneo (W) por habitacion con dispositivos activos.
        val instantByRoom = sources.rooms.map { room ->
            val watts = energyDevices
                .filter { it.device.room?.id == room.id && isDeviceActive(it.device) }
                .sumOf { it.powerW }
            RoomUsage(room.name, watts.roundToInt())
        }
        val roomConsumption = instantByRoom.filter { it.watts > 0 }.sortedByDescending { it.watts }

        // Comparativo de energia por habitacion: periodo actual vs anterior, barras normalizadas.
        val roomRows = sources.rooms.mapNotNull { room ->
            val roomDevices = energyDevices.filter { it.device.room?.id == room.id }
            val curr = sumWindow(roomDevices, periodStart, now)
            val prev = sumWindow(roomDevices, prevStart, prevEnd)
            if (curr <= 0.0 && prev <= 0.0) null else Triple(room.name, curr, prev)
        }
        val maxVal = roomRows.flatMap { listOf(it.second, it.third) }.maxOrNull()?.coerceAtLeast(0.001) ?: 0.001
        val roomComparison = roomRows.map { (name, curr, prev) ->
            RoomCompare(
                roomName = name,
                actualPct = (curr / maxVal * 100).roundToInt(),
                anteriorPct = (prev / maxVal * 100).roundToInt(),
                deltaPct = if (prev > 0.0) ((curr - prev) / prev * 100).roundToInt() else 0,
            )
        }

        return StatisticsUiState(
            loading = loadingState.first,
            isRefreshing = loadingState.second,
            errorRes = error,
            currentHome = sources.home,
            period = period,
            totalKwh = totalKwh,
            estimatedCost = totalKwh * tariff,
            tariff = tariff,
            deltaPct = deltaPct,
            mostActiveRoom = roomConsumption.firstOrNull(),
            activeDevices = homeDevices.count(::isDeviceActive),
            totalDevices = homeDevices.size,
            lineSeries = lineSeries,
            lineLabels = lineBuckets.map { it.label },
            roomConsumption = roomConsumption,
            comparison = comparison,
            roomComparison = roomComparison,
        )
    }

    private enum class BucketMode { LINE, BAR }

    private data class Bucket(val label: String, val start: Long, val end: Long)

    private data class EnergyDevice(
        val device: Device,
        val powerW: Double,
        val events: List<PowerEvent>,
    )

    private data class Sources(
        val home: Home?,
        val rooms: List<Room>,
        val devices: List<Device>,
        val types: Map<String, DeviceType>,
        val logs: List<DeviceLog>,
    )

    companion object {
        private const val LOG_FETCH_LIMIT = 500
        private const val DAY_MS = 86_400_000L
        private const val HOUR_MS = 3_600_000L

        val Factory = viewModelFactory {
            initializer { StatisticsViewModel(luminaContainer()) }
        }

        private fun startOfTodayMs(now: Long, zone: ZoneId): Long =
            Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

        private fun periodStartMs(period: StatsPeriod, now: Long, zone: ZoneId): Long {
            val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            return when (period) {
                StatsPeriod.HOY -> today
                StatsPeriod.SEMANA -> today.minusDays((today.dayOfWeek.value - 1).toLong())
                StatsPeriod.MES -> today.withDayOfMonth(1)
            }.atStartOfDay(zone).toInstant().toEpochMilli()
        }

        private fun prevRange(period: StatsPeriod, now: Long, zone: ZoneId): Pair<Long, Long> {
            val currStart = periodStartMs(period, now, zone)
            val elapsed = now - currStart
            return when (period) {
                StatsPeriod.HOY -> {
                    val start = currStart - DAY_MS
                    start to start + min(elapsed, DAY_MS)
                }
                StatsPeriod.SEMANA -> {
                    val start = currStart - 7 * DAY_MS
                    start to start + min(elapsed, 7 * DAY_MS)
                }
                StatsPeriod.MES -> {
                    val prevMonth = Instant.ofEpochMilli(currStart).atZone(zone).toLocalDate()
                        .minusMonths(1).withDayOfMonth(1)
                    val start = prevMonth.atStartOfDay(zone).toInstant().toEpochMilli()
                    val prevMonthLen = currStart - start
                    start to start + min(elapsed, prevMonthLen)
                }
            }
        }

        private fun buckets(period: StatsPeriod, mode: BucketMode, now: Long, zone: ZoneId): List<Bucket> {
            val origin = periodStartMs(period, now, zone)
            return when (period) {
                StatsPeriod.HOY -> if (mode == BucketMode.BAR) {
                    // Comparativo: 8 intervalos fijos de 3h.
                    (0 until 8).map { i ->
                        Bucket("%02d:00".format(i * 3), origin + i * 3 * HOUR_MS, origin + (i + 1) * 3 * HOUR_MS)
                    }
                } else {
                    // Linea: bloques de 2h ya completados.
                    val completed = ((now - origin) / (2 * HOUR_MS)).toInt().coerceIn(0, 12)
                    (0 until completed).map { i ->
                        Bucket("%02d:00".format(i * 2), origin + i * 2 * HOUR_MS, origin + (i + 1) * 2 * HOUR_MS)
                    }
                }
                StatsPeriod.SEMANA -> listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sá", "Do").mapIndexed { i, label ->
                    Bucket(label, origin + i * DAY_MS, origin + (i + 1) * DAY_MS)
                }
                StatsPeriod.MES -> {
                    val originDate = Instant.ofEpochMilli(origin).atZone(zone).toLocalDate()
                    val nextMonth = originDate.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
                    val daysInMonth = ((nextMonth - origin) / DAY_MS).toInt()
                    val numWeeks = ceil(daysInMonth / 7.0).toInt()
                    (0 until numWeeks).map { i ->
                        val start = origin + i * 7 * DAY_MS
                        Bucket("Sem ${i + 1}", start, min(start + 7 * DAY_MS, nextMonth))
                    }
                }
            }
        }

        private fun prevBuckets(period: StatsPeriod, mode: BucketMode, now: Long, zone: ZoneId): List<Bucket> {
            if (period == StatsPeriod.HOY && mode == BucketMode.BAR) {
                val prevOrigin = startOfTodayMs(now, zone) - DAY_MS
                return (0 until 8).map { i ->
                    Bucket("%02d:00".format(i * 3), prevOrigin + i * 3 * HOUR_MS, prevOrigin + (i + 1) * 3 * HOUR_MS)
                }
            }
            val (prevOrigin, prevCutoff) = prevRange(period, now, zone)
            val currOrigin = periodStartMs(period, now, zone)
            return buckets(period, mode, now, zone).map { b ->
                val offset = b.start - currOrigin
                val width = b.end - b.start
                Bucket(b.label, prevOrigin + offset, min(prevOrigin + offset + width, prevCutoff))
            }
        }
    }
}
