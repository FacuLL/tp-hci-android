package tp3.grupo1.hci.itba.edu.ar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tp3.grupo1.hci.itba.edu.ar.AppContainer
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.ui.luminaContainer

/**
 * Circular profile avatar used in every screen's app bar for navigation to
 * settings, replacing the old gear icon for a consistent top bar. Shows the
 * user's initials, falling back to a person icon when the name is unknown.
 */
@Composable
fun ProfileAvatar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ProfileAvatarViewModel = viewModel(factory = ProfileAvatarViewModel.Factory)
    val name by viewModel.userName.collectAsStateWithLifecycle()
    ProfileAvatar(name = name, onClick = onClick, modifier = modifier)
}

@Composable
fun ProfileAvatar(
    name: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initials = (name ?: "").trim()
        .split(' ')
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .padding(end = 8.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (initials.isNotBlank()) {
            Text(
                text = initials,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = stringResource(R.string.cd_open_settings),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

class ProfileAvatarViewModel(container: AppContainer) : ViewModel() {
    val userName: StateFlow<String?> = container.authRepository.currentUser
        .map { it?.name }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory = viewModelFactory {
            initializer { ProfileAvatarViewModel(luminaContainer()) }
        }
    }
}
