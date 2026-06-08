package tp3.grupo1.hci.itba.edu.ar.ui.screens.homes

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.data.model.Home
import tp3.grupo1.hci.itba.edu.ar.data.model.HomeSharedUser
import tp3.grupo1.hci.itba.edu.ar.ui.components.ErrorBanner
import tp3.grupo1.hci.itba.edu.ar.ui.components.LoadingButton
import tp3.grupo1.hci.itba.edu.ar.ui.components.LuminaTextField

/** Creation form: home name plus an optional list of emails to invite. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateHomeDialog(
    form: CreateHomeForm,
    saving: Boolean,
    @StringRes apiErrorRes: Int?,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onAddEmail: () -> Unit,
    onRemoveEmail: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.homes_create_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ErrorBanner(apiErrorRes?.let { stringResource(it) })
                Text(
                    text = stringResource(R.string.required_fields_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LuminaTextField(
                    value = form.name,
                    onValueChange = onNameChange,
                    label = stringResource(R.string.homes_name_label),
                    required = true,
                    error = form.nameErrorRes?.let { stringResource(it) },
                    enabled = !saving,
                    supportingText = stringResource(R.string.homes_name_hint),
                )
                Text(
                    text = stringResource(R.string.homes_invite_section),
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    LuminaTextField(
                        value = form.emailInput,
                        onValueChange = onEmailChange,
                        label = stringResource(R.string.homes_invite_email_label),
                        modifier = Modifier.weight(1f),
                        error = form.emailErrorRes?.let { stringResource(it) },
                        enabled = !saving,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    )
                    FilledTonalIconButton(
                        onClick = onAddEmail,
                        modifier = Modifier.padding(top = 8.dp),
                        enabled = !saving && form.emailInput.isNotBlank(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.homes_cd_add_email),
                        )
                    }
                }
                if (form.inviteEmails.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        form.inviteEmails.forEach { email ->
                            InputChip(
                                selected = false,
                                onClick = { onRemoveEmail(email) },
                                enabled = !saving,
                                label = { Text(email, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = stringResource(R.string.homes_cd_remove_email),
                                        modifier = Modifier.size(InputChipDefaults.IconSize),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            LoadingButton(
                text = stringResource(R.string.action_create),
                onClick = onSubmit,
                loading = saving,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
fun RenameHomeDialog(
    form: RenameHomeForm,
    saving: Boolean,
    @StringRes apiErrorRes: Int?,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.homes_rename_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ErrorBanner(apiErrorRes?.let { stringResource(it) })
                LuminaTextField(
                    value = form.name,
                    onValueChange = onNameChange,
                    label = stringResource(R.string.homes_name_label),
                    required = true,
                    error = form.nameErrorRes?.let { stringResource(it) },
                    enabled = !saving,
                )
            }
        },
        confirmButton = {
            LoadingButton(
                text = stringResource(R.string.action_save),
                onClick = onSubmit,
                loading = saving,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/** Members of a home: current guests with removal, plus an invite field. */
@Composable
fun MembersDialog(
    home: Home,
    inviteForm: InviteMemberForm,
    busy: Boolean,
    @StringRes apiErrorRes: Int?,
    onInviteEmailChange: (String) -> Unit,
    onSubmitInvite: () -> Unit,
    onRemoveMember: (HomeSharedUser) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.homes_members_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = home.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ErrorBanner(apiErrorRes?.let { stringResource(it) })
                if (home.sharedWith.isEmpty()) {
                    Text(
                        text = stringResource(R.string.homes_members_empty),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    home.sharedWith.forEach { user ->
                        MemberRow(user = user, enabled = !busy, onRemove = { onRemoveMember(user) })
                    }
                }
                HorizontalDivider()
                LuminaTextField(
                    value = inviteForm.email,
                    onValueChange = onInviteEmailChange,
                    label = stringResource(R.string.homes_invite_email_label),
                    error = inviteForm.errorRes?.let { stringResource(it) },
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                LoadingButton(
                    text = stringResource(R.string.homes_invite_button),
                    onClick = onSubmitInvite,
                    modifier = Modifier.align(Alignment.End),
                    enabled = inviteForm.email.isNotBlank(),
                    loading = busy,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

@Composable
private fun MemberRow(user: HomeSharedUser, enabled: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name ?: user.email,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (user.name != null) {
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onRemove, enabled = enabled) {
            Icon(
                imageVector = Icons.Outlined.PersonRemove,
                contentDescription = stringResource(R.string.homes_cd_remove_member),
            )
        }
    }
}

/**
 * Explicit warning when some invitations could not be sent because no account
 * exists with that email, so a home is never shared silently with nobody.
 */
@Composable
fun FailedInvitesDialog(failedInvites: FailedInvites, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.WarningAmber, contentDescription = null) },
        title = { Text(stringResource(R.string.homes_failed_invites_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(
                        if (failedInvites.homeCreated) {
                            R.string.homes_failed_invites_created_intro
                        } else {
                            R.string.homes_failed_invites_intro
                        }
                    ),
                )
                failedInvites.emails.forEach { email ->
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_ok))
            }
        },
    )
}
