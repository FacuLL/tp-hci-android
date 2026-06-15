package tp3.grupo1.hci.itba.edu.ar.ui.screens.routines

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import tp3.grupo1.hci.itba.edu.ar.ui.components.FloatingTopBar
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
import tp3.grupo1.hci.itba.edu.ar.ui.components.ProfileAvatar

// Pestania de rutinas (RF11/RF12): listar, ejecutar, crear, editar y eliminar.
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

    // RNF4/RNF5: escala por ancho: 1 columna en telefonos, 2 en horizontal/tablet
    // chica, 3+ en tablet horizontal (Adaptive elige la mayor cantidad cuyo ancho
    // de tarjeta sea >= minSize).
    val widthClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    Scaffold(
        // El outer NavigationSuiteScaffold ya consumio los insets del sistema
        // y posiciona el contenido arriba del bottom nav. Sin esto el Scaffold
        // interno re-aplica safeDrawing (default), dejando innerPadding.bottom=0
        // y forzando hacks como contentPadding(bottom = 96.dp).
        contentWindowInsets = WindowInsets(0),
        topBar = {
            FloatingTopBar(
                title = {
                    Column {
                        Text(stringResource(R.string.nav_routines))
                        if (state.routines.isNotEmpty()) {
                            RoutinesSubtitle(routines = state.routines)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onCreateRoutine) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.routine_cd_add),
                        )
                    }
                    ProfileAvatar(onClick = onOpenSettings)
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            state.loading && state.routines.isEmpty() ->
                CenteredLoading(Modifier.padding(innerPadding))

            state.routines.isEmpty() -> {
                val loadErrorRes = state.loadErrorRes
                // Centrado y con ancho limitado para que el estado vacio/error no se
                // estire en horizontal/tablet (ErrorBanner usa fillMaxWidth internamente).
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 480.dp)
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
            }

            else ->
                PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = { viewModel.refresh(manual = true) },
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                ) {
                    // En no-COMPACT (landscape phone, tablet) capeamos el
                    // ancho a 880dp y centramos para que las cards no se
                    // estiren edge-to-edge. En COMPACT no hay margen: una
                    // sola columna ocupando todo el ancho.
                    val gridModifier = if (widthClass == WindowWidthSizeClass.COMPACT) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxSize()
                            .widthIn(max = 880.dp)
                            .align(Alignment.TopCenter)
                    }
                    LazyVerticalGrid(
                        columns = if (widthClass == WindowWidthSizeClass.COMPACT) {
                            GridCells.Fixed(1)
                        } else {
                            // 280dp permite 2 columnas en landscape phone aun
                            // con el contentPadding horizontal de 48dp que
                            // deja mas aire a los costados.
                            GridCells.Adaptive(minSize = 280.dp)
                        },
                        modifier = gridModifier,
                        // 48dp horizontal en landscape — el usuario pidio
                        // mas margen a los costados. Vertical 16dp.
                        contentPadding = if (widthClass == WindowWidthSizeClass.COMPACT) {
                            PaddingValues(16.dp)
                        } else {
                            PaddingValues(horizontal = 48.dp, vertical = 16.dp)
                        },
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
    var menuOpen by remember { mutableStateOf(false) }
    val schedule = routine.schedule
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevronRotation",
    )
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
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                        text = if (!schedule.isScheduled || schedule.time == null) {
                            stringResource(R.string.routines_manual)
                        } else {
                            stringResource(R.string.routines_time, schedule.time)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = stringResource(R.string.cd_more_options),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.routine_cd_edit)) },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.routine_cd_delete)) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                        )
                    }
                }
                // Solo las rutinas programadas (automaticas) se pueden activar/desactivar;
                // las manuales se ejecutan a demanda.
                if (schedule.isScheduled) {
                    Switch(
                        checked = schedule.enabled,
                        onCheckedChange = onSetEnabled,
                        enabled = !toggling,
                        // El thumb apagado por defecto usa `outline` (muy palido en claro) y se
                        // pierde sobre el track. Solo se ajusta su color; borde y track quedan
                        // en el default para no romper consistencia con los demas switches.
                        colors = SwitchDefaults.colors(
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                    Spacer(Modifier.width(4.dp))
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

            if (schedule.isScheduled && schedule.days.isNotEmpty()) {
                // Dias + boton Run en una sola fila. Antes el Run estaba
                // en una fila propia debajo (~40dp extra). Asi la card es
                // ~50dp mas baja y entran mas routines en landscape.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        DAY_ORDER.forEach { day ->
                            val selected = day in schedule.days
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = dayShortLabel(context, day),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    LoadingButton(
                        text = stringResource(R.string.routines_execute),
                        onClick = onExecute,
                        enabled = schedule.enabled,
                        loading = executing,
                    )
                }
            } else {
                // Routine manual (sin dias) -> Run a la derecha en su
                // propia fila pero igual mas compacta.
                LoadingButton(
                    text = stringResource(R.string.routines_execute),
                    onClick = onExecute,
                    modifier = Modifier.align(Alignment.End),
                    enabled = schedule.enabled,
                    loading = executing,
                )
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

// Codigos de dia que el cliente web persiste en la metadata de la rutina ("lu".."do").
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

private fun dayShortLabel(context: Context, day: String): String =
    dayLabelRes(day)?.let { context.getString(it).take(2) }
        ?: day.take(2).replaceFirstChar { it.uppercaseChar() }

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

// Lista de dias vacia significa que la rutina corre todos los dias.
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
