package tp3.grupo1.hci.itba.edu.ar.ui.screens.rooms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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

private const val MISSING_ROOM_NOTICE_MILLIS = 1500L

/** Full screen room detail with its own contextual app bar (RNF2). */
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

    // Leave the screen once the room is deleted from here.
    LaunchedEffect(state.deleted) {
        if (state.deleted) onNavigateUp()
    }

    // The room vanished (deleted elsewhere): show a notice, then go back.
    LaunchedEffect(state.loading, state.room == null) {
        if (!state.loading && state.room == null && !state.deleted) {
            delay(MISSING_ROOM_NOTICE_MILLIS)
            onNavigateUp()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.room?.name.orEmpty(),
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
                    if (state.room != null) {
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
                    devices = state.roomDevices,
                    types = state.types,
                    pendingDeviceIds = state.pendingDeviceIds,
                    onOpenDevice = onOpenDevice,
                    onToggleDevice = viewModel::toggleDevice,
                    onAddDevice = viewModel::openAddDeviceDialog,
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
            onDismiss = viewModel::dismissDialog,
        )
    }
}
