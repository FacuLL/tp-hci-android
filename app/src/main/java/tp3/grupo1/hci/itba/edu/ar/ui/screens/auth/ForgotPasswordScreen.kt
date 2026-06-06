package tp3.grupo1.hci.itba.edu.ar.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onDone: () -> Unit,
    onNavigateUp: () -> Unit,
) {
    val viewModel: ForgotPasswordViewModel = viewModel(factory = ForgotPasswordViewModel.Factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val successMessage = stringResource(R.string.auth_reset_success)
    LaunchedEffect(uiState.done) {
        if (uiState.done) {
            snackbarHostState.showSnackbar(successMessage)
            onDone()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.auth_forgot_title)) },
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
        AuthLayout(modifier = Modifier.padding(innerPadding)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.auth_step_indicator, uiState.step, FORGOT_PASSWORD_STEPS),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { uiState.step.toFloat() / FORGOT_PASSWORD_STEPS },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            if (uiState.step == 1) {
                EmailStep(viewModel, uiState)
            } else {
                ResetStep(viewModel, uiState)
            }
        }
    }
}

@Composable
private fun EmailStep(viewModel: ForgotPasswordViewModel, uiState: ForgotPasswordUiState) {
    Text(
        text = stringResource(R.string.auth_forgot_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.auth_forgot_subtitle),
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
    Spacer(Modifier.height(16.dp))
    LoadingButton(
        text = stringResource(R.string.auth_send_code_button),
        onClick = viewModel::submitEmail,
        modifier = Modifier.fillMaxWidth(),
        loading = uiState.submitting,
    )
}

@Composable
private fun ResetStep(viewModel: ForgotPasswordViewModel, uiState: ForgotPasswordUiState) {
    Text(
        text = stringResource(R.string.auth_reset_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.auth_reset_message, uiState.email),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(20.dp))
    uiState.apiErrorRes?.let { errorRes ->
        ErrorBanner(stringResource(errorRes))
        Spacer(Modifier.height(16.dp))
    }
    LuminaTextField(
        value = uiState.code,
        onValueChange = viewModel::onCodeChange,
        label = stringResource(R.string.auth_reset_code_label),
        required = true,
        error = uiState.codeError?.let { stringResource(it) },
    )
    Spacer(Modifier.height(12.dp))
    PasswordTextField(
        value = uiState.newPassword,
        onValueChange = viewModel::onNewPasswordChange,
        label = stringResource(R.string.auth_new_password_label),
        error = uiState.newPasswordError?.let { stringResource(it) },
        supportingText = stringResource(R.string.auth_password_requirements),
    )
    Spacer(Modifier.height(12.dp))
    PasswordTextField(
        value = uiState.confirmation,
        onValueChange = viewModel::onConfirmationChange,
        label = stringResource(R.string.auth_confirm_password_label),
        error = uiState.confirmationError?.let { stringResource(it) },
    )
    Spacer(Modifier.height(16.dp))
    LoadingButton(
        text = stringResource(R.string.auth_reset_button),
        onClick = viewModel::submitReset,
        modifier = Modifier.fillMaxWidth(),
        loading = uiState.submitting,
    )
    Spacer(Modifier.height(8.dp))
    TextButton(
        onClick = viewModel::backToEmailStep,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.auth_back_to_step_one))
    }
}
