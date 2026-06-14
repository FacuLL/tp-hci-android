package tp3.grupo1.hci.itba.edu.ar

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import tp3.grupo1.hci.itba.edu.ar.data.AppPreferences
import tp3.grupo1.hci.itba.edu.ar.data.SessionManager
import tp3.grupo1.hci.itba.edu.ar.data.events.AppEvent
import tp3.grupo1.hci.itba.edu.ar.data.events.EventsClient
import tp3.grupo1.hci.itba.edu.ar.data.events.NotificationHelper
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiProvider
import tp3.grupo1.hci.itba.edu.ar.data.repository.AuthRepository
import tp3.grupo1.hci.itba.edu.ar.data.repository.DeviceTypesRepository
import tp3.grupo1.hci.itba.edu.ar.data.repository.DevicesRepository
import tp3.grupo1.hci.itba.edu.ar.data.repository.HomesRepository
import tp3.grupo1.hci.itba.edu.ar.data.repository.RoomsRepository
import tp3.grupo1.hci.itba.edu.ar.data.repository.RoutinesRepository
import tp3.grupo1.hci.itba.edu.ar.domain.DeviceTypeIds

/** Manual dependency container, created once in [LuminaApplication]. */
class AppContainer(context: Context) {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val preferences = AppPreferences(context)
    val sessionManager = SessionManager(preferences, applicationScope)

    @Volatile
    private var cachedBaseUrl: String = runBlocking { preferences.apiBaseUrl.first() }

    @Volatile
    private var cachedApiKey: String = runBlocking { preferences.apiKey.first() }

    val apiProvider = ApiProvider(
        initialBaseUrl = cachedBaseUrl,
        apiKeyProvider = { cachedApiKey },
        tokenProvider = { sessionManager.currentToken() },
        onUnauthorized = { sessionManager.onUnauthorized() },
    )

    val authRepository = AuthRepository(apiProvider, sessionManager)
    val homesRepository = HomesRepository(apiProvider, preferences, applicationScope)
    val roomsRepository = RoomsRepository(apiProvider)
    val devicesRepository = DevicesRepository(apiProvider)
    val deviceTypesRepository = DeviceTypesRepository(apiProvider)
    val routinesRepository = RoutinesRepository(apiProvider)

    val eventsClient = EventsClient()
    val notificationHelper = NotificationHelper(context)

    init {
        // Any connection-setting change rebuilds the API stack and reconnects
        // the socket once, with a consistent URL + key pair.
        applicationScope.launch {
            combine(preferences.apiBaseUrl, preferences.apiKey) { url, key -> url to key }
                .distinctUntilChanged()
                .collect { (baseUrl, apiKey) ->
                    val changed = baseUrl != cachedBaseUrl || apiKey != cachedApiKey
                    cachedBaseUrl = baseUrl
                    cachedApiKey = apiKey
                    if (changed) {
                        apiProvider.setBaseUrl(baseUrl)
                        sessionManager.currentToken()?.let { token ->
                            eventsClient.connect(baseUrl, apiKey, token)
                        }
                    }
                }
        }
        // The socket lives while there is a session; local caches die with it.
        applicationScope.launch {
            sessionManager.token.collect { token ->
                if (token == null) {
                    eventsClient.disconnect()
                    devicesRepository.clear()
                    roomsRepository.clear()
                    routinesRepository.clear()
                } else {
                    eventsClient.connect(cachedBaseUrl, cachedApiKey, token)
                }
            }
        }
        applicationScope.launch {
            eventsClient.events.collect { handleEvent(it) }
        }
        // Keep the rooms cache aligned with the selected home.
        applicationScope.launch {
            homesRepository.currentHome.collect { home ->
                if (home == null) {
                    roomsRepository.clear()
                } else {
                    runCatching { roomsRepository.refreshForHome(home.id) }
                }
            }
        }
    }

    private suspend fun handleEvent(event: AppEvent) {
        when (event) {
            is AppEvent.DeviceCreated -> {
                devicesRepository.applyDeviceCreated(event.device)
                notificationHelper.showDeviceCreated(event.device.name)
            }
            is AppEvent.DeviceUpdated -> devicesRepository.applyDeviceUpdated(event.device)
            is AppEvent.DeviceDeleted -> {
                devicesRepository.applyDeviceDeleted(event.deviceId)
                event.deviceName?.let { notificationHelper.showDeviceDeleted(it) }
            }
            is AppEvent.DeviceStateChanged -> {
                devicesRepository.applyStateEvent(event.deviceId, event.args)
                // RF20: surface the security-relevant state changes (door, alarm,
                // end of cycle). The cache was just updated, so look up the device
                // there for its name and type; ignore unknown ids silently.
                val device = devicesRepository.devices.value.firstOrNull { it.id == event.deviceId }
                classifyStateEvent(device?.type?.id, event.args)?.let { kind ->
                    notificationHelper.showDeviceStateEvent(kind, device?.name)
                }
            }
            is AppEvent.HomeShared -> {
                runCatching { homesRepository.refresh() }
                notificationHelper.showHomeShared()
            }
            is AppEvent.HomeUnshared -> {
                runCatching { homesRepository.refresh() }
                notificationHelper.showHomeUnshared()
            }
        }
    }

    /**
     * Maps a `deviceEvent` payload to the RF20 notification it deserves, or null
     * when it is not security-relevant (e.g. brightness/volume/temperature
     * sliders), so those frequent changes do not spam notifications. Reads only
     * the discrete `newStatus`/`newLock` args, never the numeric ones.
     */
    private fun classifyStateEvent(typeId: String?, args: JsonObject): NotificationHelper.DeviceStateEvent? {
        fun arg(key: String): String? =
            runCatching { args[key]?.jsonPrimitive?.contentOrNull }.getOrNull()

        when (arg("newLock")) {
            "unlocked" -> return NotificationHelper.DeviceStateEvent.DOOR_UNLOCKED
        }
        when (arg("newStatus")) {
            "armedStay", "armedAway" -> return NotificationHelper.DeviceStateEvent.ALARM_ARMED
            "disarmed" -> return NotificationHelper.DeviceStateEvent.ALARM_DISARMED
            "triggered", "alarm", "sounding" -> return NotificationHelper.DeviceStateEvent.ALARM_TRIGGERED
            // A vacuum returning to its dock signals the cleaning cycle finished.
            "docked" -> return NotificationHelper.DeviceStateEvent.CYCLE_FINISHED
            // Only doors report "opened" as a security event; blinds/faucets share
            // the value but are not relevant, so scope it to the door type.
            "opened" -> if (typeId == DeviceTypeIds.DOOR) {
                return NotificationHelper.DeviceStateEvent.DOOR_OPENED
            }
        }
        return null
    }
}
