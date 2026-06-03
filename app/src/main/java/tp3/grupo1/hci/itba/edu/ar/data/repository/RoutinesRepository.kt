package tp3.grupo1.hci.itba.edu.ar.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import tp3.grupo1.hci.itba.edu.ar.data.model.Routine
import tp3.grupo1.hci.itba.edu.ar.data.model.RoutineExecuteResponse
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiProvider
import tp3.grupo1.hci.itba.edu.ar.data.network.apiCall

class RoutinesRepository(private val api: ApiProvider) {

    private val _routines = MutableStateFlow<List<Routine>>(emptyList())
    val routines: StateFlow<List<Routine>> = _routines.asStateFlow()

    suspend fun refresh() {
        _routines.value = apiCall { api.routines.getAll() }
    }

    fun clear() {
        _routines.value = emptyList()
    }

    /**
     * Executes the routine server-side. Device state changes arrive through
     * WebSocket events, so no extra state queries are issued afterwards (the
     * web version was marked down for that).
     */
    suspend fun execute(routineId: String): RoutineExecuteResponse =
        apiCall { api.routines.execute(routineId, emptyBody()) }

    private fun emptyBody(): RequestBody = ByteArray(0).toRequestBody(null)
}
