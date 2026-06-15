package tp3.grupo1.hci.itba.edu.ar.ui.screens.devices.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import tp3.grupo1.hci.itba.edu.ar.domain.VacuumAtom
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonElement
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.domain.AlarmAtom
import tp3.grupo1.hci.itba.edu.ar.domain.DispenseAtom
import tp3.grupo1.hci.itba.edu.ar.domain.PlaybackAtom
import tp3.grupo1.hci.itba.edu.ar.domain.deviceValueLabel
import tp3.grupo1.hci.itba.edu.ar.ui.components.LoadingButton
import tp3.grupo1.hci.itba.edu.ar.ui.screens.devices.DeviceDetailDialog
import kotlin.math.roundToInt

/**
 * AlarmControl — card unica con grid 3-up de modos (Casa | Fuera | Desarmada).
 *
 * El modo activo se renderiza con `Card` filled en color de container (primary
 * para armado, errorContainer para desarmado), icono mas grande y label
 * destacada; los inactivos van como `OutlinedCard` tappables. Tap en uno
 * inactivo dispara `SecurityCodeDialog` para autorizar la transicion. El
 * activo no es clickeable (no abre dialog redundante).
 *
 * Cambio de codigo de seguridad va como `IconButton` en el header de la card,
 * sin ocupar un slot aparte (antes era una `ChangeCodeControl` con su propia
 * `ControlCard` y `OutlinedButton` full-width).
 */
