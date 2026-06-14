package tp3.grupo1.hci.itba.edu.ar.data

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Applies the in-app language choice (RNF3) by wrapping a Context with the chosen
 * locale. Every stringResource call then resolves against the matching values-*
 * folder regardless of the device language. SYSTEM leaves the device locale in place.
 */
object LocaleHelper {

    fun wrap(context: Context, language: AppLanguage): Context {
        val locale = when (language) {
            AppLanguage.SYSTEM -> return context
            AppLanguage.ES -> Locale("es")
            AppLanguage.EN -> Locale("en")
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
