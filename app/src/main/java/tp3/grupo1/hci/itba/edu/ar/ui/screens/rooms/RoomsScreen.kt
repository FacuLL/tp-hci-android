package tp3.grupo1.hci.itba.edu.ar.ui.screens.rooms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowWidthSizeClass
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.data.model.Device
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceType
import tp3.grupo1.hci.itba.edu.ar.data.model.Room
import tp3.grupo1.hci.itba.edu.ar.domain.deviceTypeNameRes
import tp3.grupo1.hci.itba.edu.ar.domain.devicesInRoom
import tp3.grupo1.hci.itba.edu.ar.domain.unassignedDevices
import tp3.grupo1.hci.itba.edu.ar.ui.screens.devices.CreateDeviceDialog
import tp3.grupo1.hci.itba.edu.ar.ui.components.CenteredLoading
import tp3.grupo1.hci.itba.edu.ar.ui.components.ConfirmDialog
import tp3.grupo1.hci.itba.edu.ar.ui.components.EmptyState
import tp3.grupo1.hci.itba.edu.ar.ui.components.ErrorBanner
import tp3.grupo1.hci.itba.edu.ar.ui.components.LoadingButton
import tp3.grupo1.hci.itba.edu.ar.ui.components.LuminaTextField
import tp3.grupo1.hci.itba.edu.ar.ui.components.ProfileAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomsScreen(
    onOpenDevice: (String) -> Unit,
    onOpenRoom: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val viewModel: RoomsViewModel = viewModel(factory = RoomsViewModel.Factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val widthClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    val isExpanded = widthClass == WindowWidthSizeClass.EXPANDED
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.snackbarMessageRes) {
        state.snackbarMessageRes?.let { messageRes ->
            snackbarHostState.showSnackbar(context.getString(messageRes))
            viewModel.onSnackbarShown()
        }
    }

    // A freshly created room opens right away by navigating to its detail.
    // The old two-pane layout (grid + side panel on EXPANDED widths) was
    // removed for consistency with the dashboard — see RoomsContent — so
    // even on tablets we navigate instead of just selecting in state.
    LaunchedEffect(state.roomToOpen) {
        state.roomToOpen?.let { roomId ->
            viewModel.onRoomOpened()
            onOpenRoom(roomId)
        }
    }

    // Tapping a room always navigates to its detail. Previously this branched
    // on `isExpanded` and called `viewModel.selectRoom` for tablets/landscape,
    // which became a no-op once the two-pane layout was removed — landscape
    // phones report EXPANDED in some configurations and the tap silently
    // did nothing.
    val openRoom: (String) -> Unit = { roomId ->
        onOpenRoom(roomId)
    }

    Scaffold(
        // El outer NavigationSuiteScaffold ya consumio los insets del sistema.
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rooms_title)) },
                actions = {
                    if (state.currentHome != null) {
                        IconButton(onClick = viewModel::openCreateDialog) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = stringResource(R.string.rooms_cd_add_room),
                            )
                        }
                    }
                    ProfileAvatar(onClick = onOpenSettings)
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val loadErrorRes = state.loadErrorRes
            when {
                state.loading -> CenteredLoading()
                loadErrorRes != null -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ErrorBanner(stringResource(loadErrorRes))
                    Button(onClick = viewModel::retry) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
                state.currentHome == null -> EmptyState(
                    icon = Icons.Outlined.HomeWork,
                    title = stringResource(R.string.rooms_no_home_title),
                    subtitle = stringResource(R.string.rooms_no_home_subtitle),
                )
                state.rooms.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.MeetingRoom,
                    title = stringResource(R.string.rooms_empty_title),
                    subtitle = stringResource(R.string.rooms_empty_subtitle),
                    actionLabel = stringResource(R.string.rooms_new_room),
                    onAction = viewModel::openCreateDialog,
                )
                else -> PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    RoomsContent(
                        state = state,
                        widthClass = widthClass,
                        onSelectRoom = openRoom,
                        onRenameRoom = viewModel::openRenameDialog,
                        onDeleteRoom = viewModel::openDeleteRoomDialog,
                        onOpenDevice = onOpenDevice,
                        onToggleDevice = viewModel::toggleDevice,
                        onAddDevice = viewModel::openAddDeviceDialog,
                    )
                }
            }
        }
    }

    when (val dialog = state.dialog) {
        null -> Unit
        RoomsDialog.Create -> RoomNameDialog(
            title = stringResource(R.string.rooms_new_room),
            confirmLabel = stringResource(R.string.action_create),
            name = state.nameInput,
            nameError = state.nameErrorRes?.let { stringResource(it) },
            apiError = state.dialogErrorRes?.let { stringResource(it) },
            saving = state.saving,
            onNameChange = viewModel::onNameChange,
            onSubmit = viewModel::submitName,
            onDismiss = viewModel::dismissDialog,
        )
        is RoomsDialog.Rename -> RoomNameDialog(
            title = stringResource(R.string.rooms_rename_room),
            confirmLabel = stringResource(R.string.action_save),
            name = state.nameInput,
            nameError = state.nameErrorRes?.let { stringResource(it) },
            apiError = state.dialogErrorRes?.let { stringResource(it) },
            saving = state.saving,
            onNameChange = viewModel::onNameChange,
            onSubmit = viewModel::submitName,
            onDismiss = viewModel::dismissDialog,
        )
        is RoomsDialog.ConfirmDeleteRoom -> ConfirmDialog(
            title = stringResource(R.string.rooms_delete_room_title),
            text = stringResource(R.string.rooms_delete_room_message, dialog.room.name),
            onConfirm = { viewModel.deleteRoom(dialog.room) },
            onDismiss = viewModel::dismissDialog,
        )
        is RoomsDialog.AddDevice -> AddDeviceDialog(
            unassigned = unassignedDevices(state.devices),
            types = state.types,
            pendingDeviceIds = state.pendingDeviceIds,
            onAssign = { device -> viewModel.assignDevice(device.id, dialog.room.id) },
            onCreateNew = { viewModel.openCreateDeviceDialog(dialog.room) },
            onDismiss = viewModel::dismissDialog,
        )
        is RoomsDialog.CreateDevice -> CreateDeviceDialog(
            rooms = state.rooms,
            creating = state.creatingDevice,
            apiErrorRes = state.createDeviceErrorRes,
            initialRoomId = dialog.room.id,
            onCreate = { name, typeId, roomId -> viewModel.createDevice(name, typeId, roomId) },
            onDismiss = viewModel::dismissDialog,
        )
    }
}

