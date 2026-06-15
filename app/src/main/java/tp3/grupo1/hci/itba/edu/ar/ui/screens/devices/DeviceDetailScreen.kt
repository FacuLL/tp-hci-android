package tp3.grupo1.hci.itba.edu.ar.ui.screens.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import tp3.grupo1.hci.itba.edu.ar.ui.components.FloatingTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonElement
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.data.model.Device
import tp3.grupo1.hci.itba.edu.ar.data.model.Room
import tp3.grupo1.hci.itba.edu.ar.domain.DeviceTypeIds
import tp3.grupo1.hci.itba.edu.ar.domain.Validators
import tp3.grupo1.hci.itba.edu.ar.domain.deviceStateText
import tp3.grupo1.hci.itba.edu.ar.domain.deviceTypeColor
import tp3.grupo1.hci.itba.edu.ar.domain.deviceTypeIcon
import tp3.grupo1.hci.itba.edu.ar.domain.deviceTypeName
import tp3.grupo1.hci.itba.edu.ar.ui.components.CenteredLoading
import tp3.grupo1.hci.itba.edu.ar.ui.components.ConfirmDialog
import tp3.grupo1.hci.itba.edu.ar.ui.components.EmptyState
import tp3.grupo1.hci.itba.edu.ar.ui.components.ErrorBanner
import tp3.grupo1.hci.itba.edu.ar.ui.components.LoadingButton
import tp3.grupo1.hci.itba.edu.ar.ui.components.LuminaTextField
import tp3.grupo1.hci.itba.edu.ar.ui.screens.devices.controls.ChangeCodeDialog
import tp3.grupo1.hci.itba.edu.ar.ui.screens.devices.controls.ControlAtomCard
import tp3.grupo1.hci.itba.edu.ar.ui.screens.devices.controls.PlaylistDialog
import tp3.grupo1.hci.itba.edu.ar.ui.screens.devices.controls.SecurityCodeDialog

private const val MISSING_DEVICE_NOTICE_MILLIS = 1500L

