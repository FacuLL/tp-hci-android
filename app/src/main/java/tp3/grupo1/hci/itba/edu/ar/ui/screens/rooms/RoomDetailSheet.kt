package tp3.grupo1.hci.itba.edu.ar.ui.screens.rooms

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DevicesOther
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

/**
 * Devices of a room with quick toggle and add actions. Used both as the body of
 * the room detail screen and as the side panel on large tablets. Unlinking a
 * device is done from the device's "change room" modal, not here.
 */
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
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(
                text = room.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(R.plurals.rooms_device_count, devices.size, devices.size),
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
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(devices, key = { it.id }) { device ->
                    RoomDeviceRow(
                        device = device,
                        type = types[device.type.id],
                        pending = device.id in pendingDeviceIds,
                        onOpen = { onOpenDevice(device.id) },
                        onToggle = { onToggleDevice(device) },
                    )
                }
            }
        }
        Button(onClick = onAddDevice, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.rooms_add_device))
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
                // Same safety rule as the device detail (DeviceControlAtoms): an
                // open/close toggle is blocked while locked, and a lock toggle is
                // blocked while the door is open.
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

/** Device type icon over its accent color, shared by the room device lists. */
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
