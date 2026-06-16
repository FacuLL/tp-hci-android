package tp3.grupo1.hci.itba.edu.ar.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import tp3.grupo1.hci.itba.edu.ar.BuildConfig
import tp3.grupo1.hci.itba.edu.ar.data.notifications.NotificationCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "lumina_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

// SYSTEM sigue el locale del dispositivo; ES/EN fuerzan ese idioma sin importarlo.
enum class AppLanguage { SYSTEM, ES, EN }

class AppPreferences(private val context: Context) {

    companion object {
        const val DEFAULT_BASE_URL = "https://hci.it.itba.edu.ar/api"

        // Inyectada en build time desde local.properties / env (ver build.gradle.kts).
        // Ya no se versiona: si falta, queda vacia y la app no autentica.
        val DEFAULT_API_KEY: String = BuildConfig.API_KEY

        private val API_BASE_URL = stringPreferencesKey("api_base_url")
        private val API_KEY = stringPreferencesKey("api_key")
        private val AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val REMEMBER_SESSION = booleanPreferencesKey("remember_session")
        private val CURRENT_HOME_ID = stringPreferencesKey("current_home_id")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val LANGUAGE = stringPreferencesKey("language")
        private val NOTIF_CATEGORIES = stringSetPreferencesKey("notif_categories_enabled")
        private val RECENT_COLORS = stringPreferencesKey("recent_colors")

        private const val BOOTSTRAP_PREFS = "lumina_bootstrap"
        private const val BOOTSTRAP_LANGUAGE = "language"

        // Cuantos colores custom recordamos y como los serializamos (lista delimitada en una sola clave).
        private const val MAX_RECENT_COLORS = 5
        private const val RECENT_COLORS_SEPARATOR = ","
    }

    // El idioma debe leerse sincronicamente en attachBaseContext, antes de que una corutina pueda leer DataStore.
    // Lo espejamos en un SharedPreferences plano para aplicar el locale en arranque en frio sin bloquear.
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

    // Categorias de notificacion que el usuario quiere recibir. Clave ausente significa el default: todas habilitadas.
    val enabledNotificationCategories: Flow<Set<NotificationCategory>> = context.dataStore.data
        .map { prefs ->
            val stored = prefs[NOTIF_CATEGORIES]
                ?: return@map NotificationCategory.entries.toSet()
            stored.mapNotNull { name ->
                NotificationCategory.entries.firstOrNull { it.name == name }
            }.toSet()
        }
        .distinctUntilChanged()

    // Ultimos colores custom elegidos en la lampara, mas reciente primero. Persistidos como hex delimitados.
    val recentColors: Flow<List<String>> = context.dataStore.data
        .map { prefs ->
            prefs[RECENT_COLORS]
                ?.split(RECENT_COLORS_SEPARATOR)
                ?.filter { it.isNotBlank() }
                ?: emptyList()
        }
        .distinctUntilChanged()

    // Antepone el color elegido, deduplica y recorta al tope para que la fila de "Recientes" no crezca sin limite.
    suspend fun addRecentColor(hex: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[RECENT_COLORS]
                ?.split(RECENT_COLORS_SEPARATOR)
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            val updated = (listOf(hex) + current)
                .distinct()
                .take(MAX_RECENT_COLORS)
            prefs[RECENT_COLORS] = updated.joinToString(RECENT_COLORS_SEPARATOR)
        }
    }

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

    // Lectura sincronica del idioma persistido para usar en attachBaseContext.
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
        // Mantiene el espejo de bootstrap sincronizado con el valor de DataStore.
        bootstrapPrefs.edit().putString(BOOTSTRAP_LANGUAGE, value.name).apply()
        context.dataStore.edit { it[LANGUAGE] = value.name }
    }
}
