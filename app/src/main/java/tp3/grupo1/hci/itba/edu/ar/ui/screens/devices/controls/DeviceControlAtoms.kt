package tp3.grupo1.hci.itba.edu.ar.ui.screens.devices.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.domain.AlarmAtom
import tp3.grupo1.hci.itba.edu.ar.domain.AlarmStatusAtom
import tp3.grupo1.hci.itba.edu.ar.domain.ButtonAtom
import tp3.grupo1.hci.itba.edu.ar.domain.ChangeCodeAtom
import tp3.grupo1.hci.itba.edu.ar.domain.ColorAtom
import tp3.grupo1.hci.itba.edu.ar.domain.ControlAtom
import tp3.grupo1.hci.itba.edu.ar.domain.DispenseAtom
import tp3.grupo1.hci.itba.edu.ar.domain.LampPreviewAtom
import tp3.grupo1.hci.itba.edu.ar.domain.PlaybackAtom
import tp3.grupo1.hci.itba.edu.ar.domain.PlaylistAtom
import tp3.grupo1.hci.itba.edu.ar.domain.PowerAtom
import tp3.grupo1.hci.itba.edu.ar.data.model.Room
import tp3.grupo1.hci.itba.edu.ar.domain.SelectAtom
import tp3.grupo1.hci.itba.edu.ar.domain.SelectKind
import tp3.grupo1.hci.itba.edu.ar.domain.SetLocationAtom
import tp3.grupo1.hci.itba.edu.ar.domain.SliderAtom
import tp3.grupo1.hci.itba.edu.ar.domain.deviceActionNameRes
import tp3.grupo1.hci.itba.edu.ar.domain.deviceValueLabel
import tp3.grupo1.hci.itba.edu.ar.ui.screens.devices.DeviceDetailDialog
import kotlin.math.roundToInt

private const val MAX_SEGMENTED_OPTIONS = 4

@Composable
internal fun ControlAtomCard(
    atom: ControlAtom,
    accent: Color,
    deviceStatus: String?,
    deviceLock: String?,
    dispensing: Boolean,
    rooms: List<Room>,
    onExecute: (String, List<JsonElement>) -> Unit,
    onDispense: (String, Int, String) -> Unit,
    onOpenDialog: (DeviceDetailDialog) -> Unit,
) {
    when (atom) {
        is LampPreviewAtom -> LampPreviewControl(atom)
        is PowerAtom -> PowerControl(atom, accent, deviceStatus, deviceLock, onExecute)
        is SliderAtom -> SliderControl(atom, accent, onExecute)
        is SelectAtom -> SelectControl(atom, onExecute)
        is ColorAtom -> ColorControl(atom, onExecute)
        is ButtonAtom -> ButtonControl(atom, onExecute)
        is AlarmStatusAtom -> AlarmStatusControl(atom)
        is AlarmAtom -> AlarmControl(atom, deviceStatus, onOpenDialog)
        ChangeCodeAtom -> ChangeCodeControl(onOpenDialog)
        is DispenseAtom -> DispenseControl(atom, accent, deviceStatus, dispensing, onDispense)
        is PlaybackAtom -> PlaybackControl(atom, accent, onExecute)
        is PlaylistAtom -> PlaylistControl(onOpenDialog)
        is SetLocationAtom -> SetLocationControl(atom, rooms, onExecute)
    }
}

