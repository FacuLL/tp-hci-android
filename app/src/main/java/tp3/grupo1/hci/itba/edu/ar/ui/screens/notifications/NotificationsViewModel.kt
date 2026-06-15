package tp3.grupo1.hci.itba.edu.ar.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tp3.grupo1.hci.itba.edu.ar.AppContainer
import tp3.grupo1.hci.itba.edu.ar.data.notifications.StoredNotification
import tp3.grupo1.hci.itba.edu.ar.ui.luminaContainer

class NotificationsViewModel(container: AppContainer) : ViewModel() {

    private val store = container.notificationStore

    val notifications: StateFlow<List<StoredNotification>> = store.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), store.notifications.value)

    /** Marks every notification read; called when the screen is opened. */
    fun markAllRead() {
        viewModelScope.launch { store.markAllRead() }
    }

    fun clearAll() {
        viewModelScope.launch { store.clear() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { NotificationsViewModel(luminaContainer()) }
        }
    }
}
