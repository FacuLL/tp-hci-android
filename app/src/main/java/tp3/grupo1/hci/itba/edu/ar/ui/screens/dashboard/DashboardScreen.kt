package tp3.grupo1.hci.itba.edu.ar.ui.screens.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
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
import tp3.grupo1.hci.itba.edu.ar.data.model.Home
import tp3.grupo1.hci.itba.edu.ar.data.model.Room
import tp3.grupo1.hci.itba.edu.ar.domain.PowerAtom
import tp3.grupo1.hci.itba.edu.ar.domain.deviceControls
import tp3.grupo1.hci.itba.edu.ar.domain.deviceStateText
import tp3.grupo1.hci.itba.edu.ar.domain.deviceTypeColor
import tp3.grupo1.hci.itba.edu.ar.domain.deviceTypeIcon
import tp3.grupo1.hci.itba.edu.ar.domain.devicesInRoom
import tp3.grupo1.hci.itba.edu.ar.domain.isDeviceActive
import tp3.grupo1.hci.itba.edu.ar.ui.components.CenteredLoading
import tp3.grupo1.hci.itba.edu.ar.ui.components.EmptyState
import tp3.grupo1.hci.itba.edu.ar.ui.components.ErrorBanner

@Composable
fun DashboardScreen(
    onOpenDevice: (String) -> Unit,
    onOpenHomes: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val toggleErrorRes by viewModel.toggleErrorRes.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    toggleErrorRes?.let { messageRes ->
        val message = stringResource(messageRes)
        LaunchedEffect(messageRes) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearToggleError()
        }
    }

    Scaffold(
        topBar = {
            DashboardTopBar(
                currentHome = state.currentHome,
                homes = state.homes,
                onSelectHome = viewModel::selectHome,
                onOpenHomes = onOpenHomes,
                onOpenSettings = onOpenSettings,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val errorRes = state.errorRes
            when {
                state.loading -> CenteredLoading()
                errorRes != null -> DashboardError(errorRes, onRetry = viewModel::refresh)
                state.loaded && state.homes.isEmpty() -> WelcomeEmptyState(onCreateHome = onOpenHomes)
                state.currentHome == null -> CenteredLoading()
                else -> DashboardContent(
                    state = state,
                    onOpenDevice = onOpenDevice,
                    onTogglePower = { device, atom -> viewModel.togglePower(device.id, atom) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    currentHome: Home?,
    homes: List<Home>,
    onSelectHome: (String) -> Unit,
    onOpenHomes: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(stringResource(R.string.nav_dashboard)) },
        actions = {
            if (currentHome != null) {
                Box {
                    TextButton(onClick = { menuExpanded = true }) {
                        Text(
                            text = currentHome.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 180.dp),
                        )
                        Icon(
                            imageVector = Icons.Outlined.ArrowDropDown,
                            contentDescription = stringResource(R.string.dashboard_cd_change_home),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        homes.forEach { home ->
                            DropdownMenuItem(
                                text = { Text(home.name) },
                                onClick = {
                                    menuExpanded = false
                                    onSelectHome(home.id)
                                },
                                trailingIcon = if (home.id == currentHome.id) {
                                    {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = stringResource(R.string.dashboard_cd_current_home),
                                        )
                                    }
                                } else {
                                    null
                                },
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dashboard_manage_homes)) },
                            leadingIcon = { Icon(Icons.Outlined.HomeWork, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpenHomes()
                            },
                        )
                    }
                }
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.cd_open_settings),
                )
            }
        },
    )
}

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onOpenDevice: (String) -> Unit,
    onTogglePower: (Device, PowerAtom) -> Unit,
) {
    val widthClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    if (widthClass == WindowWidthSizeClass.COMPACT) {
        CompactDashboard(state, onOpenDevice, onTogglePower)
    } else {
        TwoPaneDashboard(state, onOpenDevice, onTogglePower)
    }
}

