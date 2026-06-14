package tp3.grupo1.hci.itba.edu.ar.ui.screens.homes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddHome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowWidthSizeClass
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.data.model.Home
import tp3.grupo1.hci.itba.edu.ar.ui.components.CenteredLoading
import tp3.grupo1.hci.itba.edu.ar.ui.components.ConfirmDialog
import tp3.grupo1.hci.itba.edu.ar.ui.components.EmptyState
import tp3.grupo1.hci.itba.edu.ar.ui.components.ErrorBanner

/**
 * Home management (RF17-19): list of homes with the active one highlighted,
 * creation with optional invitations, members management and deletion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomesScreen(
    onNavigateUp: () -> Unit,
) {
    val viewModel: HomesViewModel = viewModel(factory = HomesViewModel.Factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val snackbar = uiState.snackbar
    if (snackbar != null) {
        val text = snackbar.formatArg
            ?.let { stringResource(snackbar.textRes, it) }
            ?: stringResource(snackbar.textRes)
        LaunchedEffect(snackbar) {
            snackbarHostState.showSnackbar(text)
            viewModel.snackbarShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_homes)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::openCreate) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.homes_cd_add_home),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val loadErrorRes = uiState.loadErrorRes
        when {
            uiState.loading -> CenteredLoading(Modifier.padding(innerPadding))
            uiState.homes.isEmpty() && loadErrorRes != null -> LoadErrorState(
                message = stringResource(loadErrorRes),
                onRetry = viewModel::retry,
                modifier = Modifier.padding(innerPadding),
            )
            uiState.homes.isEmpty() -> EmptyState(
                icon = Icons.Outlined.AddHome,
                title = stringResource(R.string.homes_empty_title),
                modifier = Modifier.padding(innerPadding),
                subtitle = stringResource(R.string.homes_empty_subtitle),
                actionLabel = stringResource(R.string.homes_create_action),
                onAction = viewModel::openCreate,
            )
            else -> PullToRefreshBox(
                isRefreshing = uiState.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                HomesContent(
                    homes = uiState.homes,
                    currentHomeId = uiState.currentHomeId,
                    onSelect = viewModel::selectHome,
                    onRename = viewModel::openRename,
                    onMembers = viewModel::openMembers,
                    onDelete = viewModel::openDelete,
                )
            }
        }
    }

    HomesScreenDialogs(uiState = uiState, viewModel = viewModel)
}

@Composable
private fun HomesScreenDialogs(uiState: HomesUiState, viewModel: HomesViewModel) {
    when (val dialog = uiState.dialog) {
        HomesDialog.Create -> CreateHomeDialog(
            form = uiState.createForm,
            saving = uiState.actionInProgress,
            apiErrorRes = uiState.actionErrorRes,
            onNameChange = viewModel::onCreateNameChange,
            onEmailChange = viewModel::onCreateEmailChange,
            onAddEmail = viewModel::addCreateEmail,
            onRemoveEmail = viewModel::removeCreateEmail,
            onSubmit = viewModel::submitCreate,
            onDismiss = viewModel::dismissDialog,
        )
        is HomesDialog.Rename -> RenameHomeDialog(
            form = uiState.renameForm,
            saving = uiState.actionInProgress,
            apiErrorRes = uiState.actionErrorRes,
            onNameChange = viewModel::onRenameNameChange,
            onSubmit = viewModel::submitRename,
            onDismiss = viewModel::dismissDialog,
        )
        is HomesDialog.Members -> uiState.homes.firstOrNull { it.id == dialog.homeId }?.let { home ->
            MembersDialog(
                home = home,
                inviteForm = uiState.inviteForm,
                busy = uiState.actionInProgress,
                apiErrorRes = uiState.actionErrorRes,
                onInviteEmailChange = viewModel::onInviteEmailChange,
                onSubmitInvite = viewModel::submitInvite,
                onRemoveMember = viewModel::requestMemberRemoval,
                onDismiss = viewModel::dismissDialog,
            )
        }
        is HomesDialog.Delete -> uiState.homes.firstOrNull { it.id == dialog.homeId }?.let { home ->
            ConfirmDialog(
                title = stringResource(R.string.homes_delete_title),
                text = stringResource(R.string.homes_delete_text, home.name),
                onConfirm = viewModel::confirmDelete,
                onDismiss = viewModel::dismissDialog,
            )
        }
        null -> Unit
    }

    uiState.memberPendingRemoval?.let { user ->
        ConfirmDialog(
            title = stringResource(R.string.homes_remove_member_title),
            text = stringResource(R.string.homes_remove_member_text, user.name ?: user.email),
            onConfirm = viewModel::confirmMemberRemoval,
            onDismiss = viewModel::cancelMemberRemoval,
            confirmLabel = stringResource(R.string.homes_remove_action),
        )
    }

    uiState.failedInvites?.let { failed ->
        FailedInvitesDialog(failedInvites = failed, onDismiss = viewModel::dismissFailedInvites)
    }
}

/**
 * RNF4/RNF5: phones show a single column, while medium and expanded widths
 * reorganize the homes into a two column grid of cards.
 */
@Composable
private fun HomesContent(
    homes: List<Home>,
    currentHomeId: String?,
    onSelect: (Home) -> Unit,
    onRename: (Home) -> Unit,
    onMembers: (Home) -> Unit,
    onDelete: (Home) -> Unit,
    modifier: Modifier = Modifier,
) {
    val compact = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass ==
        WindowWidthSizeClass.COMPACT
    if (compact) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(homes, key = { it.id }) { home ->
                HomeCard(
                    home = home,
                    isActive = home.id == currentHomeId,
                    onSelect = { onSelect(home) },
                    onRename = { onRename(home) },
                    onMembers = { onMembers(home) },
                    onDelete = { onDelete(home) },
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(homes, key = { it.id }) { home ->
                HomeCard(
                    home = home,
                    isActive = home.id == currentHomeId,
                    onSelect = { onSelect(home) },
                    onRename = { onRename(home) },
                    onMembers = { onMembers(home) },
                    onDelete = { onDelete(home) },
                )
            }
        }
    }
}

@Composable
private fun HomeCard(
    home: Home,
    isActive: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onMembers: () -> Unit,
    onDelete: () -> Unit,
) {
    val memberCount = home.sharedWith.size + 1
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = if (isActive) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Home,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = home.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = pluralStringResource(R.plurals.homes_member_count, memberCount, memberCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isActive) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text(
                        text = stringResource(R.string.homes_active_badge),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            HomeCardMenu(onRename = onRename, onMembers = onMembers, onDelete = onDelete)
        }
    }
}

@Composable
private fun HomeCardMenu(
    onRename: () -> Unit,
    onMembers: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.cd_more_options),
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.homes_menu_rename)) },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.homes_menu_members)) },
                leadingIcon = { Icon(Icons.Outlined.Group, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    onMembers()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete)) },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error,
                ),
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun LoadErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        ErrorBanner(message)
        Button(onClick = onRetry) {
            Text(stringResource(R.string.action_retry))
        }
    }
}
