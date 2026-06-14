package tp3.grupo1.hci.itba.edu.ar.ui.screens.routines

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Switch
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowWidthSizeClass
import kotlinx.serialization.json.JsonPrimitive
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.data.model.Device
import tp3.grupo1.hci.itba.edu.ar.data.model.Routine
import tp3.grupo1.hci.itba.edu.ar.data.model.RoutineAction
import tp3.grupo1.hci.itba.edu.ar.domain.deviceActionName
import tp3.grupo1.hci.itba.edu.ar.domain.deviceTypeColor
import tp3.grupo1.hci.itba.edu.ar.domain.deviceTypeIcon
import tp3.grupo1.hci.itba.edu.ar.domain.deviceValueLabel
import tp3.grupo1.hci.itba.edu.ar.ui.components.CenteredLoading
import tp3.grupo1.hci.itba.edu.ar.ui.components.ConfirmDialog
import tp3.grupo1.hci.itba.edu.ar.ui.components.EmptyState
import tp3.grupo1.hci.itba.edu.ar.ui.components.ErrorBanner
import tp3.grupo1.hci.itba.edu.ar.ui.components.LoadingButton

/**
 * Routines tab (RF11/RF12): routines are listed, executed, created, edited
 * and deleted here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    onOpenSettings: () -> Unit,
    onCreateRoutine: () -> Unit,
    onEditRoutine: (String) -> Unit,
) {
    val viewModel: RoutinesViewModel = viewModel(factory = RoutinesViewModel.Factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var routineToDelete by remember { mutableStateOf<Routine?>(null) }

    val snackbarMessage = state.snackbarMessageRes?.let { stringResource(it) }
    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(snackbarMessage)
            viewModel.consumeSnackbarMessage()
        }
    }

    // RNF4/RNF5: single column on phones, two-column grid on wider layouts.
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val columns = if (windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT) 1 else 2

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.nav_routines))
                        if (state.routines.isNotEmpty()) {
                            RoutinesSubtitle(routines = state.routines)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.cd_open_settings),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateRoutine) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.routine_cd_add))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            state.loading && state.routines.isEmpty() ->
                CenteredLoading(Modifier.padding(innerPadding))

            state.routines.isEmpty() -> {
                val loadErrorRes = state.loadErrorRes
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (loadErrorRes != null) {
                        ErrorBanner(stringResource(loadErrorRes))
                        Button(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    } else {
                        EmptyState(
                            icon = Icons.Outlined.Bolt,
                            title = stringResource(R.string.routines_empty_title),
                            subtitle = stringResource(R.string.routines_empty_subtitle),
                            actionLabel = stringResource(R.string.routine_new_title),
                            onAction = onCreateRoutine,
                        )
                    }
                }
            }

            else ->
                PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = { viewModel.refresh(manual = true) },
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        state.loadErrorRes?.let { errorRes ->
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                ErrorBanner(stringResource(errorRes))
                            }
                        }
                        items(state.routines, key = { it.id }) { routine ->
                            RoutineCard(
                                routine = routine,
                                devicesById = state.devicesById,
                                executing = state.executingId == routine.id,
                                toggling = routine.id in state.togglingIds,
                                onSetEnabled = { enabled -> viewModel.setEnabled(routine, enabled) },
                                onExecute = { viewModel.execute(routine.id) },
                                onEdit = { onEditRoutine(routine.id) },
                                onDelete = { routineToDelete = routine },
                            )
                        }
                    }
                }
        }
    }

    routineToDelete?.let { routine ->
        ConfirmDialog(
            title = stringResource(R.string.routine_delete_title),
            text = stringResource(R.string.routine_delete_text, routine.name),
            onConfirm = {
                viewModel.delete(routine.id)
                routineToDelete = null
            },
            onDismiss = { routineToDelete = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoutineCard(
    routine: Routine,
    devicesById: Map<String, Device>,
    executing: Boolean,
    toggling: Boolean,
    onSetEnabled: (Boolean) -> Unit,
    onExecute: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val schedule = routine.schedule
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevronRotation",
    )
    val showChips = !schedule.enabled ||
        (schedule.isScheduled && (schedule.time != null || schedule.days.isNotEmpty()))

    OutlinedCard(
        onClick = { expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(
            width = 1.dp,
            color = if (schedule.enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = routine.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.routines_action_count,
                            routine.actions.size,
                            routine.actions.size,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Only scheduled (automatic) routines can be enabled/disabled;
                // manual ones are always run on demand.
                if (schedule.isScheduled) {
                    Switch(
                        checked = schedule.enabled,
                        onCheckedChange = onSetEnabled,
                        enabled = !toggling,
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.routine_cd_edit),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.routine_cd_delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = stringResource(
                        if (expanded) R.string.routines_cd_collapse_actions
                        else R.string.routines_cd_expand_actions
                    ),
                    modifier = Modifier.rotate(chevronRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (showChips) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (schedule.isScheduled) {
                        schedule.time?.let { time ->
                            AssistChip(
                                onClick = { expanded = !expanded },
                                label = { Text(time) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                                    )
                                },
                            )
                        }
                        if (schedule.days.isNotEmpty()) {
                            AssistChip(
                                onClick = { expanded = !expanded },
                                label = { Text(scheduleDaysLabel(context, schedule.days)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                                    )
                                },
                            )
                        }
                    }
                    if (!schedule.enabled) {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(stringResource(R.string.routines_inactive)) },
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (routine.actions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.routines_no_actions),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        routine.actions.forEach { action ->
                            RoutineActionRow(
                                action = action,
                                device = devicesById[action.device.id],
                            )
                        }
                    }
                }
            }

            LoadingButton(
                text = stringResource(R.string.routines_execute),
                onClick = onExecute,
                modifier = Modifier.align(Alignment.End),
                enabled = schedule.enabled,
                loading = executing,
            )
        }
    }
}

@Composable
private fun RoutineActionRow(action: RoutineAction, device: Device?) {
    val context = LocalContext.current
    val typeId = device?.type?.id ?: ""
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = deviceTypeIcon(typeId),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = deviceTypeColor(typeId),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = device?.name ?: stringResource(R.string.routines_device_deleted),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = actionLabel(context, action),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Localized action name plus its parameters, when present. */
private fun actionLabel(context: Context, action: RoutineAction): String {
    val name = deviceActionName(context, action.actionName)
    val params = action.params
        .filterIsInstance<JsonPrimitive>()
        .joinToString(", ") { param ->
            if (param.isString) deviceValueLabel(context, param.content) else param.content
        }
    return if (params.isEmpty()) {
        name
    } else {
        context.getString(R.string.routines_action_detail, name, params)
    }
}

