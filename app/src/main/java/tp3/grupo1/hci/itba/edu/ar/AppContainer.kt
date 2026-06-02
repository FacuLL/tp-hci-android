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
            is AppEvent.DeviceStateChanged ->
                devicesRepository.applyStateEvent(event.deviceId, event.args)
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
}
