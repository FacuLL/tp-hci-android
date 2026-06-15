package tp3.grupo1.hci.itba.edu.ar.ui.screens.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import tp3.grupo1.hci.itba.edu.ar.domain.DeviceTypeIds
import tp3.grupo1.hci.itba.edu.ar.domain.PowerAtom
import tp3.grupo1.hci.itba.edu.ar.domain.deviceControls
import tp3.grupo1.hci.itba.edu.ar.domain.deviceStateText
import tp3.grupo1.hci.itba.edu.ar.domain.deviceTypeColor
import tp3.grupo1.hci.itba.edu.ar.domain.deviceTypeIcon
import tp3.grupo1.hci.itba.edu.ar.domain.deviceTypeNameRes
import tp3.grupo1.hci.itba.edu.ar.domain.isDeviceActive
import tp3.grupo1.hci.itba.edu.ar.ui.components.CenteredLoading
import tp3.grupo1.hci.itba.edu.ar.ui.components.ConfirmDialog
import tp3.grupo1.hci.itba.edu.ar.ui.components.EmptyState
import tp3.grupo1.hci.itba.edu.ar.ui.components.ErrorBanner
import tp3.grupo1.hci.itba.edu.ar.ui.components.ProfileAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    onOpenDevice: (String) -> Unit,
    onOpenSettings: () -> Unit,
    initialFilter: String? = null,
    onFilterConsumed: () -> Unit = {},
) {
    val viewModel: DevicesViewModel = viewModel(factory = DevicesViewModel.Factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState by viewModel.listState.collectAsStateWithLifecycle()
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val deviceTypes by viewModel.deviceTypes.collectAsStateWithLifecycle()

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    // Siempre single-pane: el layout de dos paneles se saco por consistencia con dashboard y rooms.
    val twoPane = false

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var selectedDeviceId by rememberSaveable { mutableStateOf<String?>(null) }
    // Solo los ids sobreviven la rotacion; el Device se resuelve al renderizar.
    var deviceToAssignId by rememberSaveable { mutableStateOf<String?>(null) }
    var deviceToDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val deviceToAssign = listState.devices.firstOrNull { it.id == deviceToAssignId }
    val deviceToDelete = listState.devices.firstOrNull { it.id == deviceToDeleteId }

    LaunchedEffect(uiState.snackbarRes) {
        uiState.snackbarRes?.let { messageRes ->
            snackbarHostState.showSnackbar(context.getString(messageRes))
            viewModel.snackbarShown()
        }
    }

    LaunchedEffect(initialFilter) {
        if (initialFilter != null) {
            viewModel.applyInitialFilter(initialFilter)
            onFilterConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_devices)) },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.devices_cd_add))
                    }
                    ProfileAvatar(onClick = onOpenSettings)
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (uiState.loading) {
            CenteredLoading(Modifier.padding(innerPadding))
        } else {
            val openDevice: (Device) -> Unit = { device ->
                if (twoPane) selectedDeviceId = device.id else onOpenDevice(device.id)
            }
            if (twoPane) {
                Row(
                    Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    PullToRefreshBox(
                        isRefreshing = uiState.refreshing,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight(),
                    ) {
                        DevicesListPane(
                            uiState = uiState,
                            listState = listState,
                            rooms = rooms,
                            deviceTypes = deviceTypes,
                            selectedDeviceId = selectedDeviceId,
                            onQueryChange = viewModel::onQueryChange,
                            onTypeSelected = viewModel::onTypeSelected,
                            onLockFilterToggle = viewModel::onLockFilterToggle,
                            onDeviceClick = openDevice,
                            onToggle = viewModel::toggleDevice,
                            onAssignRoom = { deviceToAssignId = it.id },
                            onRemoveFromRoom = viewModel::removeFromRoom,
                            onDelete = { deviceToDeleteId = it.id },
                            onAddDevice = { showCreateDialog = true },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    DeviceDetailPane(
                        deviceId = selectedDeviceId,
                        onDeleted = { selectedDeviceId = null },
                        modifier = Modifier
                            .weight(3f)
                            .fillMaxHeight(),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    PullToRefreshBox(
                        isRefreshing = uiState.refreshing,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .fillMaxSize(),
                    ) {
                        DevicesListPane(
                            uiState = uiState,
                            listState = listState,
                            rooms = rooms,
                            deviceTypes = deviceTypes,
                            selectedDeviceId = null,
                            onQueryChange = viewModel::onQueryChange,
                            onTypeSelected = viewModel::onTypeSelected,
                            onLockFilterToggle = viewModel::onLockFilterToggle,
                            onDeviceClick = openDevice,
                            onToggle = viewModel::toggleDevice,
                            onAssignRoom = { deviceToAssignId = it.id },
                            onRemoveFromRoom = viewModel::removeFromRoom,
                            onDelete = { deviceToDeleteId = it.id },
                            onAddDevice = { showCreateDialog = true },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateDeviceDialog(
            rooms = rooms,
            creating = uiState.creating,
            apiErrorRes = uiState.createErrorRes,
            onDismiss = {
                showCreateDialog = false
                viewModel.clearCreateError()
            },
            onCreate = { name, typeId, roomId ->
                viewModel.createDevice(name, typeId, roomId) { showCreateDialog = false }
            },
        )
    }

    deviceToAssign?.let { device ->
        AssignRoomDialog(
            deviceName = device.name,
            currentRoomId = device.room?.id,
            rooms = rooms,
            busy = uiState.assigning,
            apiErrorRes = uiState.assignErrorRes,
            onDismiss = {
                deviceToAssignId = null
                viewModel.clearAssignError()
            },
            onConfirm = { roomId ->
                viewModel.assignDevice(device, roomId) { deviceToAssignId = null }
            },
        )
    }

    deviceToDelete?.let { device ->
        ConfirmDialog(
            title = stringResource(R.string.devices_delete_title),
            text = stringResource(R.string.devices_delete_message, device.name),
            onConfirm = {
                deviceToDeleteId = null
                if (selectedDeviceId == device.id) selectedDeviceId = null
                viewModel.deleteDevice(device)
            },
            onDismiss = { deviceToDeleteId = null },
        )
    }
}

@Composable
private fun DevicesListPane(
    uiState: DevicesUiState,
    listState: DeviceListState,
    rooms: List<Room>,
    deviceTypes: Map<String, DeviceType>,
    selectedDeviceId: String?,
    onQueryChange: (String) -> Unit,
    onTypeSelected: (String?) -> Unit,
    onLockFilterToggle: () -> Unit,
    onDeviceClick: (Device) -> Unit,
    onToggle: (Device) -> Unit,
    onAssignRoom: (Device) -> Unit,
    onRemoveFromRoom: (Device) -> Unit,
    onDelete: (Device) -> Unit,
    onAddDevice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // En landscape phone (alto < 480dp) chips + busqueda comen el viewport, asi que compactamos a una sola fila.
    val configuration = LocalConfiguration.current
    val isCompactLandscape =
        configuration.screenWidthDp > configuration.screenHeightDp &&
        configuration.screenHeightDp < 480
    val verticalSpacing = if (isCompactLandscape) 8.dp else 12.dp
    Column(
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
    ) {
        uiState.loadErrorRes?.let { ErrorBanner(stringResource(it)) }
        if (isCompactLandscape) {
            CompactSearchFilterRow(
                query = uiState.query,
                selectedTypeId = uiState.selectedTypeId,
                lockOnly = uiState.lockOnly,
                onQueryChange = onQueryChange,
                onTypeSelected = onTypeSelected,
                onLockFilterToggle = onLockFilterToggle,
            )
        } else {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.devices_search_placeholder)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = uiState.selectedTypeId == null && !uiState.lockOnly,
                    onClick = { onTypeSelected(null) },
                    label = { Text(stringResource(R.string.devices_filter_all)) },
                )
                FilterChip(
                    selected = uiState.lockOnly,
                    onClick = { onLockFilterToggle() },
                    label = { Text(stringResource(R.string.dashboard_locks_title)) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    },
                )
                DeviceTypeIds.CREATABLE.forEach { typeId ->
                    val nameRes = deviceTypeNameRes(typeId) ?: return@forEach
                    FilterChip(
                        selected = uiState.selectedTypeId == typeId,
                        onClick = { onTypeSelected(if (uiState.selectedTypeId == typeId) null else typeId) },
                        label = { Text(stringResource(nameRes)) },
                        leadingIcon = {
                            Icon(
                                deviceTypeIcon(typeId),
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        },
                    )
                }
            }
        }
        when {
            listState.totalCount == 0 -> EmptyState(
                icon = Icons.Outlined.DevicesOther,
                title = stringResource(R.string.devices_empty_title),
                subtitle = stringResource(R.string.devices_empty_subtitle),
                actionLabel = stringResource(R.string.devices_empty_action),
                onAction = onAddDevice,
            )
            listState.devices.isEmpty() -> EmptyState(
                icon = Icons.Outlined.SearchOff,
                title = stringResource(R.string.devices_empty_filtered_title),
                subtitle = stringResource(R.string.devices_empty_filtered_subtitle),
            )
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                // Padding extra para que el ultimo device no quede tapado por el bottom nav.
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                items(listState.devices, key = { it.id }) { device ->
                    DeviceListItem(
                        device = device,
                        deviceType = deviceTypes[device.type.id],
                        rooms = rooms,
                        selected = device.id == selectedDeviceId,
                        onClick = { onDeviceClick(device) },
                        onToggle = { onToggle(device) },
                        onAssignRoom = { onAssignRoom(device) },
                        onRemoveFromRoom = { onRemoveFromRoom(device) },
                        onDelete = { onDelete(device) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceListItem(
    device: Device,
    deviceType: DeviceType?,
    rooms: List<Room>,
    selected: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onAssignRoom: () -> Unit,
    onRemoveFromRoom: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val accent = deviceTypeColor(device.type.id)
    val powerAtom = remember(device, deviceType) {
        deviceType?.let { deviceControls(it, device).filterIsInstance<PowerAtom>().firstOrNull() }
    }
    val typeLabel = deviceTypeNameRes(device.type.id)?.let { stringResource(it) }
        ?: deviceType?.name.orEmpty()
    val roomLabel = device.room?.let { roomRef ->
        rooms.firstOrNull { it.id == roomRef.id }?.name ?: roomRef.name
    } ?: stringResource(R.string.devices_no_room)
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(deviceTypeIcon(device.type.id), contentDescription = null, tint = accent)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.devices_item_subtitle, typeLabel, roomLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    deviceStateText(context, device),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDeviceActive(device)) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (powerAtom != null) {
                // No se puede abrir/cerrar mientras esta bloqueado, ni bloquear mientras esta abierto.
                val togglesOpening = powerAtom.onAction == "open" || powerAtom.offAction == "open"
                val blocked = (togglesOpening && device.state.lock == "locked") ||
                    (powerAtom.onAction == "lock" && device.state.status == "opened")
                Switch(
                    checked = powerAtom.active,
                    onCheckedChange = { onToggle() },
                    enabled = !blocked,
                    colors = SwitchDefaults.colors(
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.cd_more_options))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.devices_menu_view_detail)) },
                        onClick = {
                            menuOpen = false
                            onClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.devices_menu_assign_room)) },
                        onClick = {
                            menuOpen = false
                            onAssignRoom()
                        },
                    )
                    if (device.room != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.devices_menu_remove_room)) },
                            onClick = {
                                menuOpen = false
                                onRemoveFromRoom()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                        },
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
private fun DeviceDetailPane(
    deviceId: String?,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (deviceId == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Outlined.Devices,
                title = stringResource(R.string.devices_select_prompt),
                subtitle = stringResource(R.string.devices_select_prompt_subtitle),
            )
        }
    } else {
        key(deviceId) {
            DeviceDetailContent(deviceId = deviceId, modifier = modifier, onDeleted = onDeleted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactSearchFilterRow(
    query: String,
    selectedTypeId: String?,
    lockOnly: Boolean,
    onQueryChange: (String) -> Unit,
    onTypeSelected: (String?) -> Unit,
    onLockFilterToggle: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val activeFilterLabel = when {
        lockOnly -> stringResource(R.string.dashboard_locks_title)
        selectedTypeId != null -> deviceTypeNameRes(selectedTypeId)?.let { stringResource(it) }
            ?: stringResource(R.string.devices_filter_all)
        else -> stringResource(R.string.devices_filter_all)
    }
    val filterActive = lockOnly || selectedTypeId != null
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.devices_search_placeholder)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
        )
        Box {
            FilterChip(
                selected = filterActive,
                onClick = { menuOpen = true },
                label = {
                    Text(
                        text = activeFilterLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 110.dp),
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
            )
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.devices_filter_all)) },
                    onClick = {
                        menuOpen = false
                        // Reset completo: tipo null y apagar lockOnly si estaba prendido.
                        if (lockOnly) onLockFilterToggle()
                        onTypeSelected(null)
                    },
                    trailingIcon = if (!filterActive) {
                        { Icon(Icons.Outlined.Check, contentDescription = null) }
                    } else null,
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.dashboard_locks_title)) },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        if (!lockOnly) onLockFilterToggle()
                    },
                    trailingIcon = if (lockOnly) {
                        { Icon(Icons.Outlined.Check, contentDescription = null) }
                    } else null,
                )
                DeviceTypeIds.CREATABLE.forEach { typeId ->
                    val nameRes = deviceTypeNameRes(typeId) ?: return@forEach
                    DropdownMenuItem(
                        text = { Text(stringResource(nameRes)) },
                        leadingIcon = {
                            Icon(deviceTypeIcon(typeId), contentDescription = null)
                        },
                        onClick = {
                            menuOpen = false
                            onTypeSelected(if (selectedTypeId == typeId) null else typeId)
                        },
                        trailingIcon = if (selectedTypeId == typeId) {
                            { Icon(Icons.Outlined.Check, contentDescription = null) }
                        } else null,
                    )
                }
            }
        }
    }
}
