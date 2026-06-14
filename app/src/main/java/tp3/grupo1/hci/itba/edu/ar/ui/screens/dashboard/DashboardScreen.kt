package tp3.grupo1.hci.itba.edu.ar.ui.screens.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import tp3.grupo1.hci.itba.edu.ar.ui.components.FloatingTopBar
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import tp3.grupo1.hci.itba.edu.ar.domain.DeviceTypeIds
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
import tp3.grupo1.hci.itba.edu.ar.ui.components.ProfileAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenDevice: (String) -> Unit,
    onOpenHomes: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRoom: (String) -> Unit = {},
    onOpenRooms: () -> Unit = {},
    onOpenDevices: () -> Unit = {},
    onOpenLocks: () -> Unit = onOpenDevices,
    onOpenAlarms: () -> Unit = onOpenDevices,
    onOpenNotifications: () -> Unit = {},
) {
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val toggleErrorRes by viewModel.toggleErrorRes.collectAsStateWithLifecycle()
    val unreadNotifications by viewModel.unreadNotifications.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    toggleErrorRes?.let { messageRes ->
        val message = stringResource(messageRes)
        LaunchedEffect(messageRes) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearToggleError()
        }
    }

    Scaffold(
        // El outer NavigationSuiteScaffold ya consumio los insets del sistema.
        // Sin esto el Scaffold interno re-aplica safeDrawing y deja
        // innerPadding.bottom = 0, lo que forzaba contentPadding(bottom=96.dp).
        contentWindowInsets = WindowInsets(0),
        topBar = {
            DashboardTopBar(
                currentHome = state.currentHome,
                homes = state.homes,
                userName = state.userName,
                unreadNotifications = unreadNotifications,
                onSelectHome = viewModel::selectHome,
                onOpenHomes = onOpenHomes,
                onOpenNotifications = onOpenNotifications,
                onOpenProfile = onOpenSettings,
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
                errorRes != null -> DashboardError(errorRes, onRetry = { viewModel.refresh() })
                state.loaded && state.homes.isEmpty() -> WelcomeEmptyState(onCreateHome = onOpenHomes)
                state.currentHome == null -> CenteredLoading()
                else -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.refresh(manual = true) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    DashboardContent(
                        state = state,
                        onOpenDevice = onOpenDevice,
                        onTogglePower = { device, atom -> viewModel.togglePower(device.id, atom) },
                        onOpenRoom = onOpenRoom,
                        onOpenRooms = onOpenRooms,
                        onOpenLocks = onOpenLocks,
                        onOpenAlarms = onOpenAlarms,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    currentHome: Home?,
    homes: List<Home>,
    userName: String?,
    unreadNotifications: Int,
    onSelectHome: (String) -> Unit,
    onOpenHomes: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    FloatingTopBar(
        title = {
            if (currentHome != null) {
                Box {
                    Surface(
                        onClick = { menuExpanded = true },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 1.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.HomeWork,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = currentHome.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.widthIn(max = 140.dp),
                            )
                            Icon(
                                imageVector = Icons.Outlined.ArrowDropDown,
                                contentDescription = stringResource(R.string.dashboard_cd_change_home),
                                modifier = Modifier.size(18.dp),
                            )
                        }
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
        },
        actions = {
            IconButton(onClick = onOpenNotifications) {
                BadgedBox(
                    badge = {
                        if (unreadNotifications > 0) {
                            Badge {
                                Text(if (unreadNotifications > 99) "99+" else unreadNotifications.toString())
                            }
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = stringResource(R.string.title_notifications),
                    )
                }
            }
            ProfileAvatar(name = userName, onClick = onOpenProfile)
        },
    )
}

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onOpenDevice: (String) -> Unit,
    onTogglePower: (Device, PowerAtom) -> Unit,
    onOpenRoom: (String) -> Unit,
    onOpenRooms: () -> Unit,
    onOpenLocks: () -> Unit,
    onOpenAlarms: () -> Unit,
) {
    CompactDashboard(state, onOpenDevice, onTogglePower, onOpenRoom, onOpenRooms, onOpenLocks, onOpenAlarms)
}

@Composable
private fun CompactDashboard(
    state: DashboardUiState,
    onOpenDevice: (String) -> Unit,
    onTogglePower: (Device, PowerAtom) -> Unit,
    onOpenRoom: (String) -> Unit,
    onOpenRooms: () -> Unit,
    onOpenLocks: () -> Unit,
    onOpenAlarms: () -> Unit,
) {
    val lockDevices = state.devices.filter { it.state.lock != null }
    val alarmDevices = state.devices.filter { it.type.id == DeviceTypeIds.ALARM }
    // En landscape phone el alto disponible es minimo: ocultamos el saludo y
    // achicamos las tiles para que al menos una room card entre sin scroll.
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val isLandscape = screenWidthDp > screenHeightDp
    val isCompactLandscape = isLandscape && screenHeightDp < 480
    val roomCardWidth = when {
        screenWidthDp >= 840 -> (screenWidthDp * 0.32f).dp
        isLandscape          -> (screenWidthDp * 0.48f).dp
        else                 -> (screenWidthDp * 0.88f).dp
    }
    val deviceTileHeight = if (isCompactLandscape) 56.dp else 72.dp
    val verticalSpacing = if (isCompactLandscape) 12.dp else 20.dp
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
    LazyColumn(
        modifier = Modifier
            .widthIn(max = 760.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
    ) {
        if (!isCompactLandscape) {
            item { GreetingHeader(userName = state.userName) }
        }

        item {
            SectionTitle(
                text = stringResource(R.string.nav_rooms),
                onClick = onOpenRooms,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        if (state.rooms.isEmpty()) {
            item {
                SectionHint(
                    text = stringResource(R.string.dashboard_no_rooms_hint),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        } else {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.rooms, key = { it.id }) { room ->
                        RoomCard(
                            room = room,
                            roomDevices = devicesInRoom(state.devices, room.id),
                            deviceTypes = state.deviceTypes,
                            deviceTileHeight = deviceTileHeight,
                            compact = isCompactLandscape,
                            onOpenDevice = onOpenDevice,
                            onTogglePower = onTogglePower,
                            onOpenDetail = { onOpenRoom(room.id) },
                            modifier = Modifier.width(roomCardWidth),
                        )
                    }
                }
            }
        }

        item {
            SectionTitle(
                text = stringResource(R.string.dashboard_locks_title),
                onClick = onOpenLocks,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        if (lockDevices.isEmpty()) {
            item {
                SectionHint(
                    text = stringResource(R.string.dashboard_locks_empty),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        } else {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(lockDevices, key = { it.id }) { device ->
                        LockChipCard(
                            device = device,
                            onOpen = { onOpenDevice(device.id) },
                            modifier = Modifier.width(200.dp),
                        )
                    }
                }
            }
        }

        item {
            SectionTitle(
                text = stringResource(R.string.dashboard_alarms_title),
                onClick = onOpenAlarms,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        if (alarmDevices.isEmpty()) {
            item {
                SectionHint(
                    text = stringResource(R.string.dashboard_alarms_empty),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        } else {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(alarmDevices, key = { it.id }) { device ->
                        AlarmChipCard(
                            device = device,
                            onOpen = { onOpenDevice(device.id) },
                            modifier = Modifier.width(200.dp),
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun GreetingHeader(userName: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = if (userName.isNullOrBlank()) {
                stringResource(R.string.dashboard_greeting_generic)
            } else {
                stringResource(R.string.dashboard_greeting_with_name, userName.trim().split(' ').first())
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.dashboard_status_ok),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TwoPaneDashboard(
    state: DashboardUiState,
    onOpenDevice: (String) -> Unit,
    onTogglePower: (Device, PowerAtom) -> Unit,
    onOpenRoom: (String) -> Unit,
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
                        deviceTypes = state.deviceTypes,
                        onOpenDevice = onOpenDevice,
                        onTogglePower = onTogglePower,
                        onOpenDetail = { onOpenRoom(room.id) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        CategoryPager(
            state = state,
            onOpenDevice = onOpenDevice,
            onTogglePower = onTogglePower,
            modifier = Modifier
                .weight(1.4f)
                .fillMaxSize(),
        )
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
private fun RoomCard(
    room: Room,
    roomDevices: List<Device>,
    deviceTypes: Map<String, DeviceType>,
    onOpenDevice: (String) -> Unit,
    onTogglePower: (Device, PowerAtom) -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
    deviceTileHeight: androidx.compose.ui.unit.Dp = 72.dp,
    compact: Boolean = false,
) {
    val anyActive = roomDevices.any(::isDeviceActive)
    val pad = if (compact) 12.dp else 16.dp
    val gap = if (compact) 8.dp else 12.dp
    OutlinedCard(
        modifier = modifier,
        onClick = onOpenDetail,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (anyActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(pad),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            Text(
                text = room.name.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.dashboard_room_devices_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (roomDevices.isEmpty()) {
                // Reservamos el alto de las tiles para que las rooms vacias queden igual de altas.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(deviceTileHeight),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    SectionHint(text = stringResource(R.string.dashboard_room_empty))
                }
            } else {
                DeviceToggleRow(
                    devices = roomDevices.take(4),
                    deviceTypes = deviceTypes,
                    tileHeight = deviceTileHeight,
                    onOpenDevice = onOpenDevice,
                    onTogglePower = onTogglePower,
                )
            }
            // En compact toda la card es clickeable, asi que sacamos el boton "Ver detalle" para ahorrar alto.
            if (!compact) {
                TextButton(
                    onClick = onOpenDetail,
                    modifier = Modifier.align(Alignment.End),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_room_view_detail),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceToggleRow(
    devices: List<Device>,
    deviceTypes: Map<String, DeviceType>,
    onOpenDevice: (String) -> Unit,
    onTogglePower: (Device, PowerAtom) -> Unit,
    tileHeight: androidx.compose.ui.unit.Dp = 72.dp,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        devices.forEach { device ->
            val powerAtom = deviceTypes[device.type.id]?.let { type ->
                deviceControls(type, device).filterIsInstance<PowerAtom>().firstOrNull()
            }
            DeviceToggleTile(
                device = device,
                powerAtom = powerAtom,
                tileHeight = tileHeight,
                onToggle = { atom -> onTogglePower(device, atom) },
                onOpen = { onOpenDevice(device.id) },
                modifier = Modifier.weight(1f),
            )
        }
        // Rellenamos hasta 4 columnas para que las cards mantengan su ancho.
        repeat(4 - devices.size) {
            Spacer(Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceToggleTile(
    device: Device,
    powerAtom: PowerAtom?,
    onToggle: (PowerAtom) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    tileHeight: androidx.compose.ui.unit.Dp = 72.dp,
) {
    val active = powerAtom?.active == true
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .height(tileHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) primary else primary.copy(alpha = 0.12f))
            // Tap togglea el power (o abre el detalle si no hay control); long-press siempre abre el detalle.
            .combinedClickable(
                onClick = { if (powerAtom != null) onToggle(powerAtom) else onOpen() },
                onLongClick = onOpen,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = deviceTypeIcon(device.type.id),
            contentDescription = device.name,
            tint = if (active) MaterialTheme.colorScheme.onPrimary else primary,
            modifier = Modifier.size(36.dp),
        )
    }
}

@Composable
private fun AlarmChipCard(
    device: Device,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val armed = device.state.status == "armedStay" || device.state.status == "armedAway"
    val statusRes = when (device.state.status) {
        "armedStay" -> R.string.device_state_armed_stay
        "armedAway" -> R.string.device_state_armed_away
        else -> R.string.device_state_disarmed
    }
    OutlinedCard(
        modifier = modifier,
        onClick = onOpen,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (armed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = if (armed) 0.20f else 0.12f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = device.name.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            device.room?.name?.takeIf { it.isNotBlank() }?.let { roomName ->
                Text(
                    text = roomName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusChip(textRes = statusRes, highlighted = armed)
            device.state.batteryLevel?.let { battery ->
                Text(
                    text = stringResource(R.string.dashboard_chip_battery, battery),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (battery <= 20) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun LockChipCard(
    device: Device,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locked = device.state.lock == "locked"
    val opened = device.state.status == "opened"
    val closed = device.state.status == "closed"
    OutlinedCard(
        modifier = modifier,
        onClick = onOpen,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (locked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = if (locked) 0.20f else 0.12f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (locked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = device.name.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            device.room?.name?.takeIf { it.isNotBlank() }?.let { roomName ->
                Text(
                    text = roomName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // Si el device solo expone uno de los dos estados, mostramos solo ese chip.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (opened || closed) {
                    StatusChip(
                        textRes = if (opened) R.string.device_state_opened else R.string.device_state_closed,
                        highlighted = opened,
                    )
                }
                StatusChip(
                    textRes = if (locked) {
                        R.string.device_state_locked
                    } else {
                        R.string.device_state_unlocked
                    },
                    highlighted = locked,
                )
            }
            device.state.batteryLevel?.let { battery ->
                Text(
                    text = stringResource(R.string.dashboard_chip_battery, battery),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (battery <= 20) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
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
                // No se puede abrir/cerrar mientras esta bloqueado, ni bloquear mientras esta abierto.
                val togglesOpening = powerAtom.onAction == "open" || powerAtom.offAction == "open"
                val blocked = (togglesOpening && device.state.lock == "locked") ||
                    (powerAtom.onAction == "lock" && device.state.status == "opened")
                Switch(
                    checked = powerAtom.active,
                    enabled = !blocked,
                    onCheckedChange = { onTogglePower(powerAtom) },
                    colors = SwitchDefaults.colors(
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CategoryPager(
    state: DashboardUiState,
    onOpenDevice: (String) -> Unit,
    onTogglePower: (Device, PowerAtom) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lockDevices = state.devices.filter { it.state.lock != null }
    val alarmDevices = state.devices.filter { it.type.id == DeviceTypeIds.ALARM }
    val pages = listOf(
        CategoryPage.Locks(lockDevices),
        CategoryPage.Alarms(alarmDevices),
        CategoryPage.Devices(state.devices),
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 24.dp),
        ) { page ->
            CategoryCard(
                page = pages[page],
                deviceTypes = state.deviceTypes,
                onOpenDevice = onOpenDevice,
                onTogglePower = onTogglePower,
            )
        }
        PagerIndicator(
            pageCount = pages.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}

private sealed interface CategoryPage {
    @get:androidx.annotation.StringRes
    val titleRes: Int

    @get:androidx.annotation.StringRes
    val emptyRes: Int

    val devices: List<Device>

    data class Locks(override val devices: List<Device>) : CategoryPage {
        override val titleRes get() = R.string.dashboard_locks_title
        override val emptyRes get() = R.string.dashboard_locks_empty
    }

    data class Alarms(override val devices: List<Device>) : CategoryPage {
        override val titleRes get() = R.string.dashboard_alarms_title
        override val emptyRes get() = R.string.dashboard_alarms_empty
    }

    data class Devices(override val devices: List<Device>) : CategoryPage {
        override val titleRes get() = R.string.nav_devices
        override val emptyRes get() = R.string.dashboard_no_devices_title
    }
}

@Composable
private fun CategoryCard(
    page: CategoryPage,
    deviceTypes: Map<String, DeviceType>,
    onOpenDevice: (String) -> Unit,
    onTogglePower: (Device, PowerAtom) -> Unit,
) {
    Card(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(page.titleRes),
                style = MaterialTheme.typography.titleMedium,
            )
            if (page.devices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(page.emptyRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(page.devices, key = { it.id }) { device ->
                        when (page) {
                            is CategoryPage.Locks -> LockRow(
                                device = device,
                                onOpen = { onOpenDevice(device.id) },
                            )
                            is CategoryPage.Alarms -> AlarmRow(
                                device = device,
                                onOpen = { onOpenDevice(device.id) },
                            )
                            is CategoryPage.Devices -> DeviceRow(
                                device = device,
                                deviceType = deviceTypes[device.type.id],
                                onOpen = { onOpenDevice(device.id) },
                                onTogglePower = { atom -> onTogglePower(device, atom) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PagerIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (selected) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

@Composable
private fun LockRow(device: Device, onOpen: () -> Unit) {
    val locked = device.state.lock == "locked"
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryIcon(
                icon = if (locked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                active = locked,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (device.state.status == "opened" || device.state.status == "closed") {
                        StatusChip(
                            textRes = if (device.state.status == "opened") {
                                R.string.device_state_opened
                            } else {
                                R.string.device_state_closed
                            },
                            highlighted = device.state.status == "opened",
                        )
                    }
                    StatusChip(
                        textRes = if (locked) {
                            R.string.device_state_locked
                        } else {
                            R.string.device_state_unlocked
                        },
                        highlighted = locked,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmRow(device: Device, onOpen: () -> Unit) {
    val armed = device.state.status == "armedStay" || device.state.status == "armedAway"
    val statusRes = when (device.state.status) {
        "armedStay" -> R.string.device_state_armed_stay
        "armedAway" -> R.string.device_state_armed_away
        else -> R.string.device_state_disarmed
    }
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryIcon(icon = Icons.Outlined.NotificationsActive, active = armed)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusChip(textRes = statusRes, highlighted = armed)
            }
        }
    }
}

@Composable
private fun CategoryIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
) {
    val color = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(color.copy(alpha = if (active) 0.20f else 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color)
    }
}

@Composable
private fun StatusChip(@StringRes textRes: Int, highlighted: Boolean) {
    val (background, foreground) = if (highlighted) {
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(background)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
        )
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(top = 4.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    }
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (onClick != null) {
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
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
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
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
}
