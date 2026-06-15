package tp3.grupo1.hci.itba.edu.ar.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import tp3.grupo1.hci.itba.edu.ar.data.model.Routine
import tp3.grupo1.hci.itba.edu.ar.data.model.RoutineActionResult
import tp3.grupo1.hci.itba.edu.ar.data.model.RoutineUpsertRequest
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

    suspend fun create(request: RoutineUpsertRequest): Routine {
        val routine = apiCall { api.routines.create(request) }
        _routines.update { current -> current + routine }
        return routine
    }

    suspend fun update(id: String, request: RoutineUpsertRequest): Routine {
        val routine = apiCall { api.routines.update(id, request) }
        _routines.update { current -> current.map { if (it.id == id) routine else it } }
        return routine
    }

    suspend fun delete(id: String) {
        apiCall { api.routines.delete(id).close() }
        _routines.update { current -> current.filterNot { it.id == id } }
    }

    // Los cambios de estado llegan por eventos WebSocket, por eso no se hacen consultas de estado extra despues.
    suspend fun execute(routineId: String): List<RoutineActionResult> =
        apiCall { api.routines.execute(routineId, emptyBody()) }

    private fun emptyBody(): RequestBody = ByteArray(0).toRequestBody(null)
}
