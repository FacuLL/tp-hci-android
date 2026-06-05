package tp3.grupo1.hci.itba.edu.ar.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import tp3.grupo1.hci.itba.edu.ar.AppContainer
import tp3.grupo1.hci.itba.edu.ar.LuminaApplication

/**
 * Gives ViewModel factories access to the app container:
 *
 * ```
 * companion object {
 *     val Factory = viewModelFactory {
 *         initializer { LoginViewModel(luminaContainer()) }
 *     }
 * }
 * ```
 */
fun CreationExtras.luminaContainer(): AppContainer =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as LuminaApplication).container
