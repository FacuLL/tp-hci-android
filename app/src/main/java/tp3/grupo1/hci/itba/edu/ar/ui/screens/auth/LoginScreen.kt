package tp3.grupo1.hci.itba.edu.ar.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.ui.components.ErrorBanner
import tp3.grupo1.hci.itba.edu.ar.ui.components.LoadingButton
import tp3.grupo1.hci.itba.edu.ar.ui.components.LuminaTextField
import tp3.grupo1.hci.itba.edu.ar.ui.components.PasswordTextField

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    onNeedsVerification: (String) -> Unit,
) {
    val viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.loggedIn) {
        if (uiState.loggedIn) {
            viewModel.onLoggedInHandled()
            onLoggedIn()
        }
    }
    LaunchedEffect(uiState.needsVerificationEmail) {
        val email = uiState.needsVerificationEmail
        if (email != null) {
            viewModel.onNeedsVerificationHandled()
            onNeedsVerification(email)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AuthLayout {
        Text(
            text = stringResource(R.string.auth_login_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.auth_login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        uiState.apiErrorRes?.let { errorRes ->
            ErrorBanner(stringResource(errorRes))
            Spacer(Modifier.height(16.dp))
        }
        LuminaTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = stringResource(R.string.auth_email_label),
            required = true,
            error = uiState.emailError?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        Spacer(Modifier.height(12.dp))
        PasswordTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = stringResource(R.string.auth_password_label),
            error = uiState.passwordError?.let { stringResource(it) },
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onForgotPassword) {
                Text(stringResource(R.string.auth_forgot_password_link))
            }
        }
        Spacer(Modifier.height(8.dp))
        LoadingButton(
            text = stringResource(R.string.auth_login_button),
            onClick = viewModel::submit,
            modifier = Modifier.fillMaxWidth(),
            loading = uiState.submitting,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.auth_no_account),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onRegister) {
                Text(stringResource(R.string.auth_register_link))
            }
        }
        }
        // La dirección del servidor debe poder editarse antes de loguearse: con una API inalcanzable no habría forma de corregir la configuración.
        IconButton(
            onClick = viewModel::openApiConfig,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.auth_cd_api_config),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (uiState.showApiConfig) {
        ApiConfigDialog(
            apiBaseUrl = uiState.apiBaseUrl,
            apiKey = uiState.apiKey,
            urlError = uiState.apiUrlError,
            onUrlChange = viewModel::onApiBaseUrlChange,
            onKeyChange = viewModel::onApiKeyChange,
            onResetDefaults = viewModel::resetApiConfigDefaults,
            onSave = viewModel::saveApiConfig,
            onDismiss = viewModel::closeApiConfig,
        )
    }
}
