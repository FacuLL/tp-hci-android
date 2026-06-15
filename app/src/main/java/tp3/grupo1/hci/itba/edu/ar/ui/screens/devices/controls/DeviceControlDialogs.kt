package tp3.grupo1.hci.itba.edu.ar.ui.screens.devices.controls

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.ui.components.ErrorBanner
import tp3.grupo1.hci.itba.edu.ar.ui.components.LoadingButton
import tp3.grupo1.hci.itba.edu.ar.ui.components.LuminaTextField
import tp3.grupo1.hci.itba.edu.ar.ui.screens.devices.PlaylistEntry
import tp3.grupo1.hci.itba.edu.ar.ui.screens.devices.PlaylistUiState

private const val SECURITY_CODE_LENGTH = 4

@StringRes
private fun securityCodeError(code: String): Int? = when {
    code.isBlank() -> R.string.validation_required
    code.length != SECURITY_CODE_LENGTH -> R.string.device_detail_code_length
    else -> null
}

@Composable
internal fun SecurityCodeDialog(
    @StringRes titleRes: Int,
    busy: Boolean,
    @StringRes apiErrorRes: Int?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val codeErrorRes = securityCodeError(code)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                apiErrorRes?.let { ErrorBanner(stringResource(it)) }
                CodeTextField(
                    value = code,
                    onValueChange = { code = it },
                    labelRes = R.string.device_detail_code_label,
                    errorRes = if (submitted) codeErrorRes else null,
                    enabled = !busy,
                    supportingText = stringResource(R.string.device_detail_code_hint),
                )
            }
        },
        confirmButton = {
            LoadingButton(
                text = stringResource(R.string.action_confirm),
                loading = busy,
                onClick = {
                    submitted = true
                    if (codeErrorRes == null) onConfirm(code)
                },
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
internal fun ChangeCodeDialog(
    busy: Boolean,
    @StringRes apiErrorRes: Int?,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var currentCode by rememberSaveable { mutableStateOf("") }
    var newCode by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val currentErrorRes = securityCodeError(currentCode)
    val newErrorRes = securityCodeError(newCode)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.device_action_change_security_code)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                apiErrorRes?.let { ErrorBanner(stringResource(it)) }
                CodeTextField(
                    value = currentCode,
                    onValueChange = { currentCode = it },
                    labelRes = R.string.device_detail_current_code_label,
                    errorRes = if (submitted) currentErrorRes else null,
                    enabled = !busy,
                )
                CodeTextField(
                    value = newCode,
                    onValueChange = { newCode = it },
                    labelRes = R.string.device_detail_new_code_label,
                    errorRes = if (submitted) newErrorRes else null,
                    enabled = !busy,
                )
            }
        },
        confirmButton = {
            LoadingButton(
                text = stringResource(R.string.action_save),
                loading = busy,
                onClick = {
                    submitted = true
                    if (currentErrorRes == null && newErrorRes == null) onConfirm(currentCode, newCode)
                },
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

// Selector de color custom: 3 sliders R/G/B (0-255) con preview en vivo. Devuelve el hex en "#RRGGBB"
// para que despache igual que los swatches. El color inicial se usa para arrancar los sliders.
@Composable
internal fun CustomColorDialog(
    initialColor: Color,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var red by rememberSaveable { mutableFloatStateOf((initialColor.red * 255f).roundToInt().toFloat()) }
    var green by rememberSaveable { mutableFloatStateOf((initialColor.green * 255f).roundToInt().toFloat()) }
    var blue by rememberSaveable { mutableFloatStateOf((initialColor.blue * 255f).roundToInt().toFloat()) }
    val hex = "#%02X%02X%02X".format(red.roundToInt(), green.roundToInt(), blue.roundToInt())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.device_color_custom_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(red.roundToInt(), green.roundToInt(), blue.roundToInt()))
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    )
                }
                Text(
                    text = hex,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                ColorChannelSlider(R.string.device_color_channel_red, red) { red = it }
                ColorChannelSlider(R.string.device_color_channel_green, green) { green = it }
                ColorChannelSlider(R.string.device_color_channel_blue, blue) { blue = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hex) }) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun ColorChannelSlider(
    @StringRes labelRes: Int,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value.roundToInt().toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
        )
    }
}

@Composable
private fun CodeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    @StringRes errorRes: Int?,
    enabled: Boolean,
    supportingText: String? = null,
) {
    LuminaTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(SECURITY_CODE_LENGTH)) },
        label = stringResource(labelRes),
        required = true,
        error = errorRes?.let { stringResource(it) },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = PasswordVisualTransformation(),
        supportingText = supportingText,
    )
}

@Composable
internal fun PlaylistDialog(
    playlist: PlaylistUiState,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.device_detail_playlist_title)) },
        text = {
            when (playlist) {
                PlaylistUiState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                PlaylistUiState.Error -> Text(stringResource(R.string.device_detail_playlist_error))
                is PlaylistUiState.Loaded -> if (playlist.songs.isEmpty()) {
                    Text(stringResource(R.string.device_detail_playlist_empty))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        itemsIndexed(playlist.songs) { index, song ->
                            if (index > 0) HorizontalDivider()
                            PlaylistSongRow(song)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

@Composable
private fun PlaylistSongRow(song: PlaylistEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            song.subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        song.duration?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
