package tp3.grupo1.hci.itba.edu.ar.ui.screens.rooms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.data.model.Device
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceType
import tp3.grupo1.hci.itba.edu.ar.data.model.Room
import tp3.grupo1.hci.itba.edu.ar.domain.PowerAtom
import tp3.grupo1.hci.itba.edu.ar.domain.deviceControls
import tp3.grupo1.hci.itba.edu.ar.domain.deviceStateText
import tp3.grupo1.hci.itba.edu.ar.domain.deviceTypeColor
import tp3.grupo1.hci.itba.edu.ar.domain.deviceTypeIcon
import tp3.grupo1.hci.itba.edu.ar.ui.components.EmptyState

// En editMode la lista pasa a ser una columna reordenable por drag-and-drop.
@Composable
fun RoomDetailContent(
    room: Room,
    devices: List<Device>,
    types: Map<String, DeviceType>,
    pendingDeviceIds: Set<String>,
    onOpenDevice: (String) -> Unit,
    onToggleDevice: (Device) -> Unit,
    onAddDevice: () -> Unit,
    modifier: Modifier = Modifier,
    // RoomDetailScreen ya muestra el nombre en su TopAppBar, pasa false para evitar el duplicado.
    showTitle: Boolean = true,
    editMode: Boolean = false,
    onMoveDevice: (from: Int, to: Int) -> Unit = { _, _ -> },
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        Column {
            if (showTitle) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = if (editMode) {
                    stringResource(R.string.rooms_reorder_hint)
                } else {
                    pluralStringResource(R.plurals.rooms_device_count, devices.size, devices.size)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (devices.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.DevicesOther,
                title = stringResource(R.string.rooms_room_empty_title),
                subtitle = stringResource(R.string.rooms_room_empty_subtitle),
            )
        } else if (editMode) {
            ReorderableDeviceList(
                devices = devices,
                types = types,
                onMoveDevice = onMoveDevice,
                modifier = Modifier.weight(1f, fill = false),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier.weight(1f, fill = false),
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                gridItems(devices, key = { it.id }) { device ->
                    RoomDeviceSquareCard(
                        device = device,
                        type = types[device.type.id],
                        pending = device.id in pendingDeviceIds,
                        onOpen = { onOpenDevice(device.id) },
                        onToggle = { onToggleDevice(device) },
                    )
                }
            }
        }
        if (!editMode) {
            Button(
                onClick = onAddDevice,
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.rooms_add_device))
            }
        }
        }
    }
}

@Composable
private fun ReorderableDeviceList(
    devices: List<Device>,
    types: Map<String, DeviceType>,
    onMoveDevice: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onMoveDevice(from.index, to.index)
    }
    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(devices, key = { it.id }) { device ->
            ReorderableItem(reorderableState, key = device.id) { isDragging ->
                val elevation = if (isDragging) 8.dp else 0.dp
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation, MaterialTheme.shapes.medium),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        DeviceTypeBadge(typeId = device.type.id)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = deviceStateText(LocalContext.current, device),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .draggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(
                                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress,
                                        )
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DragHandle,
                                contentDescription = stringResource(R.string.rooms_cd_drag_device),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomDeviceRow(
    device: Device,
    type: DeviceType?,
    pending: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
) {
    val context = LocalContext.current
    val powerAtom = remember(device, type) {
        type?.let { deviceControls(it, device).filterIsInstance<PowerAtom>().firstOrNull() }
    }
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DeviceTypeBadge(typeId = device.type.id)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = deviceStateText(context, device),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (powerAtom != null) {
                // Misma regla de seguridad que el detalle: open/close bloqueado si esta locked, y lock bloqueado si la puerta esta abierta.
                val togglesOpening = powerAtom.onAction == "open" || powerAtom.offAction == "open"
                val blocked = (togglesOpening && device.state.lock == "locked") ||
                    (powerAtom.onAction == "lock" && device.state.status == "opened")
                Switch(
                    checked = powerAtom.active,
                    onCheckedChange = { onToggle() },
                    enabled = !pending && !blocked,
                    colors = SwitchDefaults.colors(
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

/**
 * Card de device para la grilla de la habitacion. Sin aspect ratio fijo:
 * la altura la define el contenido para no desperdiciar el centro de la
 * card (la version anterior con `aspectRatio(1f)` dejaba un hueco grande
 * entre el icono+switch arriba y el nombre+estado abajo). Asi entran
 * mas devices por pantalla y la card se ve mas compacta.
 */
@Composable
private fun RoomDeviceSquareCard(
    device: Device,
    type: DeviceType?,
    pending: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
) {
    val context = LocalContext.current
    val powerAtom = remember(device, type) {
        type?.let { deviceControls(it, device).filterIsInstance<PowerAtom>().firstOrNull() }
    }
    val active = powerAtom?.active == true
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Fila superior: icono accent a la izquierda + switch (si el
            // device tiene power atom) a la derecha.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DeviceTypeBadge(typeId = device.type.id)
                if (powerAtom != null) {
                    val togglesOpening = powerAtom.onAction == "open" ||
                        powerAtom.offAction == "open"
                    val blocked =
                        (togglesOpening && device.state.lock == "locked") ||
                        (powerAtom.onAction == "lock" && device.state.status == "opened")
                    Switch(
                        checked = active,
                        onCheckedChange = { onToggle() },
                        enabled = !pending && !blocked,
                        colors = SwitchDefaults.colors(
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = deviceStateText(context, device),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun DeviceTypeBadge(typeId: String, modifier: Modifier = Modifier) {
    val color = deviceTypeColor(typeId)
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = deviceTypeIcon(typeId),
            contentDescription = null,
            tint = color,
        )
    }
}
