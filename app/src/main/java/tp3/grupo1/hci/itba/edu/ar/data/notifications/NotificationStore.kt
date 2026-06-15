package tp3.grupo1.hci.itba.edu.ar.data.notifications

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.notificationsDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "lumina_notifications")

// Historial local persistido como una lista JSON, mas nuevo primero y limitado a MAX_ITEMS para que el archivo no crezca sin limite.
class NotificationStore(context: Context, scope: CoroutineScope) {

    companion object {
        private const val MAX_ITEMS = 100
        private val ITEMS = stringPreferencesKey("items")
    }

    private val dataStore = context.notificationsDataStore
    private val json = Json { ignoreUnknownKeys = true }

    val notifications: StateFlow<List<StoredNotification>> = dataStore.data
        .map { prefs -> decode(prefs[ITEMS]) }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val unreadCount: StateFlow<Int> = dataStore.data
        .map { prefs -> decode(prefs[ITEMS]).count { !it.read } }
        .stateIn(scope, SharingStarted.Eagerly, 0)

    suspend fun add(notification: StoredNotification) {
        dataStore.edit { prefs ->
            val current = decode(prefs[ITEMS])
            val updated = (listOf(notification) + current).take(MAX_ITEMS)
            prefs[ITEMS] = json.encodeToString(updated)
        }
    }

    suspend fun markAllRead() {
        dataStore.edit { prefs ->
            val current = decode(prefs[ITEMS])
            if (current.none { !it.read }) return@edit
            prefs[ITEMS] = json.encodeToString(current.map { it.copy(read = true) })
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs -> prefs.remove(ITEMS) }
    }

    private fun decode(raw: String?): List<StoredNotification> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<StoredNotification>>(raw)
        }.getOrDefault(emptyList())
    }
}
