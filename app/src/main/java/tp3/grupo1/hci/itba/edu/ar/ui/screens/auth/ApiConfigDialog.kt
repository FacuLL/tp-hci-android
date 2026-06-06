package tp3.grupo1.hci.itba.edu.ar.ui.screens.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.ui.components.LuminaTextField

/**
 * Server connection editor reachable from the login screen, so the API
 * address (IP and port) can be changed even without a valid session.
 */
@Composable
fun ApiConfigDialog(
    apiBaseUrl: String,
    apiKey: String,
    @StringRes urlError: Int?,
    onUrlChange: (String) -> Unit,
    onKeyChange: (String) -> Unit,
    onResetDefaults: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.auth_api_config_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.auth_api_config_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
                LuminaTextField(
                    value = apiBaseUrl,
                    onValueChange = onUrlChange,
                    label = stringResource(R.string.auth_api_url_label),
                    required = true,
                    error = urlError?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                LuminaTextField(
                    value = apiKey,
                    onValueChange = onKeyChange,
                    label = stringResource(R.string.auth_api_key_label),
                    required = true,
                )
                TextButton(onClick = onResetDefaults) {
                    Text(stringResource(R.string.auth_api_defaults))
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
