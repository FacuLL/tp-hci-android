package tp3.grupo1.hci.itba.edu.ar.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import tp3.grupo1.hci.itba.edu.ar.data.notifications.NotificationCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "lumina_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** SYSTEM follows the device locale; ES/EN force that language regardless of it. */
enum class AppLanguage { SYSTEM, ES, EN }

/**
 * Persistent user preferences. Includes the API connection settings required
 * by the chair (changeable IP and port) and the personalization options
 * (theme, session persistence).
 */
class AppPreferences(private val context: Context) {

    companion object {
        const val DEFAULT_BASE_URL = "https://hci.it.itba.edu.ar/api"
        const val DEFAULT_API_KEY = "sk_f9fd29ba6c848d6a0fcc2bb64ebd0783"

        private val API_BASE_URL = stringPreferencesKey("api_base_url")
        private val API_KEY = stringPreferencesKey("api_key")
        private val AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val REMEMBER_SESSION = booleanPreferencesKey("remember_session")
        private val CURRENT_HOME_ID = stringPreferencesKey("current_home_id")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val LANGUAGE = stringPreferencesKey("language")
        private val NOTIF_CATEGORIES = stringSetPreferencesKey("notif_categories_enabled")

        private const val BOOTSTRAP_PREFS = "lumina_bootstrap"
        private const val BOOTSTRAP_LANGUAGE = "language"
    }

    // The chosen language must be read synchronously inside Activity.attachBaseContext,
    // which runs before any coroutine can read DataStore. We mirror it into a plain
    // SharedPreferences so the locale can be applied at cold start without blocking.
    private val bootstrapPrefs =
        context.getSharedPreferences(BOOTSTRAP_PREFS, Context.MODE_PRIVATE)

    val apiBaseUrl: Flow<String> = context.dataStore.data
        .map { it[API_BASE_URL] ?: DEFAULT_BASE_URL }
        .distinctUntilChanged()

    val apiKey: Flow<String> = context.dataStore.data
        .map { it[API_KEY] ?: DEFAULT_API_KEY }
        .distinctUntilChanged()

    val token: Flow<String?> = context.dataStore.data
        .map { it[AUTH_TOKEN] }
        .distinctUntilChanged()

    val rememberSession: Flow<Boolean> = context.dataStore.data
        .map { it[REMEMBER_SESSION] ?: true }
        .distinctUntilChanged()

    val currentHomeId: Flow<String?> = context.dataStore.data
        .map { it[CURRENT_HOME_ID] }
        .distinctUntilChanged()

    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { prefs ->
            prefs[THEME_MODE]?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
                ?: ThemeMode.SYSTEM
        }
        .distinctUntilChanged()

    val language: Flow<AppLanguage> = context.dataStore.data
        .map { prefs ->
            prefs[LANGUAGE]?.let { stored -> AppLanguage.entries.firstOrNull { it.name == stored } }
                ?: AppLanguage.SYSTEM
        }
        .distinctUntilChanged()

    /**
     * Notification categories the user wants to receive. Absent key means the
     * default: all categories enabled.
     */
    val enabledNotificationCategories: Flow<Set<NotificationCategory>> = context.dataStore.data
        .map { prefs ->
            val stored = prefs[NOTIF_CATEGORIES]
                ?: return@map NotificationCategory.entries.toSet()
            stored.mapNotNull { name ->
                NotificationCategory.entries.firstOrNull { it.name == name }
            }.toSet()
        }
        .distinctUntilChanged()

    suspend fun setNotificationCategoryEnabled(category: NotificationCategory, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[NOTIF_CATEGORIES]
                ?: NotificationCategory.entries.map { it.name }.toSet()
            prefs[NOTIF_CATEGORIES] = if (enabled) {
                current + category.name
            } else {
                current - category.name
            }
        }
    }

    /** Synchronous read of the persisted language for use in attachBaseContext. */
    fun languageBlocking(): AppLanguage {
        val stored = bootstrapPrefs.getString(BOOTSTRAP_LANGUAGE, null)
        return AppLanguage.entries.firstOrNull { it.name == stored } ?: AppLanguage.SYSTEM
    }

    suspend fun setApiBaseUrl(value: String) {
        context.dataStore.edit { it[API_BASE_URL] = value }
    }

    suspend fun setApiKey(value: String) {
        context.dataStore.edit { it[API_KEY] = value }
    }

    suspend fun setToken(value: String?) {
        context.dataStore.edit {
            if (value == null) it.remove(AUTH_TOKEN) else it[AUTH_TOKEN] = value
        }
    }

    suspend fun setRememberSession(value: Boolean) {
        context.dataStore.edit { it[REMEMBER_SESSION] = value }
    }

    suspend fun setCurrentHomeId(value: String?) {
        context.dataStore.edit {
            if (value == null) it.remove(CURRENT_HOME_ID) else it[CURRENT_HOME_ID] = value
        }
    }

    suspend fun setThemeMode(value: ThemeMode) {
        context.dataStore.edit { it[THEME_MODE] = value.name }
    }

    suspend fun setLanguage(value: AppLanguage) {
        // Keep the synchronous bootstrap mirror in sync with the DataStore value.
        bootstrapPrefs.edit().putString(BOOTSTRAP_LANGUAGE, value.name).apply()
        context.dataStore.edit { it[LANGUAGE] = value.name }
    }
}
