package tp3.grupo1.hci.itba.edu.ar.ui.screens.statistics

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import tp3.grupo1.hci.itba.edu.ar.ui.components.FloatingTopBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowWidthSizeClass
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.ui.components.CenteredLoading
import tp3.grupo1.hci.itba.edu.ar.ui.components.ErrorBanner
import tp3.grupo1.hci.itba.edu.ar.ui.components.ProfileAvatar
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onOpenSettings: () -> Unit,
    onOpenRegistros: () -> Unit,
) {
    val viewModel: StatisticsViewModel = viewModel(factory = StatisticsViewModel.Factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    StatisticsScaffold(
        onOpenSettings = onOpenSettings,
        onOpenRegistros = onOpenRegistros,
    ) { innerPadding ->
        when {
            state.loading -> CenteredLoading()
            state.errorRes != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            ) {
                ErrorBanner(stringResource(state.errorRes!!))
            }
            else -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.refresh(manual = true) },
                modifier = Modifier.fillMaxSize(),
            ) {
                StatisticsContent(
                    state = state,
                    onSelectPeriod = viewModel::setPeriod,
                    onSetTariff = viewModel::setTariff,
                    contentPadding = innerPadding,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatisticsScaffold(
    onOpenSettings: () -> Unit,
    onOpenRegistros: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        // El outer NavigationSuiteScaffold ya consumio los insets del sistema.
        contentWindowInsets = WindowInsets(0),
        topBar = {
            FloatingTopBar(
                title = { Text(stringResource(R.string.statistics_title)) },
                actions = {
                    IconButton(onClick = onOpenRegistros) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                            contentDescription = stringResource(R.string.statistics_open_logs_cd),
                        )
                    }
                    ProfileAvatar(onClick = onOpenSettings)
                },
            )
        },
    ) { padding -> content(padding) }
}