@Composable
private fun SetLocationControl(
    atom: SetLocationAtom,
    rooms: List<Room>,
    onExecute: (String, List<JsonElement>) -> Unit,
) {
    val placeholder = stringResource(R.string.device_detail_set_location_placeholder)
    ControlCard {
        SectionLabel(stringResource(R.string.device_detail_set_location))
        if (rooms.isEmpty()) {
            Text(
                text = stringResource(R.string.devices_assign_no_rooms),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            // La API no expone la ubicacion del vacuum, asi que solo recordamos la seleccion de esta pantalla.
            var selectedRoomId by remember { mutableStateOf<String?>(null) }
            DropdownSelect(
                label = stringResource(R.string.device_detail_set_location),
                options = rooms.map { it.id },
                value = selectedRoomId.orEmpty(),
                optionLabel = { id -> rooms.firstOrNull { it.id == id }?.name ?: placeholder },
                onSelect = { roomId ->
                    selectedRoomId = roomId
                    onExecute(atom.action, listOf(JsonPrimitive(roomId)))
                },
            )
        }
    }
}

@Composable
internal fun ControlCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun actionLabel(action: String): String =
    deviceActionNameRes(action)?.let { stringResource(it) } ?: action

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DropdownSelect(
    label: String,
    options: List<String>,
    value: String,
    optionLabel: (String) -> String,
    onSelect: (String) -> Unit,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
    ) {
        OutlinedTextField(
            value = optionLabel(value),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

internal fun parseHexColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: IllegalArgumentException) {
    Color.White
}

@Composable
private fun PowerControl(
    atom: PowerAtom,
    accent: Color,
    deviceStatus: String?,
    deviceLock: String?,
    onExecute: (String, List<JsonElement>) -> Unit,
) {
    // No se puede abrir/cerrar mientras esta bloqueado, ni bloquear mientras esta abierto.
    val togglesOpening = atom.onAction == "open" || atom.offAction == "open"
    val blocked = (togglesOpening && deviceLock == "locked") ||
        (atom.onAction == "lock" && deviceStatus == "opened")
    ControlCard {
        atom.sectionLabelRes?.let { SectionLabel(stringResource(it)) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(if (atom.active) atom.activeLabelRes else atom.inactiveLabelRes),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = atom.active,
                enabled = !blocked,
                onCheckedChange = { checked ->
                    onExecute(if (checked) atom.onAction else atom.offAction, emptyList())
                },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = accent,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    // El track off por defecto se confunde con la card, asi que forzamos contraste.
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
    }
}

@Composable
private fun SliderControl(
    atom: SliderAtom,
    accent: Color,
    onExecute: (String, List<JsonElement>) -> Unit,
) {
    val label = atom.labelRes?.let { stringResource(it) } ?: atom.rawName
    // Valor local mientras se arrastra; la API solo se llama al soltar y los updates externos se ignoran durante el drag.
    var sliderValue by remember { mutableFloatStateOf(atom.value.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(atom.value) {
        if (!dragging) sliderValue = atom.value.toFloat()
    }
    ControlCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${sliderValue.roundToInt()}${atom.unit}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = accent,
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = {
                dragging = true
                sliderValue = it
            },
            valueRange = atom.min.toFloat()..atom.max.toFloat(),
            steps = (atom.max - atom.min - 1).coerceAtLeast(0),
            onValueChangeFinished = {
                dragging = false
                onExecute(atom.action, listOf(JsonPrimitive(sliderValue.roundToInt())))
            },
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SelectControl(
    atom: SelectAtom,
    onExecute: (String, List<JsonElement>) -> Unit,
) {
    val context = LocalContext.current
    val label = atom.labelRes?.let { stringResource(it) } ?: atom.rawName
    ControlCard {
        when (atom.kind) {
            SelectKind.MODE -> {
                SectionLabel(label)
                if (atom.options.size <= MAX_SEGMENTED_OPTIONS) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        atom.options.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = option == atom.value,
                                onClick = { onExecute(atom.action, listOf(JsonPrimitive(option))) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = atom.options.size),
                            ) {
                                Text(deviceValueLabel(context, option), maxLines = 1)
                            }
                        }
                    }
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        atom.options.forEach { option ->
                            FilterChip(
                                selected = option == atom.value,
                                onClick = { onExecute(atom.action, listOf(JsonPrimitive(option))) },
                                label = { Text(deviceValueLabel(context, option)) },
                            )
                        }
                    }
                }
            }
            SelectKind.FAN_SPEED -> {
                SectionLabel(label)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    atom.options.forEach { option ->
                        FilterChip(
                            selected = option == atom.value,
                            onClick = { onExecute(atom.action, listOf(JsonPrimitive(option))) },
                            label = { Text(fanSpeedLabel(option)) },
                        )
                    }
                }
            }
            SelectKind.SWING_VERTICAL, SelectKind.SWING_HORIZONTAL -> {
                SectionLabel(label)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    atom.options.forEach { option ->
                        FilterChip(
                            selected = option == atom.value,
                            onClick = { onExecute(atom.action, listOf(JsonPrimitive(option))) },
                            leadingIcon = { SwingOptionIcon(option, atom.kind) },
                            label = { Text(swingLabel(option)) },
                        )
                    }
                }
            }
            SelectKind.GENERIC -> {
                DropdownSelect(
                    label = label,
                    options = atom.options,
                    value = atom.value,
                    optionLabel = { deviceValueLabel(context, it) },
                    onSelect = { onExecute(atom.action, listOf(JsonPrimitive(it))) },
                )
            }
        }
    }
}

