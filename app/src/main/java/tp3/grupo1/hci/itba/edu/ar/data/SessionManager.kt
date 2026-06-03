package tp3.grupo1.hci.itba.edu.ar.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Holds the auth token in memory and optionally persists it. When the user
 * chooses to keep the session open the token is stored in DataStore, so the
 * app opens straight into the dashboard on the next launch.
 */
class SessionManager(
    private val preferences: AppPreferences,
    private val scope: CoroutineScope,
) {
    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _initialized = MutableStateFlow(false)
    val initialized: StateFlow<Boolean> = _initialized.asStateFlow()

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    init {
        scope.launch {
            _token.value = preferences.token.first()
            _initialized.value = true
        }
    }

    val isLoggedIn: Boolean get() = _token.value != null

    fun currentToken(): String? = _token.value

    suspend fun startSession(token: String, remember: Boolean) {
        _token.value = token
        preferences.setRememberSession(remember)
        preferences.setToken(if (remember) token else null)
    }

    suspend fun clearSession() {
        _token.value = null
        preferences.setToken(null)
        preferences.setCurrentHomeId(null)
    }

    /** Invoked when the API answers 401 to an authenticated request. */
    fun onUnauthorized() {
        if (_token.value == null) return
        scope.launch {
            clearSession()
            _sessionExpired.tryEmit(Unit)
        }
    }
}
