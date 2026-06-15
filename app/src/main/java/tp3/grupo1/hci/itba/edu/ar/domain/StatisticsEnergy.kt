package tp3.grupo1.hci.itba.edu.ar.domain

import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceLog
import java.time.Instant
import java.time.OffsetDateTime
import kotlin.math.min

// Acciones que dejan el dispositivo encendido / apagado. El resto (setBrightness, etc.) no
// cambia el estado on/off, asi que se ignoran al reconstruir la curva de consumo.
private val ON_ACTIONS = setOf("turnOn", "play", "start", "arm", "armStay", "armAway")
private val OFF_ACTIONS = setOf("turnOff", "stop", "pause", "dock", "disarm")

fun isPowerAction(actionName: String): Boolean =
    actionName in ON_ACTIONS || actionName in OFF_ACTIONS

// Evento on/off de un dispositivo ya normalizado a epoch-millis.
data class PowerEvent(val ms: Long, val on: Boolean)

// Parsea el timestamp ISO-8601 de la API a epoch-millis. Devuelve null si no matchea.
fun parseLogTimestamp(ts: String): Long? = runCatching { Instant.parse(ts).toEpochMilli() }
    .recoverCatching { OffsetDateTime.parse(ts).toInstant().toEpochMilli() }
    .getOrNull()

// Convierte los logs crudos de un dispositivo en eventos on/off ordenados ascendentemente.
fun powerEventsFor(logs: List<DeviceLog>): List<PowerEvent> = logs
    .filter { isPowerAction(it.actionName) }
    .mapNotNull { log -> parseLogTimestamp(log.timestamp)?.let { PowerEvent(it, log.actionName in ON_ACTIONS) } }
    .sortedBy { it.ms }

// Estado on/off inferido en el instante [t] a partir del evento mas cercano: el ultimo
// anterior manda; si no hay, se mira el primero posterior; si tampoco, el estado actual.
private fun inferStateAt(t: Long, events: List<PowerEvent>, currentlyActive: Boolean): Boolean {
    var latestBefore: PowerEvent? = null
    for (e in events) {
        if (e.ms >= t) break
        latestBefore = e
    }
    if (latestBefore != null) return latestBefore.on
    val firstAfter = events.firstOrNull { it.ms >= t } ?: return currentlyActive
    // Si la primera accion posterior es un apagado, antes estaba encendido (y viceversa).
    return !firstAfter.on
}

// Energia (kWh) consumida por un dispositivo en [windowStart, windowEnd), reconstruyendo
// los tramos encendido desde sus eventos. Portado del dashboard web para alinear ambas apps.
fun deviceEnergyKwh(
    powerW: Double,
    events: List<PowerEvent>,
    windowStart: Long,
    windowEnd: Long,
    now: Long,
    alwaysOn: Boolean,
    currentlyActive: Boolean,
): Double {
    val clampedEnd = min(windowEnd, now)
    if (windowStart >= clampedEnd || powerW == 0.0) return 0.0

    // Dispositivos sin estado on/off (ej. heladera): se asumen siempre encendidos.
    if (alwaysOn) return powerW * (clampedEnd - windowStart) / 3_600_000_000.0

    var isOn = inferStateAt(windowStart, events, currentlyActive)
    var energyWh = 0.0
    var cursor = windowStart
    for (e in events) {
        if (e.ms < windowStart) continue
        if (e.ms >= clampedEnd) break
        if (isOn) energyWh += powerW * (e.ms - cursor) / 3_600_000.0
        isOn = e.on
        cursor = e.ms
    }
    if (isOn) energyWh += powerW * (clampedEnd - cursor) / 3_600_000.0
    return energyWh / 1000.0
}