@Composable
private fun RoomsContent(
    state: RoomsUiState,
    widthClass: WindowWidthSizeClass,
    onSelectRoom: (String) -> Unit,
    onRenameRoom: (Room) -> Unit,
    onDeleteRoom: (Room) -> Unit,
    onOpenDevice: (String) -> Unit,
    onToggleDevice: (Device) -> Unit,
    onAddDevice: (Room) -> Unit,
) {
    // Always single-pane vertical scroll, regardless of width. Phones use a
    // simple list; landscape / tablet uses the adaptive grid but with a width
    // cap so the content doesn't stretch edge-to-edge. The previous two-pane
    // layout (grid + fixed 360dp detail panel) was removed for consistency
    // with the dashboard.
    if (widthClass == WindowWidthSizeClass.COMPACT) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.rooms, key = { it.id }) { room ->
                RoomCard(
                    room = room,
                    deviceCount = devicesInRoom(state.devices, room.id).size,
                    selected = false,
                    onClick = { onSelectRoom(room.id) },
                    onRename = { onRenameRoom(room) },
                    onDelete = { onDeleteRoom(room) },
                )
            }
        }
    } else {
        // Capeamos el ancho a 880dp para que en landscape phone / tablet las
        // cards no se estiren edge-to-edge — quedan centradas con margen
        // a los costados.
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            RoomsGrid(
                rooms = state.rooms,
                devices = state.devices,
                selectedRoomId = null,
                onSelectRoom = onSelectRoom,
                onRenameRoom = onRenameRoom,
                onDeleteRoom = onDeleteRoom,
                modifier = Modifier
                    .widthIn(max = 880.dp)
                    .fillMaxSize(),
                horizontalPadding = 32.dp,
            )
        }
    }
}

@Composable
private fun RoomsGrid(
    rooms: List<Room>,
    devices: List<Device>,
    selectedRoomId: String?,
    onSelectRoom: (String) -> Unit,
    onRenameRoom: (Room) -> Unit,
    onDeleteRoom: (Room) -> Unit,
    modifier: Modifier = Modifier,
    // Horizontal padding extra para el caso non-COMPACT: el widthIn cap
    // recorta en tablets, pero en landscape phone (viewport < 880dp) hace
    // falta padding lateral explicito para que las cards no toquen el borde.
    horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp,
) {
    LazyVerticalGrid(
        // 200dp lets EXPANDED layouts squeeze in 3 columns alongside the 360dp
        // detail pane; MEDIUM still resolves to 2 columns at ~640-720dp.
        columns = GridCells.Adaptive(minSize = 200.dp),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        gridItems(rooms, key = { it.id }) { room ->
            RoomCard(
                room = room,
                deviceCount = devicesInRoom(devices, room.id).size,
                selected = room.id == selectedRoomId,
                onClick = { onSelectRoom(room.id) },
                onRename = { onRenameRoom(room) },
                onDelete = { onDeleteRoom(room) },
            )
        }
    }
}

@Composable
private fun RoomCard(
    room: Room,
    deviceCount: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.MeetingRoom,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = pluralStringResource(R.plurals.rooms_device_count, deviceCount, deviceCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.cd_more_options),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rooms_action_rename)) },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete)) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun RoomNameDialog(
    title: String,
    confirmLabel: String,
    name: String,
    nameError: String?,
    apiError: String?,
    saving: Boolean,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ErrorBanner(apiError)
                LuminaTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = stringResource(R.string.rooms_name_label),
                    required = true,
                    error = nameError,
                    enabled = !saving,
                )
            }
        },
        confirmButton = {
            LoadingButton(text = confirmLabel, onClick = onSubmit, loading = saving)
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
fun AddDeviceDialog(
    unassigned: List<Device>,
    types: Map<String, DeviceType>,
    pendingDeviceIds: Set<String>,
    onAssign: (Device) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rooms_add_device)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (unassigned.isEmpty()) {
                    Text(stringResource(R.string.rooms_no_unassigned))
                } else {
                    Text(
                        text = stringResource(R.string.rooms_unassigned_section),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(unassigned, key = { it.id }) { device ->
                            UnassignedDeviceRow(
                                device = device,
                                types = types,
                                pending = device.id in pendingDeviceIds,
                                onAssign = { onAssign(device) },
                            )
                        }
                    }
                }
                TextButton(
                    onClick = onCreateNew,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.rooms_create_new_device))
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
private fun UnassignedDeviceRow(
    device: Device,
    types: Map<String, DeviceType>,
    pending: Boolean,
    onAssign: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
            val typeName = deviceTypeNameRes(device.type.id)?.let { stringResource(it) }
                ?: types[device.type.id]?.name.orEmpty()
            if (typeName.isNotBlank()) {
                Text(
                    text = typeName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onAssign, enabled = !pending) {
            Icon(
                imageVector = Icons.Outlined.AddCircleOutline,
                contentDescription = stringResource(R.string.rooms_cd_assign_device),
            )
        }
    }
}
