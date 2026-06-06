package tp3.grupo1.hci.itba.edu.ar.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
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
fun RegisterScreen(
    onRegistered: (String) -> Unit,
    onNavigateUp: () -> Unit,
) {
    val viewModel: RegisterViewModel = viewModel(factory = RegisterViewModel.Factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.registeredEmail) {
        val email = uiState.registeredEmail
        if (email != null) {
            viewModel.onRegisteredHandled()
            onRegistered(email)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.auth_register_top_title)) },
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
    ) { innerPadding ->
        AuthLayout(modifier = Modifier.padding(innerPadding)) {
            Text(
                text = stringResource(R.string.auth_register_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.auth_register_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            uiState.apiErrorRes?.let { errorRes ->
                ErrorBanner(stringResource(errorRes))
                Spacer(Modifier.height(16.dp))
            }
            LuminaTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = stringResource(R.string.auth_name_label),
                required = true,
                error = uiState.nameError?.let { stringResource(it) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            Spacer(Modifier.height(12.dp))
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
                supportingText = stringResource(R.string.auth_password_requirements),
            )
            Spacer(Modifier.height(12.dp))
            PasswordTextField(
                value = uiState.confirmation,
                onValueChange = viewModel::onConfirmationChange,
                label = stringResource(R.string.auth_confirm_password_label),
                error = uiState.confirmationError?.let { stringResource(it) },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.required_fields_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            LoadingButton(
                text = stringResource(R.string.auth_register_button),
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
                    text = stringResource(R.string.auth_have_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onNavigateUp) {
                    Text(stringResource(R.string.auth_login_link))
                }
            }
        }
    }
}
