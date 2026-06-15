package tp3.grupo1.hci.itba.edu.ar.domain

import android.util.Patterns
import androidx.annotation.StringRes
import tp3.grupo1.hci.itba.edu.ar.R

// Validadores de formulario: cada uno devuelve el recurso del mensaje de error, o null si el valor es valido.
object Validators {

    @StringRes
    fun required(value: String): Int? =
        if (value.isBlank()) R.string.validation_required else null

    @StringRes
    fun email(value: String): Int? = when {
        value.isBlank() -> R.string.validation_required
        !Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches() -> R.string.validation_email
        else -> null
    }

    @StringRes
    fun password(value: String): Int? = when {
        value.isBlank() -> R.string.validation_required
        value.length < 8 -> R.string.validation_password_length
        value.none { it.isUpperCase() } -> R.string.validation_password_uppercase
        else -> null
    }

    @StringRes
    fun passwordConfirmation(password: String, confirmation: String): Int? = when {
        confirmation.isBlank() -> R.string.validation_required
        password != confirmation -> R.string.validation_password_match
        else -> null
    }

    // Nombres de usuarios, hogares, habitaciones y dispositivos: de 3 a 100 caracteres.
    @StringRes
    fun name(value: String): Int? = when {
        value.isBlank() -> R.string.validation_required
        value.trim().length < 3 -> R.string.validation_name_length
        value.trim().length > 100 -> R.string.validation_name_max
        else -> null
    }

    @StringRes
    fun code(value: String): Int? =
        if (value.isBlank()) R.string.validation_code_required else null
}
