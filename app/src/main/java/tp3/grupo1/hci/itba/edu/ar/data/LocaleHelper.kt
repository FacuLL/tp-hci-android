package tp3.grupo1.hci.itba.edu.ar.data

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

// Aplica el idioma elegido en la app (RNF3) envolviendo un Context con el locale. SYSTEM deja el locale del dispositivo.
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
