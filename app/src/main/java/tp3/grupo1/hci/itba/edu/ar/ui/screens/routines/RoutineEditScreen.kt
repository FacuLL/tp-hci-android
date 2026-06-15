package tp3.grupo1.hci.itba.edu.ar.ui.screens.routines

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.data.model.Device
import tp3.grupo1.hci.itba.edu.ar.data.model.DeviceTypeActionParam
import tp3.grupo1.hci.itba.edu.ar.domain.deviceActionName
import tp3.grupo1.hci.itba.edu.ar.domain.deviceValueLabel
import tp3.grupo1.hci.itba.edu.ar.ui.components.CenteredLoading
import tp3.grupo1.hci.itba.edu.ar.ui.components.ErrorBanner
import tp3.grupo1.hci.itba.edu.ar.ui.components.LoadingButton
import tp3.grupo1.hci.itba.edu.ar.ui.components.LuminaTextField

// Codigos de dia que persiste la API en la metadata de la rutina, en orden semanal.
private val DAY_ORDER = listOf("lu", "ma", "mi", "ju", "vi", "sa", "do")

@StringRes
private fun dayFullRes(day: String): Int = when (day) {
    "lu" -> R.string.routines_day_monday
    "ma" -> R.string.routines_day_tuesday
    "mi" -> R.string.routines_day_wednesday
    "ju" -> R.string.routines_day_thursday
    "vi" -> R.string.routines_day_friday
    "sa" -> R.string.routines_day_saturday
    else -> R.string.routines_day_sunday
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoutineEditScreen(
    routineId: String?,
    onNavigateUp: () -> Unit,
) {
    val viewModel: RoutineEditViewModel =
        viewModel(factory = RoutineEditViewModel.factory(routineId))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.saved) {
        if (state.saved) onNavigateUp()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isEditing) R.string.routine_edit_title
                            else R.string.routine_new_title
                        )
                    )
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
        if (state.loading) {
            CenteredLoading(Modifier.padding(innerPadding))
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            state.apiErrorRes?.let { ErrorBanner(stringResource(it)) }

            LuminaTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = stringResource(R.string.routine_field_name),
                required = true,
                error = if (state.submitted && state.name.isBlank()) {
                    stringResource(R.string.validation_required)
                } else {
                    null
                },
            )

            SectionLabel(stringResource(R.string.routine_section_schedule))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !state.scheduled,
                    onClick = { viewModel.setScheduled(false) },
                    label = { Text(stringResource(R.string.routine_schedule_manual)) },
                )
                FilterChip(
                    selected = state.scheduled,
                    onClick = { viewModel.setScheduled(true) },
                    label = { Text(stringResource(R.string.routine_schedule_programmed)) },
                )
            }
            if (state.scheduled) {
                TimeField(
                    time = state.time,
                    onTimeChange = viewModel::setTime,
                    label = stringResource(R.string.routine_field_time),
                )
                SectionLabel(stringResource(R.string.routine_field_days))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DAY_ORDER.forEach { day ->
                        val short = stringResource(dayFullRes(day)).take(2)
                        FilterChip(
                            selected = day in state.days,
                            onClick = { viewModel.toggleDay(day) },
                            label = { Text(short) },
                        )
                    }
                }
                if (state.submitted && state.days.isEmpty()) {
                    Text(
                        stringResource(R.string.routine_days_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            SectionLabel(stringResource(R.string.routine_section_actions))
            Text(
                stringResource(R.string.routine_actions_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.devices.isEmpty()) {
                Text(
                    stringResource(R.string.routine_no_devices),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.actions.forEachIndexed { index, form ->
                ActionRowItem(
                    index = index,
                    form = form,
                    devices = state.devices,
                    actionOptions = viewModel.actionsForDevice(form.deviceId).map { it.name },
                    paramDefs = viewModel.paramsForAction(form.deviceId, form.actionName),
                    onDeviceChange = { viewModel.setActionDevice(index, it) },
                    onActionChange = { viewModel.setActionName(index, it) },
                    onParamChange = { pIdx, value -> viewModel.setParam(index, pIdx, value) },
                    onRemove = { viewModel.removeAction(index) },
                )
            }
            TextButton(
                onClick = viewModel::addAction,
                enabled = state.devices.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.routine_add_action))
            }
            if (state.submitted && state.actions.isEmpty()) {
                Text(
                    stringResource(R.string.routine_action_no_actions),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(4.dp))
            LoadingButton(
                text = stringResource(R.string.routine_save),
                onClick = viewModel::save,
                loading = state.saving,
                enabled = state.canSave && !state.saving && !state.saved,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionRowItem(
    index: Int,
    form: ActionForm,
    devices: List<Device>,
    actionOptions: List<String>,
    paramDefs: List<DeviceTypeActionParam>,
    onDeviceChange: (String) -> Unit,
    onActionChange: (String) -> Unit,
    onParamChange: (Int, String) -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.routine_action_number, index + 1),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.routine_cd_remove_action),
                    )
                }
            }

            DropdownField(
                label = stringResource(R.string.routine_field_device),
                selectedText = devices.firstOrNull { it.id == form.deviceId }?.name.orEmpty(),
                options = devices,
                optionText = { it.name },
                onSelect = { onDeviceChange(it.id) },
            )

            // Deshabilitada hasta elegir dispositivo.
            DropdownField(
                label = stringResource(R.string.routine_field_action),
                selectedText = form.actionName.takeIf { it.isNotBlank() }
                    ?.let { deviceActionName(context, it) }
                    .orEmpty(),
                options = actionOptions,
                optionText = { deviceActionName(context, it) },
                onSelect = onActionChange,
                enabled = form.deviceId.isNotBlank(),
            )

            paramDefs.forEachIndexed { pIdx, def ->
                ParamInput(
                    def = def,
                    value = form.params.getOrElse(pIdx) { "" },
                    onValue = { onParamChange(pIdx, it) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParamInput(
    def: DeviceTypeActionParam,
    value: String,
    onValue: (String) -> Unit,
) {
    val context = LocalContext.current
    val label = def.name ?: stringResource(R.string.routine_param_value)
    val supported = def.supportedValuesList()
    when {
        supported.isNotEmpty() -> DropdownField(
            label = label,
            selectedText = value.takeIf { it.isNotBlank() }?.let { deviceValueLabel(context, it) }.orEmpty(),
            options = supported,
            optionText = { deviceValueLabel(context, it) },
            onSelect = onValue,
        )

        def.type == "boolean" -> {
            val options = listOf("true", "false")
            DropdownField(
                label = label,
                selectedText = boolLabel(context, value.ifBlank { "false" }),
                options = options,
                optionText = { boolLabel(context, it) },
                onSelect = onValue,
            )
        }

        else -> {
            val range = if (def.isNumeric && def.minNumber != null && def.maxNumber != null) {
                stringResource(
                    R.string.routine_param_range,
                    formatNumber(def.minNumber!!),
                    formatNumber(def.maxNumber!!),
                )
            } else {
                null
            }
            LuminaTextField(
                value = value,
                onValueChange = onValue,
                label = label,
                keyboardOptions = if (def.isNumeric) {
                    KeyboardOptions(keyboardType = KeyboardType.Number)
                } else {
                    KeyboardOptions.Default
                },
                supportingText = range,
            )
        }
    }
}

private fun boolLabel(context: Context, value: String): String =
    context.getString(if (value == "true") R.string.routine_param_true else R.string.routine_param_false)

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownField(
    label: String,
    selectedText: String,
    options: List<T>,
    optionText: (T) -> String,
    onSelect: (T) -> Unit,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionText(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(
    time: String,
    onTimeChange: (String) -> Unit,
    label: String,
) {
    var showDialog by remember { mutableStateOf(false) }
    val parts = time.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    Box {
        OutlinedTextField(
            value = time,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true },
        )
    }

    if (showDialog) {
        val pickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onTimeChange("%02d:%02d".format(pickerState.hour, pickerState.minute))
                    showDialog = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            text = {
                // En landscape el TimePicker analogico se corta; se usa el TimeInput numerico que entra en un dialog corto.
                val isLandscape = LocalConfiguration.current.orientation ==
                    android.content.res.Configuration.ORIENTATION_LANDSCAPE
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (isLandscape) TimeInput(state = pickerState)
                    else TimePicker(state = pickerState)
                }
            },
        )
    }
}
