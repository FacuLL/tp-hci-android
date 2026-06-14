package tp3.grupo1.hci.itba.edu.ar.ui.screens.homes

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tp3.grupo1.hci.itba.edu.ar.AppContainer
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.data.model.Home
import tp3.grupo1.hci.itba.edu.ar.data.model.HomeSharedUser
import tp3.grupo1.hci.itba.edu.ar.data.network.ApiException
import tp3.grupo1.hci.itba.edu.ar.domain.Validators
import tp3.grupo1.hci.itba.edu.ar.ui.luminaContainer

data class HomesUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    @StringRes val loadErrorRes: Int? = null,
    val homes: List<Home> = emptyList(),
    val currentHomeId: String? = null,
    val dialog: HomesDialog? = null,
    val actionInProgress: Boolean = false,
    @StringRes val actionErrorRes: Int? = null,
    val createForm: CreateHomeForm = CreateHomeForm(),
    val renameForm: RenameHomeForm = RenameHomeForm(),
    val inviteForm: InviteMemberForm = InviteMemberForm(),
    val memberPendingRemoval: HomeSharedUser? = null,
    val failedInvites: FailedInvites? = null,
    val snackbar: SnackbarMessage? = null,
)

sealed interface HomesDialog {
    data object Create : HomesDialog
    data class Rename(val homeId: String) : HomesDialog
    data class Members(val homeId: String) : HomesDialog
    data class Delete(val homeId: String) : HomesDialog
}

data class CreateHomeForm(
    val name: String = "",
    @StringRes val nameErrorRes: Int? = null,
    val emailInput: String = "",
    @StringRes val emailErrorRes: Int? = null,
    val inviteEmails: List<String> = emptyList(),
)

data class RenameHomeForm(
    val name: String = "",
    @StringRes val nameErrorRes: Int? = null,
)

data class InviteMemberForm(
    val email: String = "",
    @StringRes val errorRes: Int? = null,
)

/** Invitations the API rejected (no account registered with that email). */
data class FailedInvites(
    val emails: List<String>,
    val homeCreated: Boolean,
)

data class SnackbarMessage(
    @StringRes val textRes: Int,
    val formatArg: String? = null,
    val id: Long = ids.getAndIncrement(),
) {
    private companion object {
        val ids = AtomicLong()
    }
}

class HomesViewModel(container: AppContainer) : ViewModel() {

    private val homesRepository = container.homesRepository
    private val authRepository = container.authRepository

    private val _uiState = MutableStateFlow(HomesUiState(loading = !homesRepository.loaded.value))
    val uiState: StateFlow<HomesUiState> = _uiState.asStateFlow()

    private var createSubmitAttempted = false
    private var renameSubmitAttempted = false

    init {
        viewModelScope.launch {
            combine(homesRepository.homes, homesRepository.currentHome) { homes, current ->
                homes to current?.id
            }.collect { (homes, currentId) ->
                _uiState.update { it.copy(homes = homes, currentHomeId = currentId) }
            }
        }
        load()
    }

    fun retry() = load()

