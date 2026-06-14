package tp3.grupo1.hci.itba.edu.ar.ui.screens.devices.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonElement
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.domain.AlarmAtom
import tp3.grupo1.hci.itba.edu.ar.domain.AlarmStatusAtom
import tp3.grupo1.hci.itba.edu.ar.domain.DispenseAtom
import tp3.grupo1.hci.itba.edu.ar.domain.PlaybackAtom
import tp3.grupo1.hci.itba.edu.ar.ui.components.LoadingButton
import tp3.grupo1.hci.itba.edu.ar.ui.screens.devices.DeviceDetailDialog
import kotlin.math.roundToInt

@Composable
internal fun AlarmStatusControl(atom: AlarmStatusAtom) {
    val armed = atom.currentStatus == "armedStay" || atom.currentStatus == "armedAway"
    val labelRes = when (atom.currentStatus) {
        "armedStay" -> R.string.device_state_armed_stay
        "armedAway" -> R.string.device_state_armed_away
        else -> R.string.device_state_disarmed
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (armed) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
            contentColor = if (armed) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.device_detail_status_label),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Arm/disarm buttons; every transition asks for the security code in a dialog. */
@Composable
internal fun AlarmControl(
    atom: AlarmAtom,
    deviceStatus: String?,
    onOpenDialog: (DeviceDetailDialog) -> Unit,
) {
    val armed = deviceStatus == "armedStay" || deviceStatus == "armedAway"
    // Always offer every transition (like the web app); disable only the one that
    // matches the current state so house<->away can be switched in a single step.
    ControlCard {
        atom.armActions.forEach { arm ->
            val isCurrent = when (arm.action) {
                "armStay" -> deviceStatus == "armedStay"
                "armAway" -> deviceStatus == "armedAway"
                else -> false
            }
            OutlinedButton(
                onClick = { onOpenDialog(DeviceDetailDialog.SecurityCode(arm.action, arm.labelRes)) },
                enabled = !isCurrent,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(arm.labelRes))
            }
        }
        OutlinedButton(
            onClick = {
                onOpenDialog(
                    DeviceDetailDialog.SecurityCode(atom.disarmAction, R.string.device_action_disarm)
                )
            },
            enabled = armed,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.device_action_disarm))
        }
    }
}

@Composable
internal fun ChangeCodeControl(onOpenDialog: (DeviceDetailDialog) -> Unit) {
    ControlCard {
        OutlinedButton(
            onClick = { onOpenDialog(DeviceDetailDialog.ChangeCode) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.device_action_change_security_code))
        }
    }
}

@Composable
internal fun DispenseControl(
    atom: DispenseAtom,
    accent: Color,
    deviceStatus: String?,
    dispensing: Boolean,
    onDispense: (String, Int, String) -> Unit,
) {
    // Same rule as the web app: a closed faucet cannot dispense.
    val closed = deviceStatus == "closed"
    var quantity by remember(atom) { mutableFloatStateOf(atom.quantityMin.toFloat()) }
    var unit by remember(atom) { mutableStateOf(atom.units.firstOrNull().orEmpty()) }
    ControlCard {
        SectionLabel(stringResource(R.string.device_action_dispense))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.device_detail_quantity_label),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${quantity.roundToInt()} $unit",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = accent,
            )
        }
        Slider(
            value = quantity,
            onValueChange = { quantity = it },
            valueRange = atom.quantityMin.toFloat()..atom.quantityMax.toFloat(),
            steps = (atom.quantityMax - atom.quantityMin - 1).coerceAtLeast(0),
            enabled = !dispensing && !closed,
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
        )
        if (atom.units.isNotEmpty()) {
            DropdownSelect(
                label = stringResource(R.string.device_detail_unit_label),
                options = atom.units,
                value = unit,
                optionLabel = { it },
                onSelect = { unit = it },
                enabled = !dispensing && !closed,
            )
        }
        LoadingButton(
            text = stringResource(R.string.device_action_dispense),
            loading = dispensing,
            enabled = !closed,
            onClick = { onDispense(atom.action, quantity.roundToInt(), unit) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (closed) {
            Text(
                text = stringResource(R.string.device_detail_dispense_closed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun PlaybackControl(
    atom: PlaybackAtom,
    accent: Color,
    onExecute: (String, List<JsonElement>) -> Unit,
) {
    val playing = atom.status == "playing" || atom.status == "on" || atom.status == "active"
    val paused = atom.status == "paused"
    val toggleAction = when {
        playing -> atom.pause ?: atom.stop
        paused -> atom.resume ?: atom.play
        else -> atom.play
    }
    ControlCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            atom.prev?.let { prev ->
                IconButton(onClick = { onExecute(prev, emptyList()) }) {
                    Icon(
                        imageVector = Icons.Outlined.SkipPrevious,
                        contentDescription = actionLabel(prev),
                    )
                }
            }
            toggleAction?.let { action ->
                FilledIconButton(
                    onClick = { onExecute(action, emptyList()) },
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = accent,
                        contentColor = if (accent.luminance() > 0.5f) Color.Black else Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = actionLabel(action),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            atom.stop?.let { stop ->
                IconButton(onClick = { onExecute(stop, emptyList()) }) {
                    Icon(
                        imageVector = Icons.Outlined.Stop,
                        contentDescription = actionLabel(stop),
                    )
                }
            }
            atom.next?.let { next ->
                IconButton(onClick = { onExecute(next, emptyList()) }) {
                    Icon(
                        imageVector = Icons.Outlined.SkipNext,
                        contentDescription = actionLabel(next),
                    )
                }
            }
        }
    }
}

@Composable
internal fun PlaylistControl(onOpenDialog: (DeviceDetailDialog) -> Unit) {
    ControlCard {
        OutlinedButton(
            onClick = { onOpenDialog(DeviceDetailDialog.Playlist) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Outlined.QueueMusic,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.device_action_get_playlist),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