// "auto" -> "Auto"; numero crudo del supportedValue -> "NN %" (no cambia el dato enviado al API).
@Composable
private fun fanSpeedLabel(option: String): String =
    if (option == "auto") stringResource(R.string.device_value_auto_short) else "$option %"

// "auto" -> "Auto"; numero crudo -> "NN°" con sufijo de grados.
@Composable
private fun swingLabel(option: String): String =
    if (option == "auto") stringResource(R.string.device_value_auto_short) else "$option°"

// Flechita de direccion rotada al angulo (como la web): vertical parte apuntando a la derecha y
// horizontal hacia abajo, ambas rotadas por el grado; "auto" usa el icono de refresco.
@Composable
private fun SwingOptionIcon(option: String, kind: SelectKind) {
    if (option == "auto") {
        Icon(
            imageVector = Icons.Outlined.Refresh,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        return
    }
    val degrees = option.toFloatOrNull() ?: 0f
    val base = if (kind == SelectKind.SWING_HORIZONTAL) {
        Icons.Filled.ArrowDownward
    } else {
        Icons.AutoMirrored.Filled.ArrowForward
    }
    Icon(
        imageVector = base,
        contentDescription = null,
        modifier = Modifier
            .size(16.dp)
            .rotate(degrees),
    )
}

// Swatches con nombre en vez de un picker hex libre, para que cada color se anuncie por accesibilidad.
private val COLOR_SWATCHES: List<Pair<String, Int>> = listOf(
    "#FFFFFF" to R.string.device_color_white,
    "#FFE4B5" to R.string.device_color_warm_white,
    "#FFD700" to R.string.device_color_yellow,
    "#FFA500" to R.string.device_color_orange,
    "#FF4040" to R.string.device_color_red,
    "#FF69B4" to R.string.device_color_pink,
    "#9B59B6" to R.string.device_color_purple,
    "#2890D0" to R.string.device_color_blue,
    "#87CEEB" to R.string.device_color_light_blue,
    "#3CAB5E" to R.string.device_color_green,
    "#98FB98" to R.string.device_color_light_green,
    "#14B8A6" to R.string.device_color_teal,
    "#B0BEC5" to R.string.device_color_gray,
    "#C3B1E1" to R.string.device_color_lavender,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorControl(
    atom: ColorAtom,
    onExecute: (String, List<JsonElement>) -> Unit,
) {
    // El API devuelve el color inicial como nombre ("white"), asi que lo normalizamos a Color
    // (parseHexColor sabe parsear nombres) antes de compararlo con los swatches hex.
    val selectedColor = parseHexColor(atom.value)
    ControlCard {
        SectionLabel(stringResource(R.string.device_action_set_color))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            COLOR_SWATCHES.forEach { (hex, nameRes) ->
                ColorSwatch(
                    hex = hex,
                    name = stringResource(nameRes),
                    selected = parseHexColor(hex) == selectedColor,
                    onClick = { onExecute(atom.action, listOf(JsonPrimitive(hex))) },
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    hex: String,
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = parseHexColor(hex)
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
            )
        }
    }
}

@Composable
private fun LampPreviewControl(atom: LampPreviewAtom) {
    val color = parseHexColor(atom.color)
    val fill = if (atom.active) {
        color.copy(alpha = (atom.brightness / 100f).coerceIn(0.1f, 1f))
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(fill)
                // Borde sutil siempre presente: en modo claro con color blanco, sin borde el
                // circulo se funde con el fondo y no se ve donde esta.
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                ),
        )
    }
}

@Composable
private fun ButtonControl(
    atom: ButtonAtom,
    onExecute: (String, List<JsonElement>) -> Unit,
) {
    ControlCard {
        OutlinedButton(
            onClick = { onExecute(atom.action, emptyList()) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(atom.labelRes?.let { stringResource(it) } ?: atom.rawName)
        }
    }
}
