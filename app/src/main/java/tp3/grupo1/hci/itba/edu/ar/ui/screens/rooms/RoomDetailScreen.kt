package tp3.grupo1.hci.itba.edu.ar.ui.screens.rooms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import tp3.grupo1.hci.itba.edu.ar.ui.components.FloatingTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.ui.components.CenteredLoading
import tp3.grupo1.hci.itba.edu.ar.ui.components.ConfirmDialog
import tp3.grupo1.hci.itba.edu.ar.ui.components.EmptyState
import tp3.grupo1.hci.itba.edu.ar.ui.screens.devices.CreateDeviceDialog

private const val MISSING_ROOM_NOTICE_MILLIS = 1500L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(
    roomId: String,
    onNavigateUp: () -> Unit,
    onOpenDevice: (String) -> Unit,
) {
    val viewModel: RoomDetailViewModel = viewModel(
        key = "room_detail_$roomId",
        factory = RoomDetailViewModel.Factory(roomId),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.snackbarMessageRes) {
        state.snackbarMessageRes?.let { messageRes ->
            snackbarHostState.showSnackbar(context.getString(messageRes))
            viewModel.onSnackbarShown()
        }
    }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onNavigateUp()
    }

    // La room desaparecio (borrada en otro lado): muestra un aviso y vuelve atras.
    LaunchedEffect(state.loading, state.room == null) {
        if (!state.loading && state.room == null && !state.deleted) {
            delay(MISSING_ROOM_NOTICE_MILLIS)
            onNavigateUp()
        }
    }

    Scaffold(
        topBar = {
            FloatingTopBar(
                title = {
                    Text(
                        text = state.room?.name.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = if (state.editMode) viewModel::cancelReorder else onNavigateUp,
                        enabled = !state.savingOrder,
                    ) {
                        Icon(
                            imageVector = if (state.editMode) Icons.Outlined.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(
                                if (state.editMode) R.string.rooms_reorder_cancel else R.string.cd_navigate_back,
                            ),
                        )
                    }
                },
                actions = {
                    if (state.room != null) {
                        if (state.editMode) {
                            IconButton(
                                onClick = viewModel::saveOrder,
                                enabled = !state.savingOrder,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = stringResource(R.string.rooms_reorder_save),
                                )
                            }
                        } else {
                            IconButton(
                                onClick = viewModel::enterReorderMode,
                                enabled = state.roomDevices.size > 1,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SwapVert,
                                    contentDescription = stringResource(R.string.rooms_action_reorder),
                                )
                            }
                            IconButton(onClick = viewModel::openRenameDialog) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = stringResource(R.string.rooms_action_rename),
                                )
                            }
                            IconButton(onClick = viewModel::openDeleteDialog) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = stringResource(R.string.rooms_cd_delete_room),
                                )
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val room = state.room
        when {
            room == null && state.loading -> CenteredLoading(Modifier.padding(innerPadding))
            room == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Outlined.MeetingRoom,
                    title = stringResource(R.string.rooms_missing_title),
                    subtitle = stringResource(R.string.rooms_missing_subtitle),
                )
            }
            else -> PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                RoomDetailContent(
                    room = room,
                    devices = if (state.editMode) state.draftOrder else state.roomDevices,
                    types = state.types,
                    pendingDeviceIds = state.pendingDeviceIds,
                    showTitle = false,
                    editMode = state.editMode,
                    onOpenDevice = onOpenDevice,
                    onToggleDevice = viewModel::toggleDevice,
                    onAddDevice = viewModel::openAddDeviceDialog,
                    onMoveDevice = viewModel::moveDevice,
                )
            }
        }
    }

    when (state.dialog) {
        null -> Unit
        RoomDetailDialog.Rename -> RoomNameDialog(
            title = stringResource(R.string.rooms_rename_room),
            confirmLabel = stringResource(R.string.action_save),
            name = state.nameInput,
            nameError = state.nameErrorRes?.let { stringResource(it) },
            apiError = state.dialogErrorRes?.let { stringResource(it) },
            saving = state.saving,
            onNameChange = viewModel::onNameChange,
            onSubmit = viewModel::submitRename,
            onDismiss = viewModel::dismissDialog,
        )
        RoomDetailDialog.ConfirmDelete -> ConfirmDialog(
            title = stringResource(R.string.rooms_delete_room_title),
            text = stringResource(R.string.rooms_delete_room_message, state.room?.name.orEmpty()),
            onConfirm = viewModel::deleteRoom,
            onDismiss = viewModel::dismissDialog,
        )
        RoomDetailDialog.AddDevice -> AddDeviceDialog(
            unassigned = state.unassignedDevices,
            types = state.types,
            pendingDeviceIds = state.pendingDeviceIds,
            onAssign = { device -> viewModel.assignDevice(device.id) },
            onCreateNew = viewModel::openCreateDeviceDialog,
            onDismiss = viewModel::dismissDialog,
        )
        RoomDetailDialog.CreateDevice -> CreateDeviceDialog(
            rooms = state.rooms,
            creating = state.creatingDevice,
            apiErrorRes = state.createDeviceErrorRes,
            initialRoomId = state.room?.id,
            onCreate = { name, typeId, roomId -> viewModel.createDevice(name, typeId, roomId) },
            onDismiss = viewModel::dismissDialog,
        )
    }
}