@Composable
private fun StatisticsContent(
    state: StatisticsUiState,
    onSelectPeriod: (StatsPeriod) -> Unit,
    onSetTariff: (Double) -> Unit,
    contentPadding: PaddingValues,
) {
    val widthClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    var showTariffDialog by remember { mutableStateOf(false) }

    if (showTariffDialog) {
        TariffDialog(
            current = state.tariff,
            onDismiss = { showTariffDialog = false },
            onConfirm = {
                onSetTariff(it)
                showTariffDialog = false
            },
        )
    }

    // Limitar y centrar: las tarjetas KPI y los graficos quedan legibles en vez de estirarse
    // de borde a borde en horizontal / tablet.
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
                start = 16.dp,
                end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.statistics_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.statistics_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                PeriodSelector(selected = state.period, onSelect = onSelectPeriod)
            }
            item { KpiGrid(state, widthClass, onOpenTariff = { showTariffDialog = true }) }
            item {
                LineChartCard(
                    values = state.lineSeries,
                    labels = state.lineLabels,
                    title = chartTitle(state.period),
                    subtitle = chartSubtitle(state.period),
                )
            }
            item { RoomConsumptionCard(state.roomConsumption) }
            item { ComparisonCard(state.comparison, compareSubtitle(state.period)) }
            item { RoomComparisonCard(state.roomComparison) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSelector(
    selected: StatsPeriod,
    onSelect: (StatsPeriod) -> Unit,
) {
    val options = listOf(
        StatsPeriod.HOY to R.string.statistics_period_today,
        StatsPeriod.SEMANA to R.string.statistics_period_week,
        StatsPeriod.MES to R.string.statistics_period_month,
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (period, labelRes) ->
            SegmentedButton(
                selected = selected == period,
                onClick = { onSelect(period) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) { Text(stringResource(labelRes)) }
        }
    }
}

@Composable
private fun KpiGrid(
    state: StatisticsUiState,
    widthClass: WindowWidthSizeClass,
    onOpenTariff: () -> Unit,
) {
    val kwhStr = formatKwh(state.totalKwh)
    val costStr = formatCost(state.estimatedCost)
    val cards = listOf<@Composable (Modifier) -> Unit>(
        { m ->
            KpiCard(
                eyebrowRes = R.string.statistics_kpi_total_consumption,
                value = stringResource(R.string.statistics_kpi_total_consumption_value, kwhStr),
                description = stringResource(R.string.statistics_kpi_total_consumption_desc),
                delta = deltaLabel(state.deltaPct),
                deltaPositive = (state.deltaPct ?: 0) > 0,
                modifier = m,
            )
        },
        { m ->
            KpiCard(
                eyebrowRes = R.string.statistics_kpi_estimated_cost,
                value = stringResource(R.string.statistics_kpi_estimated_cost_value, costStr),
                description = stringResource(
                    R.string.statistics_kpi_estimated_cost_desc_tariff,
                    formatCost(state.tariff),
                ),
                delta = deltaLabel(state.deltaPct),
                deltaPositive = (state.deltaPct ?: 0) > 0,
                onSettings = onOpenTariff,
                settingsCd = stringResource(R.string.statistics_cost_settings_cd),
                modifier = m,
            )
        },
        { m ->
            KpiCard(
                eyebrowRes = R.string.statistics_kpi_most_active_room,
                value = state.mostActiveRoom?.let {
                    stringResource(R.string.statistics_kpi_most_active_room_value, it.roomName, it.watts)
                } ?: stringResource(R.string.statistics_kpi_most_active_room_empty),
                description = stringResource(R.string.statistics_kpi_most_active_room_desc),
                delta = null,
                deltaPositive = false,
                modifier = m,
            )
        },
        { m ->
            KpiCard(
                eyebrowRes = R.string.statistics_kpi_active_devices,
                value = stringResource(
                    R.string.statistics_kpi_active_devices_value,
                    state.activeDevices,
                    state.totalDevices,
                ),
                description = stringResource(R.string.statistics_kpi_active_devices_desc),
                delta = null,
                deltaPositive = false,
                modifier = m,
            )
        },
    )
    // EXPANDED usa una sola fila de 4; el resto cae a 2x2.
    if (widthClass == WindowWidthSizeClass.EXPANDED) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            cards.forEach { it(Modifier.weight(1f)) }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            cards.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { it(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    @StringRes eyebrowRes: Int,
    value: String,
    description: String,
    delta: String?,
    deltaPositive: Boolean,
    modifier: Modifier = Modifier,
    onSettings: (() -> Unit)? = null,
    settingsCd: String? = null,
) {
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(eyebrowRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (onSettings != null) {
                    IconButton(
                        onClick = onSettings,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = settingsCd,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (delta != null) {
                Text(
                    text = delta,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (deltaPositive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun TariffDialog(
    current: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var text by remember { mutableStateOf(formatCost(current)) }
    val parsed = text.replace(',', '.').toDoubleOrNull()
    val valid = parsed != null && parsed > 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.statistics_tariff_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text(stringResource(R.string.statistics_tariff_dialog_label)) },
                isError = text.isNotEmpty() && !valid,
                supportingText = if (text.isNotEmpty() && !valid) {
                    { Text(stringResource(R.string.statistics_tariff_dialog_error)) }
                } else {
                    null
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onConfirm) },
                enabled = valid,
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun chartTitle(period: StatsPeriod): String = stringResource(
    when (period) {
        StatsPeriod.HOY -> R.string.statistics_chart_title_today
        StatsPeriod.SEMANA -> R.string.statistics_chart_title_week
        StatsPeriod.MES -> R.string.statistics_chart_title_month
    },
)

@Composable
private fun chartSubtitle(period: StatsPeriod): String = stringResource(
    when (period) {
        StatsPeriod.HOY -> R.string.statistics_chart_subtitle_today
        StatsPeriod.SEMANA -> R.string.statistics_chart_subtitle_week
        StatsPeriod.MES -> R.string.statistics_chart_subtitle_month
    },
)

@Composable
private fun compareSubtitle(period: StatsPeriod): String = stringResource(
    when (period) {
        StatsPeriod.HOY -> R.string.statistics_compare_subtitle_today
        StatsPeriod.SEMANA -> R.string.statistics_compare_subtitle_week
        StatsPeriod.MES -> R.string.statistics_compare_subtitle_month
    },
)

@Composable
private fun LineChartCard(values: List<Double>, labels: List<String>, title: String, subtitle: String) {
    SectionCard(title = title, subtitle = subtitle) {
        if (values.isEmpty() || values.all { it == 0.0 }) {
            EmptyChart(180.dp, stringResource(R.string.statistics_chart_empty))
            return@SectionCard
        }
        val yMax = niceCeiling((values.maxOrNull() ?: 0.0).coerceAtLeast(0.0001))
        // Altura basada en el aspecto: evita que el grafico se vea aplastado en horizontal.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val chartHeight = (maxWidth * 0.32f).coerceIn(160.dp, 260.dp)
            ChartWithAxes(yMax = yMax, xLabels = labels, plotHeight = chartHeight) { mod ->
                LineChart(values = values, yMax = yMax, modifier = mod)
            }
        }
    }
}

// Marco comun: columna de reglas/unidades (eje Y, "X kWh") + etiquetas del eje X bajo el plot.
@Composable
private fun ChartWithAxes(
    yMax: Double,
    xLabels: List<String>,
    plotHeight: androidx.compose.ui.unit.Dp,
    plot: @Composable (Modifier) -> Unit,
) {
    Column {
        Row {
            YAxisLabels(yMax = yMax, height = plotHeight, modifier = Modifier.width(AXIS_WIDTH))
            plot(
                Modifier
                    .weight(1f)
                    .height(plotHeight),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(AXIS_WIDTH))
            XAxisLabels(labels = xLabels, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun YAxisLabels(yMax: Double, height: androidx.compose.ui.unit.Dp, modifier: Modifier) {
    Column(
        modifier = modifier
            .height(height)
            .padding(end = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End,
    ) {
        for (g in 0..CHART_GRID_LINES) {
            val v = yMax * (CHART_GRID_LINES - g) / CHART_GRID_LINES
            Text(
                text = stringResource(R.string.statistics_axis_kwh, trimKwh(v)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun XAxisLabels(labels: List<String>, modifier: Modifier) {
    // Si hay muchas etiquetas (ej. 12 bloques de 2h) se muestran salteadas para no amontonarse.
    val skip = labels.size > 8
    Row(modifier = modifier.padding(top = 4.dp)) {
        labels.forEachIndexed { i, label ->
            Text(
                text = if (!skip || i % 2 == 0) label else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// Formatea un valor de tick: entero si es redondo, si no un decimal.
private fun trimKwh(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

@Composable
private fun RoomConsumptionCard(rooms: List<RoomUsage>) {
    SectionCard(
        title = stringResource(R.string.statistics_rooms_title),
        subtitle = stringResource(R.string.statistics_rooms_subtitle),
    ) {
        if (rooms.isEmpty()) {
            Text(
                text = stringResource(R.string.statistics_rooms_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val total = rooms.sumOf { it.watts }.coerceAtLeast(1)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rooms.forEach { room ->
                    val fraction = room.watts.toFloat() / total
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row {
                            Text(
                                text = room.roomName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${(fraction * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonCard(points: List<ComparePoint>, subtitle: String) {
    SectionCard(
        title = stringResource(R.string.statistics_compare_title),
        subtitle = subtitle,
    ) {
        if (points.isEmpty() || points.all { it.current == 0.0 && it.previous == 0.0 }) {
            EmptyChart(180.dp, stringResource(R.string.statistics_chart_empty))
            return@SectionCard
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(MaterialTheme.colorScheme.primary, stringResource(R.string.statistics_compare_current))
            LegendDot(
                MaterialTheme.colorScheme.surfaceVariant,
                stringResource(R.string.statistics_compare_previous),
            )
        }
        val currentColor = MaterialTheme.colorScheme.primary
        val previousColor = MaterialTheme.colorScheme.surfaceVariant
        val yMax = niceCeiling(points.maxOf { maxOf(it.current, it.previous) }.coerceAtLeast(0.0001))
        ChartWithAxes(yMax = yMax, xLabels = points.map { it.label }, plotHeight = 160.dp) { mod ->
            BarChart(points = points, yMax = yMax, current = currentColor, previous = previousColor, modifier = mod)
        }
    }
}

@Composable
private fun BarChart(
    points: List<ComparePoint>,
    yMax: Double,
    current: Color,
    previous: Color,
    modifier: Modifier,
) {
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier.fillMaxWidth()) {
        for (g in 0..CHART_GRID_LINES) {
            val y = size.height * (g.toFloat() / CHART_GRID_LINES)
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        val groupWidth = size.width / points.size
        val barWidth = groupWidth * 0.3f
        val gap = groupWidth * 0.1f
        points.forEachIndexed { i, p ->
            val groupX = groupWidth * i + (groupWidth - 2 * barWidth - gap) / 2f
            val prevH = (p.previous / yMax).toFloat() * size.height
            val currH = (p.current / yMax).toFloat() * size.height
            drawRect(
                color = current,
                topLeft = Offset(groupX, size.height - currH),
                size = Size(barWidth, currH),
            )
            drawRect(
                color = previous,
                topLeft = Offset(groupX + barWidth + gap, size.height - prevH),
                size = Size(barWidth, prevH),
            )
        }
    }
}

@Composable
private fun RoomComparisonCard(rooms: List<RoomCompare>) {
    SectionCard(
        title = stringResource(R.string.statistics_room_compare_title),
        subtitle = stringResource(R.string.statistics_room_compare_subtitle),
    ) {
        if (rooms.isEmpty()) {
            Text(
                text = stringResource(R.string.statistics_room_compare_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val currentColor = MaterialTheme.colorScheme.primary
            val previousColor = MaterialTheme.colorScheme.surfaceVariant
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                rooms.forEach { room ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = room.roomName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = deltaLabel(room.deltaPct) ?: stringResource(R.string.statistics_delta_zero),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    room.deltaPct > 0 -> MaterialTheme.colorScheme.error
                                    room.deltaPct < 0 -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        CompareBar(room.actualPct / 100f, currentColor)
                        CompareBar(room.anteriorPct / 100f, previousColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareBar(fraction: Float, color: Color) {
    LinearProgressIndicator(
        progress = { fraction },
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp),
    )
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp)),
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun EmptyChart(height: androidx.compose.ui.unit.Dp, message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LineChart(values: List<Double>, yMax: Double, modifier: Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val fillBrush = Brush.verticalGradient(
        colors = listOf(accent.copy(alpha = 0.30f), accent.copy(alpha = 0.02f)),
    )
    Canvas(modifier = modifier.fillMaxWidth()) {
        val gridLines = CHART_GRID_LINES
        // Puntos centrados en su franja (estilo eje de categorias) para alinear con las etiquetas X.
        val xs = values.indices.map { i ->
            size.width * (i + 0.5f) / values.size
        }
        val ys = values.map { v ->
            size.height - (v / yMax).toFloat() * size.height
        }
        for (g in 0..gridLines) {
            val y = size.height * (g.toFloat() / gridLines)
            drawLine(
                color = grid,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
        }
        val areaPath = Path().apply {
            moveTo(xs.first(), size.height)
            for (i in values.indices) lineTo(xs[i], ys[i])
            lineTo(xs.last(), size.height)
            close()
        }
        drawPath(path = areaPath, brush = fillBrush)
        val linePath = Path().apply {
            moveTo(xs.first(), ys.first())
            for (i in 1 until values.size) lineTo(xs[i], ys[i])
        }
        drawPath(
            path = linePath,
            color = accent,
            style = Stroke(width = 3.5f),
        )
        for (i in values.indices) {
            drawCircle(
                color = accent,
                radius = 3.5f,
                center = Offset(xs[i], ys[i]),
            )
        }
    }
}

// Ancho reservado para las etiquetas del eje Y; numero de divisiones horizontales del grafico.
private val AXIS_WIDTH = 44.dp
private const val CHART_GRID_LINES = 4

private fun niceCeiling(value: Double): Double {
    if (value <= 0.5) return 0.5
    if (value <= 1.0) return 1.0
    if (value <= 2.5) return 2.5
    if (value <= 5.0) return 5.0
    if (value <= 10.0) return 10.0
    var ceiling = 10.0
    while (ceiling < value) ceiling *= 2
    return ceiling
}

@Composable
private fun deltaLabel(deltaPct: Int?): String? {
    if (deltaPct == null) return null
    val pct = kotlin.math.abs(deltaPct)
    if (pct == 0) return null
    return if (deltaPct > 0) {
        stringResource(R.string.statistics_delta_up, pct)
    } else {
        stringResource(R.string.statistics_delta_down, pct)
    }
}

private fun formatKwh(value: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-AR")).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }
    return nf.format(value)
}

private fun formatCost(value: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-AR")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }
    return nf.format(value)
}