private fun deviceDetailViewModelKey(deviceId: String) = "device_detail_$deviceId"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    deviceId: String,
    onNavigateUp: () -> Unit,
) {
    val viewModel: DeviceDetailViewModel = viewModel(
        key = deviceDetailViewModelKey(deviceId),
        factory = DeviceDetailViewModel.Factory(deviceId),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Device borrado (o id desconocido): el body muestra el aviso y despues volvemos atras.
    LaunchedEffect(state.loading, state.device == null) {
        if (!state.loading && state.device == null) {
            delay(MISSING_DEVICE_NOTICE_MILLIS)
            onNavigateUp()
        }
    }

    Scaffold(
        topBar = {
            FloatingTopBar(
                title = {
                    Text(
                        text = state.device?.name.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                        )
                    }
                },
                actions = {
                    if (state.device != null) {
                        DeviceManagementActions(onOpenDialog = viewModel::openDialog)
                    }
                },
            )
        },
    ) { innerPadding ->
        DeviceDetailBody(
            viewModel = viewModel,
            onDeleted = onNavigateUp,
            showManagementActions = false,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

@Composable
fun DeviceDetailContent(
    deviceId: String,
    modifier: Modifier = Modifier,
    onDeleted: () -> Unit = {},
) {
    val viewModel: DeviceDetailViewModel = viewModel(
        key = deviceDetailViewModelKey(deviceId),
        factory = DeviceDetailViewModel.Factory(deviceId),
    )
    DeviceDetailBody(
        viewModel = viewModel,
        onDeleted = onDeleted,
        showManagementActions = true,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceDetailBody(
    viewModel: DeviceDetailViewModel,
    onDeleted: () -> Unit,
    showManagementActions: Boolean,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recentColors by viewModel.recentColors.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { messageRes ->
            snackbarHostState.showSnackbar(context.getString(messageRes))
        }
    }

    Box(modifier = modifier) {
        val device = state.device
        when {
            device == null && state.loading -> CenteredLoading()
            device == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Outlined.DevicesOther,
                    title = stringResource(R.string.device_detail_missing_title),
                    subtitle = stringResource(R.string.device_detail_missing_subtitle),
                )
            }
            else -> PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                DeviceDetailLayout(
                    state = state,
                    device = device,
                    showManagementActions = showManagementActions,
                    recentColors = recentColors,
                    onExecute = viewModel::execute,
                    onPickCustomColor = viewModel::onPickCustomColor,
                    onDispense = viewModel::dispense,
                    onOpenDialog = viewModel::openDialog,
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    when (val dialog = state.dialog) {
        null -> Unit
        DeviceDetailDialog.Rename -> RenameDeviceDialog(
            currentName = state.device?.name.orEmpty(),
            busy = state.dialogBusy,
            apiErrorRes = state.dialogErrorRes,
            onConfirm = viewModel::rename,
            onDismiss = viewModel::closeDialog,
        )
        DeviceDetailDialog.AssignRoom -> AssignRoomDialog(
            deviceName = state.device?.name.orEmpty(),
            currentRoomId = state.device?.room?.id,
            rooms = state.rooms,
            busy = state.dialogBusy,
            apiErrorRes = state.dialogErrorRes,
            onDismiss = viewModel::closeDialog,
            onConfirm = viewModel::assignRoom,
        )
        DeviceDetailDialog.Delete -> ConfirmDialog(
            title = stringResource(R.string.device_detail_delete_title),
            text = stringResource(R.string.device_detail_delete_message, state.device?.name.orEmpty()),
            onConfirm = { viewModel.deleteDevice(onDeleted) },
            onDismiss = viewModel::closeDialog,
        )
        is DeviceDetailDialog.SecurityCode -> SecurityCodeDialog(
            titleRes = dialog.labelRes,
            busy = state.dialogBusy,
            apiErrorRes = state.dialogErrorRes,
            onConfirm = { code -> viewModel.executeWithCode(dialog.action, code) },
            onDismiss = viewModel::closeDialog,
        )
        DeviceDetailDialog.ChangeCode -> ChangeCodeDialog(
            busy = state.dialogBusy,
            apiErrorRes = state.dialogErrorRes,
            onConfirm = viewModel::changeSecurityCode,
            onDismiss = viewModel::closeDialog,
        )
        DeviceDetailDialog.Playlist -> PlaylistDialog(
            playlist = state.playlist,
            onDismiss = viewModel::closeDialog,
        )
    }
}

@Composable
private fun DeviceDetailLayout(
    state: DeviceDetailUiState,
    device: Device,
    showManagementActions: Boolean,
    recentColors: List<String>,
    onExecute: (String, List<JsonElement>) -> Unit,
    onPickCustomColor: (String, String) -> Unit,
    onDispense: (String, Int, String) -> Unit,
    onOpenDialog: (DeviceDetailDialog) -> Unit,
) {
    val accent = deviceTypeColor(device.type.id)

    // Las columnas dependen del ancho LOCAL del pane, no de la window class.
    BoxWithConstraints {
        val compact = maxWidth < 600.dp
        LazyVerticalGrid(
            columns = if (compact) GridCells.Fixed(1) else GridCells.Adaptive(minSize = 280.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                DeviceHeader(
                    device = device,
                    typeName = state.type?.name.orEmpty(),
                    rooms = state.rooms,
                    showManagementActions = showManagementActions,
                    onOpenDialog = onOpenDialog,
                )
            }
            items(state.atoms) { atom ->
                ControlAtomCard(
                    atom = atom,
                    accent = accent,
                    deviceStatus = device.state.status,
                    deviceLock = device.state.lock,
                    dispensing = state.dispensing,
                    rooms = state.rooms,
                    recentColors = recentColors,
                    onExecute = onExecute,
                    onPickCustomColor = onPickCustomColor,
                    onDispense = onDispense,
                    onOpenDialog = onOpenDialog,
                )
            }
        }
    }
}

@Composable
private fun DeviceHeader(
    device: Device,
    typeName: String,
    rooms: List<Room>,
    showManagementActions: Boolean,
    onOpenDialog: (DeviceDetailDialog) -> Unit,
) {
    val context = LocalContext.current
    val accent = deviceTypeColor(device.type.id)
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = deviceTypeIcon(device.type.id),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val roomName = device.room?.let { ref ->
                rooms.firstOrNull { it.id == ref.id }?.name ?: ref.name?.ifBlank { null }
            }
            Text(
                text = listOf(
                    deviceTypeName(context, device.type.id, typeName),
                    roomName ?: stringResource(R.string.device_detail_no_room),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // En alarmas el estado actual lo muestra la card de modos
            // (AlarmControl con grid 3-up: la celda activa ya indica
            // "Casa" / "Fuera de casa" / "Desarmada"). Mostrarlo aca
            // tambien es ruido visual triplicado.
            if (device.type.id != DeviceTypeIds.ALARM) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = deviceStateText(context, device),
                    style = MaterialTheme.typography.titleSmall,
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (showManagementActions) {
            Row { DeviceManagementActions(onOpenDialog = onOpenDialog) }
        }
    }
}

@Composable
private fun DeviceManagementActions(onOpenDialog: (DeviceDetailDialog) -> Unit) {
    IconButton(onClick = { onOpenDialog(DeviceDetailDialog.Rename) }) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = stringResource(R.string.cd_device_rename),
        )
    }
    IconButton(onClick = { onOpenDialog(DeviceDetailDialog.AssignRoom) }) {
        Icon(
            imageVector = Icons.Outlined.MeetingRoom,
            contentDescription = stringResource(R.string.cd_device_assign_room),
        )
    }
    IconButton(onClick = { onOpenDialog(DeviceDetailDialog.Delete) }) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = stringResource(R.string.cd_device_delete),
        )
    }
}

@Composable
private fun RenameDeviceDialog(
    currentName: String,
    busy: Boolean,
    apiErrorRes: Int?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(currentName) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val nameErrorRes = Validators.name(name)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.device_detail_rename_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                apiErrorRes?.let { ErrorBanner(stringResource(it)) }
                LuminaTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.device_detail_name_label),
                    required = true,
                    error = if (submitted) nameErrorRes?.let { stringResource(it) } else null,
                    enabled = !busy,
                )
            }
        },
        confirmButton = {
            LoadingButton(
                text = stringResource(R.string.action_save),
                loading = busy,
                onClick = {
                    submitted = true
                    if (nameErrorRes == null) onConfirm(name)
                },
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

