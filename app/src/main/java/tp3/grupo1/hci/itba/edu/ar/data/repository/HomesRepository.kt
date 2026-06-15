package tp3.grupo1.hci.itba.edu.ar.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import tp3.grupo1.hci.itba.edu.ar.data.AppPreferences
import tp3.grupo1.hci.itba.edu.ar.data.model.Home
import tp3.grupo1.hci.itba.edu.ar.data.model.HomeCreateRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.HomeShareRequest
import tp3.grupo1.hci.itba.edu.ar.data.model.HomeUpdateRequest
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiException
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiProvider
import tp3.grupo1.hci.itba.edu.ar.data.network.apiCall

// Resultado de crear o compartir un hogar: que invitaciones no se pudieron enviar.
data class ShareOutcome(
    val home: Home,
    val failedEmails: List<String>,
)

class HomesRepository(
    private val api: ApiProvider,
    private val preferences: AppPreferences,
    scope: CoroutineScope,
) {
    private val _homes = MutableStateFlow<List<Home>>(emptyList())
    val homes: StateFlow<List<Home>> = _homes.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    val currentHome: StateFlow<Home?> =
        combine(_homes, preferences.currentHomeId) { homes, id ->
            homes.firstOrNull { it.id == id } ?: homes.firstOrNull()
        }.stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun refresh() {
        val homes = apiCall { api.homes.getAll() }
        _homes.value = homes
        _loaded.value = true
        val storedId = preferences.currentHomeId.first()
        if (homes.none { it.id == storedId }) {
            preferences.setCurrentHomeId(homes.firstOrNull()?.id)
        }
    }

    suspend fun selectHome(homeId: String) {
        preferences.setCurrentHomeId(homeId)
    }

    // Comparte con cada invitado individualmente para que un email no registrado no aborte todo el flujo y se sepa cuales fallaron.
    suspend fun create(name: String, inviteEmails: List<String>): ShareOutcome {
        val home = apiCall { api.homes.create(HomeCreateRequest(name)) }
        _homes.update { it + home }
        if (preferences.currentHomeId.first() == null) {
            preferences.setCurrentHomeId(home.id)
        }
        val failed = shareIndividually(home.id, inviteEmails)
        if (inviteEmails.size > failed.size) refreshHome(home.id)
        return ShareOutcome(currentSnapshot(home.id) ?: home, failed)
    }

    suspend fun rename(homeId: String, name: String): Home {
        val updated = apiCall { api.homes.update(homeId, HomeUpdateRequest(name = name)) }
        replaceHome(updated)
        return updated
    }

    // Guarda la tarifa (ARS/kWh) en metadata["tariff"] preservando el resto de las claves.
    suspend fun updateTariff(homeId: String, tariff: Double): Home {
        val current = currentSnapshot(homeId)?.metadata ?: JsonObject(emptyMap())
        val metadata = JsonObject(current + ("tariff" to JsonPrimitive(tariff)))
        val updated = apiCall { api.homes.update(homeId, HomeUpdateRequest(metadata = metadata)) }
        replaceHome(updated)
        return updated
    }

    suspend fun delete(homeId: String) {
        apiCall { api.homes.delete(homeId).close() }
        _homes.update { list -> list.filterNot { it.id == homeId } }
        if (preferences.currentHomeId.first() == homeId) {
            preferences.setCurrentHomeId(_homes.value.firstOrNull()?.id)
        }
    }

    suspend fun share(homeId: String, emails: List<String>): ShareOutcome {
        val failed = shareIndividually(homeId, emails)
        val home = if (emails.size > failed.size) refreshHome(homeId) else currentSnapshot(homeId)
        return ShareOutcome(home ?: error("Home not loaded"), failed)
    }

    suspend fun unshare(homeId: String, email: String) {
        apiCall { api.homes.unshare(homeId, HomeShareRequest(listOf(email))) }
        refreshHome(homeId)
    }

    suspend fun refreshHome(homeId: String): Home? = try {
        val home = apiCall { api.homes.getById(homeId) }
        replaceHome(home)
        home
    } catch (_: ApiException) {
        null
    }

    private suspend fun shareIndividually(homeId: String, emails: List<String>): List<String> {
        val failed = mutableListOf<String>()
        for (email in emails) {
            try {
                apiCall { api.homes.share(homeId, HomeShareRequest(listOf(email))) }
            } catch (_: ApiException) {
                failed.add(email)
            }
        }
        return failed
    }

    private fun currentSnapshot(homeId: String): Home? =
        _homes.value.firstOrNull { it.id == homeId }

    private fun replaceHome(home: Home) {
        _homes.update { list -> list.map { if (it.id == home.id) home else it } }
    }
}
