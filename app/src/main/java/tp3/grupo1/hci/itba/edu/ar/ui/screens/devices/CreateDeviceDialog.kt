package tp3.grupo1.hci.itba.edu.ar.ui.screens.devices

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.data.model.Room
import tp3.grupo1.hci.itba.edu.ar.domain.DeviceTypeIds
import tp3.grupo1.hci.itba.edu.ar.domain.Validators
import tp3.grupo1.hci.itba.edu.ar.domain.deviceTypeColor
import tp3.grupo1.hci.itba.edu.ar.domain.deviceTypeIcon
import tp3.grupo1.hci.itba.edu.ar.domain.deviceTypeNameRes
import tp3.grupo1.hci.itba.edu.ar.ui.components.ErrorBanner
import tp3.grupo1.hci.itba.edu.ar.ui.components.LoadingButton
import tp3.grupo1.hci.itba.edu.ar.ui.components.LuminaTextField

/**
 * Creation form mirroring the web app's stepper as a single dialog: name,
 * device type (required, with the catalog icon and accent color) and an
 * optional room of the current home.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDeviceDialog(
    rooms: List<Room>,
    creating: Boolean,
    @StringRes apiErrorRes: Int?,
    onDismiss: () -> Unit,
    onCreate: (name: String, typeId: String, roomId: String?) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedTypeId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedRoomId by rememberSaveable { mutableStateOf<String?>(null) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var roomMenuExpanded by remember { mutableStateOf(false) }

    // After the first submit attempt every field revalidates as it changes.
    val nameError = if (submitted) Validators.name(name)?.let { stringResource(it) } else null
    val showTypeError = submitted && selectedTypeId == null

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(stringResource(R.string.devices_create_title), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.required_fields_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LuminaTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = stringResource(R.string.devices_field_name),
                        required = true,
                        error = nameError,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "${stringResource(R.string.devices_field_type)} *",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        DeviceTypeIds.CREATABLE.chunked(3).forEach { rowTypes ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                rowTypes.forEach { typeId ->
                                    DeviceTypeOption(
                                        typeId = typeId,
                                        selected = selectedTypeId == typeId,
                                        onSelect = { selectedTypeId = typeId },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                    )
                                }
                                repeat(3 - rowTypes.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                        if (showTypeError) {
                            Text(
                                stringResource(R.string.devices_type_required),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = roomMenuExpanded,
                        onExpandedChange = { roomMenuExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = rooms.firstOrNull { it.id == selectedRoomId }?.name
                                ?: stringResource(R.string.devices_no_room),
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = {
                                Text(
                                    "${stringResource(R.string.devices_field_room)} " +
                                        stringResource(R.string.optional_label)
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roomMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = roomMenuExpanded,
                            onDismissRequest = { roomMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.devices_no_room)) },
                                onClick = {
                                    selectedRoomId = null
                                    roomMenuExpanded = false
                                },
                            )
                            rooms.forEach { room ->
                                DropdownMenuItem(
                                    text = { Text(room.name) },
                                    onClick = {
                                        selectedRoomId = room.id
                                        roomMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    apiErrorRes?.let { ErrorBanner(stringResource(it)) }
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = onDismiss, enabled = !creating) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    LoadingButton(
                        text = stringResource(R.string.action_create),
                        onClick = {
                            submitted = true
                            val typeId = selectedTypeId
                            if (Validators.name(name) == null && typeId != null) {
                                onCreate(name.trim(), typeId, selectedRoomId)
                            }
                        },
                        loading = creating,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceTypeOption(
    typeId: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = deviceTypeColor(typeId)
    OutlinedCard(
        onClick = onSelect,
        modifier = modifier,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    deviceTypeIcon(typeId),
                    contentDescription = null,
                    tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            deviceTypeNameRes(typeId)?.let { nameRes ->
                Text(
                    stringResource(nameRes),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
