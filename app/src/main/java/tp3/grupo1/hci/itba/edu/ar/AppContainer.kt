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
import tp3.grupo1.hci.itba.edu.ar.data.notifications.NotificationStore
import tp3.grupo1.hci.itba.edu.ar.data.notifications.NotificationType
import tp3.grupo1.hci.itba.edu.ar.data.notifications.SelfActionTracker
import tp3.grupo1.hci.itba.edu.ar.data.notifications.StoredNotification
import tp3.grupo1.hci.itba.edu.ar.data.repository.AuthRepository
import tp3.grupo1.hci.itba.edu.ar.data.repository.DeviceTypesRepository
import tp3.grupo1.hci.itba.edu.ar.data.repository.DevicesRepository
import tp3.grupo1.hci.itba.edu.ar.data.repository.HomesRepository
import tp3.grupo1.hci.itba.edu.ar.data.repository.RoomsRepository
import tp3.grupo1.hci.itba.edu.ar.data.repository.RoutinesRepository
import tp3.grupo1.hci.itba.edu.ar.domain.DeviceTypeIds

// Contenedor manual de dependencias, creado una vez en LuminaApplication.
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

    val selfActionTracker = SelfActionTracker()

    val authRepository = AuthRepository(apiProvider, sessionManager)
    val homesRepository = HomesRepository(apiProvider, preferences, applicationScope)
    val roomsRepository = RoomsRepository(apiProvider)
    val devicesRepository = DevicesRepository(apiProvider, selfActionTracker)
    val deviceTypesRepository = DeviceTypesRepository(apiProvider)
    val routinesRepository = RoutinesRepository(apiProvider)

    val eventsClient = EventsClient()
    val notificationHelper = NotificationHelper(context)
    val notificationStore = NotificationStore(context, applicationScope)

    init {
        // Cualquier cambio de conexion reconstruye el stack de API y reconecta el socket con un par URL+key consistente.
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
        // El socket vive mientras hay sesion; los caches locales se limpian con ella.
        applicationScope.launch {
            sessionManager.token.collect { token ->
                if (token == null) {
                    eventsClient.disconnect()
                    devicesRepository.clear()
                    roomsRepository.clear()
                    routinesRepository.clear()
                    // Limpia el historial de notificaciones para que no pase a la proxima cuenta en este dispositivo.
                    notificationStore.clear()
                } else {
                    eventsClient.connect(cachedBaseUrl, cachedApiKey, token)
                    // Carga el perfil apenas hay sesion para que el avatar de la app-bar muestre iniciales en toda pantalla.
                    runCatching { authRepository.loadProfile() }
                }
            }
        }
        applicationScope.launch {
            eventsClient.events.collect { handleEvent(it) }
        }
        // Mantiene el cache de habitaciones alineado con el hogar seleccionado.
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
                raise(
                    NotificationType.DEVICE_CREATED,
                    event.device.name,
                    deviceId = event.device.id,
                    selfKey = SelfActionTracker.deviceCreated(event.device.id),
                )
            }
            is AppEvent.DeviceUpdated -> devicesRepository.applyDeviceUpdated(event.device)
            is AppEvent.DeviceDeleted -> {
                devicesRepository.applyDeviceDeleted(event.deviceId)
                // Sin deviceId: el dispositivo ya no existe, no hay panel que abrir.
                raise(
                    NotificationType.DEVICE_DELETED,
                    event.deviceName,
                    deviceId = null,
                    selfKey = SelfActionTracker.deviceDeleted(event.deviceId),
                )
            }
            is AppEvent.DeviceStateChanged -> {
                devicesRepository.applyStateEvent(event.deviceId, event.args)
                // RF20: notifica los cambios de estado relevantes para seguridad (puerta, alarma, fin de ciclo).
                val device = devicesRepository.devices.value.firstOrNull { it.id == event.deviceId }
                classifyStateEvent(device?.type?.id, event.args)?.let { type ->
                    raise(type, device?.name, deviceId = event.deviceId, selfKey = SelfActionTracker.deviceState(event.deviceId))
                }
            }
            is AppEvent.HomeShared -> {
                runCatching { homesRepository.refresh() }
                raise(NotificationType.HOME_SHARED, arg = null, deviceId = null, selfKey = null)
            }
            is AppEvent.HomeUnshared -> {
                runCatching { homesRepository.refresh() }
                raise(NotificationType.HOME_UNSHARED, arg = null, deviceId = null, selfKey = null)
            }
        }
    }

    // Publica una notificacion y la guarda en el historial, salvo que la haya disparado esta app o su categoria este deshabilitada.
    private suspend fun raise(type: NotificationType, arg: String?, deviceId: String?, selfKey: String?) {
        if (selfKey != null && selfActionTracker.consume(selfKey)) return
        val enabled = preferences.enabledNotificationCategories.first()
        if (type.category !in enabled) return
        notificationStore.add(
            StoredNotification(
                id = java.util.UUID.randomUUID().toString(),
                type = type,
                arg = arg,
                deviceId = deviceId,
                timestamp = System.currentTimeMillis(),
            )
        )
        notificationHelper.show(type, arg)
    }

    // Mapea un payload deviceEvent a la notificacion RF20 correspondiente, o null si no es relevante para seguridad.
    // Lee solo los args discretos newStatus/newLock, nunca los numericos, para no spamear con cambios frecuentes.
    private fun classifyStateEvent(typeId: String?, args: JsonObject): NotificationType? {
        fun arg(key: String): String? =
            runCatching { args[key]?.jsonPrimitive?.contentOrNull }.getOrNull()

        when (arg("newLock")) {
            "unlocked" -> return NotificationType.DOOR_UNLOCKED
        }
        when (arg("newStatus")) {
            "armedStay", "armedAway" -> return NotificationType.ALARM_ARMED
            "disarmed" -> return NotificationType.ALARM_DISARMED
            "triggered", "alarm", "sounding" -> return NotificationType.ALARM_TRIGGERED
            // Una aspiradora que vuelve a su dock indica que termino el ciclo de limpieza.
            "docked" -> return NotificationType.CYCLE_FINISHED
            // Solo las puertas reportan "opened" como evento de seguridad; persianas/canillas comparten el valor pero no aplican.
            "opened" -> if (typeId == DeviceTypeIds.DOOR) {
                return NotificationType.DOOR_OPENED
            }
        }
        return null
    }
}