@Composable
internal fun AlarmControl(
    atom: AlarmAtom,
    onOpenDialog: (DeviceDetailDialog) -> Unit,
) {
    ControlCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.alarm_mode_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (atom.changeCodeAction != null) {
                IconButton(onClick = { onOpenDialog(DeviceDetailDialog.ChangeCode) }) {
                    Icon(
                        imageVector = Icons.Outlined.VpnKey,
                        contentDescription = stringResource(R.string.device_action_change_security_code),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            atom.armActions.forEach { arm ->
                val isCurrent = when (arm.action) {
                    "armStay" -> atom.currentStatus == "armedStay"
                    "armAway" -> atom.currentStatus == "armedAway"
                    else -> false
                }
                AlarmModeCell(
                    icon = when (arm.action) {
                        "armStay" -> Icons.Outlined.Home
                        "armAway" -> Icons.Outlined.DirectionsRun
                        else -> Icons.Outlined.Shield
                    },
                    // Label corto para que entre en una linea aunque la
                    // celda sea angosta (3 columnas). El verbo completo
                    // ("Activar en casa") sigue apareciendo en el titulo
                    // del SecurityCodeDialog via arm.labelRes.
                    shortLabelRes = when (arm.action) {
                        "armStay" -> R.string.alarm_mode_short_stay
                        "armAway" -> R.string.alarm_mode_short_away
                        else -> arm.labelRes
                    },
                    isCurrent = isCurrent,
                    isDisarm = false,
                    onClick = {
                        onOpenDialog(DeviceDetailDialog.SecurityCode(arm.action, arm.labelRes))
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            val disarmCurrent = atom.currentStatus != "armedStay" && atom.currentStatus != "armedAway"
            AlarmModeCell(
                icon = Icons.Outlined.LockOpen,
                shortLabelRes = R.string.alarm_mode_short_disarm,
                isCurrent = disarmCurrent,
                isDisarm = true,
                onClick = {
                    onOpenDialog(
                        DeviceDetailDialog.SecurityCode(atom.disarmAction, R.string.device_action_disarm)
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Celda individual del grid 3-up de modos. `isCurrent=true` -> filled con
 * colores de container y el icono mas grande; `false` -> outlined,
 * clickeable, icono mas chico. `isDisarm=true` cambia el color de container
 * a `errorContainer` para diferenciar el modo destructivo del armado activo.
 */
@Composable
private fun AlarmModeCell(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    shortLabelRes: Int,
    isCurrent: Boolean,
    isDisarm: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Inactivos usan `surfaceContainerHigh` (no `surface`) para contrastar
    // claramente con el fondo de la ControlCard padre — sino las celdas
    // inactivas quedaban "invisibles" sobre el azul claro de la card.
    val containerColor = when {
        isCurrent && isDisarm -> MaterialTheme.colorScheme.errorContainer
        isCurrent             -> MaterialTheme.colorScheme.primaryContainer
        else                  -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when {
        isCurrent && isDisarm -> MaterialTheme.colorScheme.onErrorContainer
        isCurrent             -> MaterialTheme.colorScheme.onPrimaryContainer
        else                  -> MaterialTheme.colorScheme.onSurface
    }
    Card(
        // El activo no es clickeable: el dialog ya esta abierto-y-cerrado en
        // ese estado. Eso ademas elimina el efecto "button deshabilitado
        // ahi parado" que tenia la version anterior.
        onClick = if (isCurrent) ({}) else onClick,
        enabled = !isCurrent,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                // Iconos mas grandes para que la celda no se sienta vacia:
                // 36dp activo / 30dp inactivo (antes 32/24).
                modifier = Modifier.size(if (isCurrent) 36.dp else 30.dp),
            )
            Text(
                text = stringResource(shortLabelRes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun VacuumControl(
    atom: VacuumAtom,
    onExecute: (String, List<JsonElement>) -> Unit,
) {
    val status = atom.status
    val docked = status == "docked"
    val paused = status == "paused"
    // El API solo documenta "docked"; cualquier estado que no sea base ni pausa se trata como
    // "limpiando" (el string real de limpieza lo reconcilia el evento de WebSocket).
    val running = !docked && !paused && !status.isNullOrBlank()
    val statusRes = when {
        running -> R.string.vacuum_status_cleaning
        paused -> R.string.vacuum_status_paused
        else -> R.string.vacuum_status_docked
    }
    ControlCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (running) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
            )
            Text(
                text = stringResource(statusRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Button(
            onClick = { onExecute(if (running) atom.pauseAction else atom.startAction, emptyList()) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (running) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                contentColor = if (running) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (running) R.string.vacuum_pause else R.string.vacuum_start))
        }
        atom.dockAction?.let { dock ->
            OutlinedButton(
                onClick = { onExecute(dock, emptyList()) },
                enabled = !docked,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Home, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.vacuum_dock))
            }
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
    // Solo se puede dispensar con el grifo cerrado (sin agua corriendo); con el grifo abierto está deshabilitado.
    val canDispense = deviceStatus == "closed"
    var quantity by remember(atom) { mutableFloatStateOf(atom.quantityMin.toFloat()) }
    var unit by remember(atom) { mutableStateOf(atom.units.firstOrNull().orEmpty()) }
    // La API no avisa cuándo termina de dispensar, así que mantenemos un cooldown mínimo de 1s
    // tras el tap para evitar re-disparos instantáneos y dar feedback de carga.
    var cooldown by remember(atom) { mutableStateOf(false) }
    LaunchedEffect(cooldown) {
        if (cooldown) {
            delay(1000)
            cooldown = false
        }
    }
    val busy = dispensing || cooldown
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
            enabled = !busy && canDispense,
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
        )
        if (atom.units.isNotEmpty()) {
            DropdownSelect(
                label = stringResource(R.string.device_detail_unit_label),
                options = atom.units,
                value = unit,
                optionLabel = { it },
                onSelect = { unit = it },
                enabled = !busy && canDispense,
            )
        }
        LoadingButton(
            text = stringResource(R.string.device_action_dispense),
            loading = busy,
            enabled = canDispense,
            onClick = {
                cooldown = true
                onDispense(atom.action, quantity.roundToInt(), unit)
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (!canDispense) {
            Text(
                text = stringResource(R.string.device_detail_dispense_open),
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
    onOpenDialog: (DeviceDetailDialog) -> Unit,
) {
    val context = LocalContext.current
    val playing = atom.status == "playing" || atom.status == "on" || atom.status == "active"
    val paused = atom.status == "paused"
    val toggleAction = when {
        playing -> atom.pause ?: atom.stop
        paused -> atom.resume ?: atom.play
        else -> atom.play
    }
    val statusRes = when {
        playing -> R.string.device_state_playing
        paused -> R.string.device_state_paused
        else -> R.string.device_state_stopped
    }
    // El estado del parlante solo expone genero y volumen (no la cancion actual): el genero ocupa el
    // lugar destacado donde el boceto muestra el titulo, sin inventar un track ni una barra de progreso.
    val genreLabel = atom.genre?.let { deviceValueLabel(context, it) }
    ControlCard {
        // Cabecera: genero prominente arriba (como el titulo del boceto) con el estado como subtitulo.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.MusicNote,
                    contentDescription = null,
                    tint = accent,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = genreLabel ?: stringResource(statusRes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                if (genreLabel != null) {
                    Text(
                        text = stringResource(statusRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        // Transporte centrado: play/pausa grande y circular destacado, flanqueado por prev/next.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            atom.prev?.let { prev ->
                IconButton(
                    onClick = { onExecute(prev, emptyList()) },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SkipPrevious,
                        contentDescription = actionLabel(prev),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            toggleAction?.let { action ->
                FilledIconButton(
                    onClick = { onExecute(action, emptyList()) },
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = accent,
                        contentColor = if (accent.luminance() > 0.5f) Color.Black else Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = actionLabel(action),
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            atom.next?.let { next ->
                IconButton(
                    onClick = { onExecute(next, emptyList()) },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SkipNext,
                        contentDescription = actionLabel(next),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
        // Fila de volumen limpia: icono de parlante + valor actual (de solo lectura; el ajuste vive
        // en su propio control setVolume, no se duplica aca).
        atom.volume?.let { volume ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.device_detail_player_volume, volume),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        atom.playlistAction?.let {
            HorizontalDivider()
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