/** Day codes persisted by the web client inside routine metadata ("lu".."do"). */
private val DAY_ORDER = listOf("lu", "ma", "mi", "ju", "vi", "sa", "do")

@StringRes
private fun dayLabelRes(day: String): Int? = when (day) {
    "lu", "lunes" -> R.string.routines_day_monday
    "ma", "martes" -> R.string.routines_day_tuesday
    "mi", "miercoles", "miércoles" -> R.string.routines_day_wednesday
    "ju", "jueves" -> R.string.routines_day_thursday
    "vi", "viernes" -> R.string.routines_day_friday
    "sa", "sabado", "sábado" -> R.string.routines_day_saturday
    "do", "domingo" -> R.string.routines_day_sunday
    else -> null
}

/** Full day names in canonical order, comma separated and capitalized. */
private fun scheduleDaysLabel(context: Context, days: List<String>): String =
    days.map { it.lowercase() }
        .sortedBy { day ->
            val index = DAY_ORDER.indexOf(day.take(2))
            if (index == -1) DAY_ORDER.size else index
        }
        .joinToString(", ") { day ->
            dayLabelRes(day)?.let(context::getString)
                ?: day.replaceFirstChar { it.uppercaseChar() }
        }

/** Header subtitle: active routine count plus the time until the next run. */
@Composable
private fun RoutinesSubtitle(routines: List<Routine>) {
    val context = LocalContext.current
    val activeCount = routines.count { it.schedule.isScheduled && it.schedule.enabled }
    val nextMinutes = remember(routines) { nextRoutineMinutes(routines) }
    val activeText = pluralStringResource(R.plurals.routines_active_count, activeCount, activeCount)
    val nextText = when {
        nextMinutes == null -> null
        nextMinutes < 1L -> stringResource(R.string.routines_next_soon)
        else -> stringResource(R.string.routines_next_in, formatDelay(context, nextMinutes))
    }
    Text(
        text = if (nextText != null) "$activeText · $nextText" else activeText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private val DAY_CODE_TO_DOW = mapOf(
    "lu" to DayOfWeek.MONDAY,
    "ma" to DayOfWeek.TUESDAY,
    "mi" to DayOfWeek.WEDNESDAY,
    "ju" to DayOfWeek.THURSDAY,
    "vi" to DayOfWeek.FRIDAY,
    "sa" to DayOfWeek.SATURDAY,
    "do" to DayOfWeek.SUNDAY,
)

/**
 * Minutes from now until the next scheduled+enabled routine fires, or null when
 * none is scheduled. Empty day list means the routine runs every day.
 */
private fun nextRoutineMinutes(routines: List<Routine>): Long? {
    val now = LocalDateTime.now()
    var best: Long? = null
    routines.forEach { routine ->
        val schedule = routine.schedule
        if (!schedule.isScheduled || !schedule.enabled) return@forEach
        val time = schedule.time?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return@forEach
        val days = if (schedule.days.isEmpty()) {
            DayOfWeek.values().toSet()
        } else {
            schedule.days.mapNotNull { DAY_CODE_TO_DOW[it.lowercase().take(2)] }.toSet()
        }
        if (days.isEmpty()) return@forEach
        for (offset in 0..7L) {
            val date = now.toLocalDate().plusDays(offset)
            if (date.dayOfWeek in days) {
                val candidate = LocalDateTime.of(date, time)
                if (candidate.isAfter(now)) {
                    val minutes = Duration.between(now, candidate).toMinutes()
                    if (best == null || minutes < best!!) best = minutes
                    break
                }
            }
        }
    }
    return best
}

private fun formatDelay(context: Context, minutes: Long): String {
    val hours = (minutes / 60).toInt()
    val mins = (minutes % 60).toInt()
    return if (hours > 0) {
        context.getString(R.string.routines_duration_hm, hours, mins)
    } else {
        context.getString(R.string.routines_duration_m, mins)
    }
}
