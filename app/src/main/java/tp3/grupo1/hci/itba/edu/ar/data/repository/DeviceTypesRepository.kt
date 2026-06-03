package tp3.grupo1.hci.itba.edu.ar.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceType
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiProvider
import tp3.grupo1.hci.itba.edu.ar.data.network.apiCall

/** Device types are static catalog data, fetched once and cached in memory. */
class DeviceTypesRepository(private val api: ApiProvider) {

    private val mutex = Mutex()
    private val _types = MutableStateFlow<Map<String, DeviceType>>(emptyMap())
    val types: StateFlow<Map<String, DeviceType>> = _types.asStateFlow()

    suspend fun ensureLoaded(): Map<String, DeviceType> {
        if (_types.value.isNotEmpty()) return _types.value
        mutex.withLock {
            if (_types.value.isEmpty()) {
                _types.value = apiCall { api.deviceTypes.getAll() }.associateBy { it.id }
            }
        }
        return _types.value
    }

    fun byId(id: String): DeviceType? = _types.value[id]
}