    /** Pull-to-refresh: re-fetch homes keeping content visible. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true, loadErrorRes = null) }
            try {
                homesRepository.refresh()
                _uiState.update { it.copy(refreshing = false) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(refreshing = false, loadErrorRes = e.userMessageRes) }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = !homesRepository.loaded.value, loadErrorRes = null) }
            try {
                homesRepository.refresh()
                _uiState.update { it.copy(loading = false) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(loading = false, loadErrorRes = e.userMessageRes) }
            }
        }
    }

    fun selectHome(home: Home) {
        if (home.id == _uiState.value.currentHomeId) return
        viewModelScope.launch {
            homesRepository.selectHome(home.id)
            _uiState.update {
                it.copy(snackbar = SnackbarMessage(R.string.homes_active_snackbar, home.name))
            }
        }
    }

    fun openCreate() {
        createSubmitAttempted = false
        _uiState.update {
            it.copy(dialog = HomesDialog.Create, createForm = CreateHomeForm(), actionErrorRes = null)
        }
    }

    fun openRename(home: Home) {
        renameSubmitAttempted = false
        _uiState.update {
            it.copy(
                dialog = HomesDialog.Rename(home.id),
                renameForm = RenameHomeForm(name = home.name),
                actionErrorRes = null,
            )
        }
    }

    fun openMembers(home: Home) {
        _uiState.update {
            it.copy(
                dialog = HomesDialog.Members(home.id),
                inviteForm = InviteMemberForm(),
                actionErrorRes = null,
                memberPendingRemoval = null,
            )
        }
    }

    fun openDelete(home: Home) {
        _uiState.update { it.copy(dialog = HomesDialog.Delete(home.id), actionErrorRes = null) }
    }

    fun dismissDialog() {
        if (_uiState.value.actionInProgress) return
        createSubmitAttempted = false
        renameSubmitAttempted = false
        _uiState.update { it.copy(dialog = null, actionErrorRes = null, memberPendingRemoval = null) }
    }

    fun onCreateNameChange(value: String) {
        _uiState.update {
            it.copy(
                createForm = it.createForm.copy(
                    name = value,
                    nameErrorRes = if (createSubmitAttempted) Validators.name(value) else null,
                )
            )
        }
    }

    fun onCreateEmailChange(value: String) {
        _uiState.update {
            it.copy(createForm = it.createForm.copy(emailInput = value, emailErrorRes = null))
        }
    }

    fun addCreateEmail() {
        val form = _uiState.value.createForm
        val email = form.emailInput.trim()
        val errorRes = inviteEmailError(email, form.inviteEmails, home = null)
        _uiState.update {
            it.copy(
                createForm = if (errorRes == null) {
                    form.copy(emailInput = "", emailErrorRes = null, inviteEmails = form.inviteEmails + email)
                } else {
                    form.copy(emailErrorRes = errorRes)
                }
            )
        }
    }

    fun removeCreateEmail(email: String) {
        _uiState.update {
            it.copy(createForm = it.createForm.copy(inviteEmails = it.createForm.inviteEmails - email))
        }
    }

    fun submitCreate() {
        if (_uiState.value.actionInProgress) return
        createSubmitAttempted = true
        var form = _uiState.value.createForm
        val nameErrorRes = Validators.name(form.name)
        // An email typed but not yet added to the list is validated and included
        // too, so the invitation is not silently lost when submitting.
        val pendingEmail = form.emailInput.trim()
        var emailErrorRes: Int? = null
        if (pendingEmail.isNotEmpty()) {
            emailErrorRes = inviteEmailError(pendingEmail, form.inviteEmails, home = null)
            if (emailErrorRes == null) {
                form = form.copy(emailInput = "", inviteEmails = form.inviteEmails + pendingEmail)
            }
        }
        form = form.copy(nameErrorRes = nameErrorRes, emailErrorRes = emailErrorRes)
        _uiState.update { it.copy(createForm = form) }
        if (nameErrorRes != null || emailErrorRes != null) return

        val name = form.name.trim()
        val emails = form.inviteEmails
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true, actionErrorRes = null) }
            try {
                val outcome = homesRepository.create(name, emails)
                createSubmitAttempted = false
                _uiState.update {
                    it.copy(
                        actionInProgress = false,
                        dialog = null,
                        createForm = CreateHomeForm(),
                        failedInvites = outcome.failedEmails
                            .ifEmpty { null }
                            ?.let { failed -> FailedInvites(failed, homeCreated = true) },
                        snackbar = if (outcome.failedEmails.isEmpty()) {
                            SnackbarMessage(R.string.homes_created_snackbar, outcome.home.name)
                        } else {
                            it.snackbar
                        },
                    )
                }
            } catch (e: ApiException) {
                _uiState.update { it.copy(actionInProgress = false, actionErrorRes = e.userMessageRes) }
            }
        }
    }

    fun onRenameNameChange(value: String) {
        _uiState.update {
            it.copy(
                renameForm = it.renameForm.copy(
                    name = value,
                    nameErrorRes = if (renameSubmitAttempted) Validators.name(value) else null,
                )
            )
        }
    }

    fun submitRename() {
        if (_uiState.value.actionInProgress) return
        val homeId = (_uiState.value.dialog as? HomesDialog.Rename)?.homeId ?: return
        renameSubmitAttempted = true
        val form = _uiState.value.renameForm
        val nameErrorRes = Validators.name(form.name)
        _uiState.update { it.copy(renameForm = form.copy(nameErrorRes = nameErrorRes)) }
        if (nameErrorRes != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true, actionErrorRes = null) }
            try {
                homesRepository.rename(homeId, form.name.trim())
                renameSubmitAttempted = false
                _uiState.update {
                    it.copy(
                        actionInProgress = false,
                        dialog = null,
                        renameForm = RenameHomeForm(),
                        snackbar = SnackbarMessage(R.string.homes_renamed_snackbar),
                    )
                }
            } catch (e: ApiException) {
                _uiState.update { it.copy(actionInProgress = false, actionErrorRes = e.userMessageRes) }
            }
        }
    }

    fun confirmDelete() {
        if (_uiState.value.actionInProgress) return
        val homeId = (_uiState.value.dialog as? HomesDialog.Delete)?.homeId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true) }
            try {
                homesRepository.delete(homeId)
                _uiState.update {
                    it.copy(
                        actionInProgress = false,
                        dialog = null,
                        snackbar = SnackbarMessage(R.string.homes_deleted_snackbar),
                    )
                }
            } catch (e: ApiException) {
                _uiState.update {
                    it.copy(
                        actionInProgress = false,
                        dialog = null,
                        snackbar = SnackbarMessage(e.userMessageRes),
                    )
                }
            }
        }
    }

    fun onInviteEmailChange(value: String) {
        _uiState.update { it.copy(inviteForm = it.inviteForm.copy(email = value, errorRes = null)) }
    }

    fun submitInvite() {
        if (_uiState.value.actionInProgress) return
        val state = _uiState.value
        val homeId = (state.dialog as? HomesDialog.Members)?.homeId ?: return
        val email = state.inviteForm.email.trim()
        val home = state.homes.firstOrNull { it.id == homeId }
        val errorRes = inviteEmailError(email, alreadyAdded = emptyList(), home = home)
        if (errorRes != null) {
            _uiState.update { it.copy(inviteForm = it.inviteForm.copy(errorRes = errorRes)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true, actionErrorRes = null) }
            try {
                val outcome = homesRepository.share(homeId, listOf(email))
                _uiState.update {
                    if (outcome.failedEmails.isEmpty()) {
                        it.copy(
                            actionInProgress = false,
                            inviteForm = InviteMemberForm(),
                            snackbar = SnackbarMessage(R.string.homes_invite_sent_snackbar, email),
                        )
                    } else {
                        it.copy(
                            actionInProgress = false,
                            failedInvites = FailedInvites(outcome.failedEmails, homeCreated = false),
                        )
                    }
                }
            } catch (e: ApiException) {
                _uiState.update { it.copy(actionInProgress = false, actionErrorRes = e.userMessageRes) }
            }
        }
    }

    fun requestMemberRemoval(user: HomeSharedUser) {
        _uiState.update { it.copy(memberPendingRemoval = user) }
    }

    fun cancelMemberRemoval() {
        _uiState.update { it.copy(memberPendingRemoval = null) }
    }

    fun confirmMemberRemoval() {
        if (_uiState.value.actionInProgress) return
        val homeId = (_uiState.value.dialog as? HomesDialog.Members)?.homeId ?: return
        val user = _uiState.value.memberPendingRemoval ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true, actionErrorRes = null) }
            try {
                homesRepository.unshare(homeId, user.email)
                _uiState.update {
                    it.copy(
                        actionInProgress = false,
                        memberPendingRemoval = null,
                        snackbar = SnackbarMessage(
                            R.string.homes_member_removed_snackbar,
                            user.name ?: user.email,
                        ),
                    )
                }
            } catch (e: ApiException) {
                _uiState.update {
                    it.copy(
                        actionInProgress = false,
                        memberPendingRemoval = null,
                        actionErrorRes = e.userMessageRes,
                    )
                }
            }
        }
    }

    fun dismissFailedInvites() {
        _uiState.update { it.copy(failedInvites = null) }
    }

    fun snackbarShown() {
        _uiState.update { it.copy(snackbar = null) }
    }

    @StringRes
    private fun inviteEmailError(email: String, alreadyAdded: List<String>, home: Home?): Int? {
        Validators.email(email)?.let { return it }
        val ownEmail = authRepository.currentUser.value?.email
        return when {
            ownEmail != null && email.equals(ownEmail, ignoreCase = true) ->
                R.string.homes_email_self
            alreadyAdded.any { it.equals(email, ignoreCase = true) } ->
                R.string.homes_email_duplicate
            home?.sharedWith?.any { it.email.equals(email, ignoreCase = true) } == true ->
                R.string.homes_email_already_member
            else -> null
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { HomesViewModel(luminaContainer()) }
        }
    }
}
