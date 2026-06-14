package tp3.grupo1.hci.itba.edu.ar.ui.screens.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowWidthSizeClass
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.data.AppLanguage
import tp3.grupo1.hci.itba.edu.ar.data.ThemeMode
import tp3.grupo1.hci.itba.edu.ar.ui.components.CenteredLoading
import tp3.grupo1.hci.itba.edu.ar.ui.components.ConfirmDialog
import tp3.grupo1.hci.itba.edu.ar.ui.components.ErrorBanner
import tp3.grupo1.hci.itba.edu.ar.ui.components.LoadingButton
import tp3.grupo1.hci.itba.edu.ar.ui.components.LuminaTextField
import tp3.grupo1.hci.itba.edu.ar.ui.components.PasswordTextField

/**
 * Full settings screen: profile, security, personalization (RNF3), API
 * connection (changeable address and key) and session. On medium/expanded
 * widths the content is centered with a bounded width instead of stretching
 * the forms across the whole screen (RNF4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

    uiState.snackbarMessage?.let { messageRes ->
        val message = stringResource(messageRes)
        LaunchedEffect(messageRes) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeSnackbarMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (uiState.loading) {
            CenteredLoading(Modifier.padding(innerPadding))
        } else {
            val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
            val compact = windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = if (compact) Dp.Unspecified else 600.dp)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ErrorBanner(uiState.loadError?.let { stringResource(it) })

                    SettingsSection(
                        icon = Icons.Outlined.Person,
                        title = stringResource(R.string.settings_section_profile),
                    ) {
                        Text(
                            text = currentUser?.name ?: stringResource(R.string.settings_no_name),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        currentUser?.email?.let { email ->
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(
                            onClick = viewModel::openNameDialog,
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text(stringResource(R.string.settings_edit_name))
                        }
                    }

                    SettingsSection(
                        icon = Icons.Outlined.Lock,
                        title = stringResource(R.string.settings_section_security),
                    ) {
                        ErrorBanner(uiState.passwordApiError?.let { stringResource(it) })
                        PasswordTextField(
                            value = uiState.oldPassword,
                            onValueChange = viewModel::onOldPasswordChange,
                            label = stringResource(R.string.settings_current_password),
                            error = uiState.oldPasswordError?.let { stringResource(it) },
                            enabled = !uiState.passwordSaving,
                        )
                        PasswordTextField(
                            value = uiState.newPassword,
                            onValueChange = viewModel::onNewPasswordChange,
                            label = stringResource(R.string.settings_new_password),
                            error = uiState.newPasswordError?.let { stringResource(it) },
                            supportingText = stringResource(R.string.auth_password_requirements),
                            enabled = !uiState.passwordSaving,
                        )
                        PasswordTextField(
                            value = uiState.confirmPassword,
                            onValueChange = viewModel::onConfirmPasswordChange,
                            label = stringResource(R.string.settings_confirm_password),
                            error = uiState.confirmPasswordError?.let { stringResource(it) },
                            enabled = !uiState.passwordSaving,
                        )
                        LoadingButton(
                            text = stringResource(R.string.settings_change_password),
                            onClick = viewModel::submitPasswordChange,
                            loading = uiState.passwordSaving,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    SettingsSection(
                        icon = Icons.Outlined.Palette,
                        title = stringResource(R.string.settings_section_personalization),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_theme_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(modifier = Modifier.selectableGroup()) {
                            ThemeMode.entries.forEach { mode ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = themeMode == mode,
                                            onClick = { viewModel.setThemeMode(mode) },
                                            role = Role.RadioButton,
                                        )
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    RadioButton(selected = themeMode == mode, onClick = null)
                                    Text(
                                        text = stringResource(mode.labelRes()),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                        }
                        Text(
                            text = stringResource(R.string.settings_language_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(modifier = Modifier.selectableGroup()) {
                            AppLanguage.entries.forEach { lang ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = language == lang,
                                            onClick = { viewModel.setLanguage(lang) },
                                            role = Role.RadioButton,
                                        )
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    RadioButton(selected = language == lang, onClick = null)
                                    Text(
                                        text = stringResource(lang.labelRes()),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                        }
                    }

                    SettingsSection(
                        icon = Icons.Outlined.Dns,
                        title = stringResource(R.string.settings_section_server),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_server_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LuminaTextField(
                            value = uiState.apiUrl,
                            onValueChange = viewModel::onApiUrlChange,
                            label = stringResource(R.string.settings_api_url_label),
                            required = true,
                            error = uiState.apiUrlError?.let { stringResource(it) },
                            enabled = !uiState.apiConfigSaving,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        )
                        LuminaTextField(
                            value = uiState.apiKey,
                            onValueChange = viewModel::onApiKeyChange,
                            label = stringResource(R.string.settings_api_key_label),
                            required = true,
                            error = uiState.apiKeyError?.let { stringResource(it) },
                            enabled = !uiState.apiConfigSaving,
                        )
                        LoadingButton(
                            text = stringResource(R.string.action_save),
                            onClick = viewModel::saveApiConfig,
                            loading = uiState.apiConfigSaving,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TextButton(
                            onClick = viewModel::resetApiConfig,
                            enabled = !uiState.apiConfigSaving,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.settings_api_reset))
                        }
                    }

                    SettingsSection(
                        icon = Icons.AutoMirrored.Outlined.Logout,
                        title = stringResource(R.string.settings_section_session),
                    ) {
                        Button(
                            onClick = { showLogoutDialog = true },
                            enabled = !uiState.loggingOut,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.settings_logout))
                        }
                    }
                }
            }
        }
    }

    if (uiState.nameDialogOpen) {
        AlertDialog(
            onDismissRequest = viewModel::dismissNameDialog,
            title = { Text(stringResource(R.string.settings_edit_name)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ErrorBanner(uiState.nameApiError?.let { stringResource(it) })
                    LuminaTextField(
                        value = uiState.nameDraft,
                        onValueChange = viewModel::onNameDraftChange,
                        label = stringResource(R.string.settings_name_label),
                        required = true,
                        error = uiState.nameError?.let { stringResource(it) },
                        enabled = !uiState.nameSaving,
                    )
                }
            },
            confirmButton = {
                LoadingButton(
                    text = stringResource(R.string.action_save),
                    onClick = viewModel::submitName,
                    loading = uiState.nameSaving,
                )
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissNameDialog, enabled = !uiState.nameSaving) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showLogoutDialog) {
        ConfirmDialog(
            title = stringResource(R.string.settings_logout_confirm_title),
            text = stringResource(R.string.settings_logout_confirm_text),
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout()
            },
            onDismiss = { showLogoutDialog = false },
            confirmLabel = stringResource(R.string.settings_logout),
            destructive = false,
        )
    }
}

/** Card with an icon + title header, shared by every settings section. */
@Composable
private fun SettingsSection(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(text = title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@StringRes
private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}

@StringRes
private fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.SYSTEM -> R.string.settings_language_system
    AppLanguage.ES -> R.string.settings_language_spanish
    AppLanguage.EN -> R.string.settings_language_english
}
