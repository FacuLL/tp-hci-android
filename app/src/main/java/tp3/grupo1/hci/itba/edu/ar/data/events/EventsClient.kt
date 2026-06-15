package tp3.grupo1.hci.itba.edu.ar.data.events

import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.Polling
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject
import tp3.grupo1.hci.itba.edu.ar.data.model.Device
import java.net.URI

sealed interface AppEvent {
    data class DeviceCreated(val device: Device) : AppEvent
    data class DeviceUpdated(val device: Device) : AppEvent
    data class DeviceDeleted(val deviceId: String, val deviceName: String?) : AppEvent
    data class DeviceStateChanged(val deviceId: String, val eventName: String, val args: JsonObject) : AppEvent
    data class HomeShared(val homeId: String) : AppEvent
    data class HomeUnshared(val homeId: String) : AppEvent
}

// Cliente Socket.IO para los eventos en tiempo real de la API. Usa el transporte polling segun lo que soporta la API.
class EventsClient {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    private var socket: Socket? = null

    @Synchronized
    fun connect(apiBaseUrl: String, apiKey: String?, token: String) {
        disconnect()
        val serverUrl = serverUrlFrom(apiBaseUrl) ?: return
        val options = IO.Options().apply {
            reconnection = true
            transports = arrayOf(Polling.NAME)
            auth = buildMap {
                put("token", token)
                if (!apiKey.isNullOrBlank()) put("apiKey", apiKey)
            }
        }
        val newSocket = IO.socket(URI.create(serverUrl), options)

        newSocket.on("deviceCreated") { args ->
            val payload = parseObject(args) ?: return@on
            decodeDevice(payload)?.let { _events.tryEmit(AppEvent.DeviceCreated(it)) }
        }
        newSocket.on("deviceUpdated") { args ->
            val payload = parseObject(args) ?: return@on
            decodeDevice(payload)?.let { _events.tryEmit(AppEvent.DeviceUpdated(it)) }
        }
        newSocket.on("deviceDeleted") { args ->
            val payload = parseObject(args) ?: return@on
            val deviceId = payload.stringField("deviceId") ?: return@on
            val name = (payload["device"] as? JsonObject)?.stringField("name")
            _events.tryEmit(AppEvent.DeviceDeleted(deviceId, name))
        }
        newSocket.on("deviceEvent") { args ->
            val payload = parseObject(args) ?: return@on
            val data = payload["data"] as? JsonObject ?: return@on
            val deviceId = data.stringField("deviceId") ?: return@on
            val eventName = data.stringField("event") ?: return@on
            val eventArgs = data["args"] as? JsonObject ?: JsonObject(emptyMap())
            _events.tryEmit(AppEvent.DeviceStateChanged(deviceId, eventName, eventArgs))
        }
        newSocket.on("homeShared") { args ->
            val payload = parseObject(args) ?: return@on
            payload.stringField("homeId")?.let { _events.tryEmit(AppEvent.HomeShared(it)) }
        }
        newSocket.on("homeUnshared") { args ->
            val payload = parseObject(args) ?: return@on
            payload.stringField("homeId")?.let { _events.tryEmit(AppEvent.HomeUnshared(it)) }
        }

        newSocket.connect()
        socket = newSocket
    }

    @Synchronized
    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
    }

    private fun parseObject(args: Array<Any?>): JsonObject? {
        val raw = args.firstOrNull() as? JSONObject ?: return null
        return runCatching { json.parseToJsonElement(raw.toString()).jsonObject }.getOrNull()
    }

    private fun decodeDevice(payload: JsonObject): Device? {
        val element = payload["device"] ?: return null
        return runCatching { json.decodeFromJsonElement<Device>(element) }.getOrNull()
    }

    private fun JsonObject.stringField(key: String): String? =
        runCatching { this[key]?.jsonPrimitive?.contentOrNull }.getOrNull()

    // El servidor de Socket.IO vive en el host de la API sin el path /api.
    private fun serverUrlFrom(apiBaseUrl: String): String? = runCatching {
        val uri = URI.create(apiBaseUrl.trim())
        val port = if (uri.port != -1) ":${uri.port}" else ""
        "${uri.scheme}://${uri.host}$port"
    }.getOrNull()
}
