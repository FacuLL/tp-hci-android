package tp3.grupo1.hci.itba.edu.ar.ui.screens.registros

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.domain.deviceActionName
import tp3.grupo1.hci.itba.edu.ar.domain.deviceTypeColor
import tp3.grupo1.hci.itba.edu.ar.domain.deviceTypeIcon
import tp3.grupo1.hci.itba.edu.ar.ui.components.CenteredLoading
import tp3.grupo1.hci.itba.edu.ar.ui.components.ErrorBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrosScreen(
    onNavigateUp: () -> Unit,
    onOpenDevice: (String) -> Unit,
) {
    val viewModel: RegistrosViewModel = viewModel(factory = RegistrosViewModel.Factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.registros_title))
                        Text(
                            text = stringResource(R.string.registros_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
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
        when {
            state.loading -> CenteredLoading(Modifier.padding(innerPadding))
            state.errorRes != null && state.rows.isEmpty() -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            ) {
                ErrorBanner(stringResource(state.errorRes!!))
            }
            state.rows.isEmpty() -> EmptyState(Modifier.padding(innerPadding))
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(state.rows, key = { it.id }) { row ->
                    // Solo navegable si el dispositivo sigue en cache (typeId resuelto); si fue
                    // borrado no hay panel que abrir.
                    RegistroItem(
                        row = row,
                        onClick = if (row.typeId != null) {
                            { onOpenDevice(row.deviceId) }
                        } else {
                            null
                        },
                    )
                }
                if (!state.endReached) {
                    item {
                        LoadMoreRow(
                            loading = state.loadingMore,
                            onClick = viewModel::loadMore,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RegistroItem(row: RegistroRow, onClick: (() -> Unit)?) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = row.typeId?.let { deviceTypeColor(it) } ?: MaterialTheme.colorScheme.primary
        Surface(
            shape = CircleShape,
            color = tint.copy(alpha = 0.15f),
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = deviceTypeIcon(row.typeId ?: ""),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = (row.deviceName ?: stringResource(R.string.notif_device_fallback_name)).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = deviceActionName(context, row.actionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = relativeTime(row.timestampMs, row.rawTimestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val result = row.result
        if (result != null) {
            Icon(
                imageVector = if (result) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                contentDescription = stringResource(
                    if (result) R.string.registros_result_ok else R.string.registros_result_fail,
                ),
                tint = if (result) Color(0xFF22A06B) else MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .clip(CircleShape)
                    .size(20.dp),
            )
        }
    }
}

// "Ahora mismo" para <1 min; el resto via DateUtils ("hace 5 minutos", "hace 2 horas"...).
// Si el timestamp no se pudo parsear, cae al valor crudo.
@Composable
private fun relativeTime(timestampMs: Long?, raw: String): String {
    if (timestampMs == null) return raw
    val now = System.currentTimeMillis()
    if (now - timestampMs < DateUtils.MINUTE_IN_MILLIS) {
        return stringResource(R.string.registros_now)
    }
    return DateUtils.getRelativeTimeSpanString(
        timestampMs,
        now,
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
}

@Composable
private fun LoadMoreRow(loading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            TextButton(onClick = onClick) {
                Text(stringResource(R.string.registros_load_more))
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = stringResource(R.string.registros_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
