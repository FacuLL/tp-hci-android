package tp3.grupo1.hci.itba.edu.ar.ui.screens.devices

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.data.model.Room
import tp3.grupo1.hci.itba.edu.ar.ui.components.ErrorBanner
import tp3.grupo1.hci.itba.edu.ar.ui.components.LoadingButton

@Composable
fun AssignRoomDialog(
    deviceName: String,
    currentRoomId: String?,
    rooms: List<Room>,
    busy: Boolean,
    @StringRes apiErrorRes: Int?,
    onDismiss: () -> Unit,
    onConfirm: (roomId: String?) -> Unit,
) {
    // String vacio representa "sin habitacion" para que el estado sea saveable.
    var selectedRoomId by rememberSaveable { mutableStateOf(currentRoomId.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.devices_assign_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.devices_assign_subtitle, deviceName),
                    style = MaterialTheme.typography.bodyMedium,
                )
                apiErrorRes?.let { ErrorBanner(stringResource(it)) }
                if (rooms.isEmpty()) {
                    Text(
                        stringResource(R.string.devices_assign_no_rooms),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .selectableGroup(),
                ) {
                    RoomOption(
                        label = stringResource(R.string.devices_no_room),
                        selected = selectedRoomId.isEmpty(),
                        enabled = !busy,
                        onSelect = { selectedRoomId = "" },
                    )
                    rooms.forEach { room ->
                        RoomOption(
                            label = room.name,
                            selected = selectedRoomId == room.id,
                            enabled = !busy,
                            onSelect = { selectedRoomId = room.id },
                        )
                    }
                }
            }
        },
        confirmButton = {
            LoadingButton(
                text = stringResource(R.string.action_save),
                onClick = { onConfirm(selectedRoomId.takeIf { it.isNotEmpty() }) },
                loading = busy,
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
private fun RoomOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
