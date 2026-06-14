package tp3.grupo1.hci.itba.edu.ar.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.ui.components.ErrorBanner
import tp3.grupo1.hci.itba.edu.ar.ui.components.LoadingButton
import tp3.grupo1.hci.itba.edu.ar.ui.components.LuminaTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyAccountScreen(
    email: String,
    onVerified: () -> Unit,
    onNavigateUp: () -> Unit,
) {
    val viewModel: VerifyAccountViewModel = viewModel(factory = VerifyAccountViewModel.factory(email))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val successMessage = stringResource(R.string.auth_verify_success)
    LaunchedEffect(uiState.verified) {
        if (uiState.verified) {
            snackbarHostState.showSnackbar(successMessage)
            onVerified()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.auth_verify_top_title)) },
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
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.auth_verify_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.auth_verify_message, email),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            uiState.displayedCode?.let { code ->
                Spacer(Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.auth_displayed_code_label),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = code,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.auth_displayed_code_hint),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            uiState.apiErrorRes?.let { errorRes ->
                ErrorBanner(stringResource(errorRes))
                Spacer(Modifier.height(16.dp))
            }
            LuminaTextField(
                value = uiState.code,
                onValueChange = viewModel::onCodeChange,
                label = stringResource(R.string.auth_code_label),
                required = true,
                error = uiState.codeError?.let { stringResource(it) },
            )
            Spacer(Modifier.height(16.dp))
            LoadingButton(
                text = stringResource(R.string.auth_verify_button),
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
                    text = stringResource(R.string.auth_no_code_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = viewModel::resend,
                    enabled = !uiState.resending && uiState.resendCooldownSeconds == 0,
                ) {
                    Text(
                        text = if (uiState.resendCooldownSeconds > 0) {
                            pluralStringResource(
                                R.plurals.auth_resend_countdown,
                                uiState.resendCooldownSeconds,
                                uiState.resendCooldownSeconds,
                            )
                        } else {
                            stringResource(R.string.auth_resend_button)
                        },
                    )
                }
            }
            if (uiState.resendSuccess) {
                Text(
                    text = stringResource(R.string.auth_resend_success),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}