/** Phone portrait: a single scrolling column with rooms as a horizontal carousel. */
@Composable
private fun CompactDashboard(
    state: DashboardUiState,
    onOpenDevice: (String) -> Unit,
    onTogglePower: (Device, PowerAtom) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SummaryCard(state.devices) }
        item { SectionTitle(stringResource(R.string.nav_rooms)) }
        if (state.rooms.isEmpty()) {
            item { SectionHint(stringResource(R.string.dashboard_no_rooms_hint)) }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.rooms, key = { it.id }) { room ->
                        RoomCard(
                            room = room,
                            roomDevices = devicesInRoom(state.devices, room.id),
                            modifier = Modifier.width(170.dp),
                        )
                    }
                }
            }
        }
        item { SectionTitle(stringResource(R.string.nav_devices)) }
        if (state.devices.isEmpty()) {
            item { NoDevicesEmptyState() }
        } else {
            items(state.devices, key = { it.id }) { device ->
                DeviceRow(
                    device = device,
                    deviceType = state.deviceTypes[device.type.id],
                    onOpen = { onOpenDevice(device.id) },
                    onTogglePower = { atom -> onTogglePower(device, atom) },
                )
            }
        }
    }
}

/** Tablet / landscape: summary and rooms grid on the left, device list on the right. */
@Composable
private fun TwoPaneDashboard(
    state: DashboardUiState,
    onOpenDevice: (String) -> Unit,
    onTogglePower: (Device, PowerAtom) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { SummaryCard(state.devices) }
            item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle(stringResource(R.string.nav_rooms)) }
            if (state.rooms.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHint(stringResource(R.string.dashboard_no_rooms_hint))
                }
            } else {
                items(state.rooms, key = { it.id }) { room ->
                    RoomCard(
                        room = room,
                        roomDevices = devicesInRoom(state.devices, room.id),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1.4f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionTitle(stringResource(R.string.nav_devices)) }
            if (state.devices.isEmpty()) {
                item { NoDevicesEmptyState() }
            } else {
                items(state.devices, key = { it.id }) { device ->
                    DeviceRow(
                        device = device,
                        deviceType = state.deviceTypes[device.type.id],
                        onOpen = { onOpenDevice(device.id) },
                        onTogglePower = { atom -> onTogglePower(device, atom) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(devices: List<Device>, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Bolt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(
                    R.string.dashboard_summary_active,
                    devices.count(::isDeviceActive),
                    devices.size,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun RoomCard(room: Room, roomDevices: List<Device>, modifier: Modifier = Modifier) {
    val anyActive = roomDevices.any(::isDeviceActive)
    OutlinedCard(
        modifier = modifier,
        border = BorderStroke(
            width = 1.dp,
            color = if (anyActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = room.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.dashboard_room_device_count,
                    roomDevices.size,
                    roomDevices.size,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DeviceRow(
    device: Device,
    deviceType: DeviceType?,
    onOpen: () -> Unit,
    onTogglePower: (PowerAtom) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val accent = deviceTypeColor(device.type.id)
    val powerAtom = deviceType?.let { type ->
        deviceControls(type, device).filterIsInstance<PowerAtom>().firstOrNull()
    }
    Card(onClick = onOpen, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = deviceTypeIcon(device.type.id),
                    contentDescription = null,
                    tint = accent,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = deviceStateText(context, device),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (powerAtom != null) {
                Switch(
                    checked = powerAtom.active,
                    onCheckedChange = { onTogglePower(powerAtom) },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(top = 4.dp),
    )
}

@Composable
private fun SectionHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun NoDevicesEmptyState(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Outlined.DevicesOther,
        title = stringResource(R.string.dashboard_no_devices_title),
        subtitle = stringResource(R.string.dashboard_no_devices_subtitle),
        modifier = modifier,
    )
}

@Composable
private fun WelcomeEmptyState(onCreateHome: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            icon = Icons.Outlined.HomeWork,
            title = stringResource(R.string.dashboard_welcome_title),
            subtitle = stringResource(R.string.dashboard_welcome_subtitle),
            actionLabel = stringResource(R.string.dashboard_create_home),
            onAction = onCreateHome,
        )
    }
}

@Composable
private fun DashboardError(
    @StringRes messageRes: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ErrorBanner(stringResource(messageRes))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.action_retry))
        }
    }
}
