package tp3.grupo1.hci.itba.edu.ar.ui.screens.statistics

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import tp3.grupo1.hci.itba.edu.ar.ui.components.FloatingTopBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tp3.grupo1.hci.itba.edu.ar.R
import tp3.grupo1.hci.itba.edu.ar.ui.components.CenteredLoading
import tp3.grupo1.hci.itba.edu.ar.ui.components.ErrorBanner
import tp3.grupo1.hci.itba.edu.ar.ui.components.ProfileAvatar
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(onOpenSettings: () -> Unit) {
    val viewModel: StatisticsViewModel = viewModel(factory = StatisticsViewModel.Factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    StatisticsScaffold(onOpenSettings = onOpenSettings) { innerPadding ->
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
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            FloatingTopBar(
                title = { Text(stringResource(R.string.statistics_title)) },
                actions = {
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
    contentPadding: PaddingValues,
) {
    val widthClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    // Limitar y centrar: las tarjetas KPI y el grafico quedan legibles en vez de estirarse
    // de borde a borde en horizontal / tablet (el aspecto del grafico se distorsionaria).
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
            item { KpiGrid(state, widthClass) }
            item { HourlyChartCard(state.hourly) }
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
private fun KpiGrid(state: StatisticsUiState, widthClass: WindowWidthSizeClass) {
    val kwhStr = formatKwh(state.totalKwh)
    val costStr = formatCost(state.estimatedCost)
    val cards = listOf<@Composable (Modifier) -> Unit>(
        { m ->
            KpiCard(
                eyebrowRes = R.string.statistics_kpi_total_consumption,
                value = stringResource(R.string.statistics_kpi_total_consumption_value, kwhStr),
                descriptionRes = R.string.statistics_kpi_total_consumption_desc,
                delta = deltaLabel(state.deltaPct),
                deltaPositive = state.deltaPct > 0,
                modifier = m,
            )
        },
        { m ->
            KpiCard(
                eyebrowRes = R.string.statistics_kpi_estimated_cost,
                value = stringResource(R.string.statistics_kpi_estimated_cost_value, costStr),
                descriptionRes = R.string.statistics_kpi_estimated_cost_desc,
                delta = deltaLabel(state.deltaPct),
                deltaPositive = state.deltaPct > 0,
                modifier = m,
            )
        },
        { m ->
            KpiCard(
                eyebrowRes = R.string.statistics_kpi_most_active_room,
                value = state.mostActiveRoom?.let {
                    stringResource(R.string.statistics_kpi_most_active_room_value, it.roomName, it.watts)
                } ?: stringResource(R.string.statistics_kpi_most_active_room_empty),
                descriptionRes = R.string.statistics_kpi_most_active_room_desc,
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
                descriptionRes = R.string.statistics_kpi_active_devices_desc,
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
    @StringRes descriptionRes: Int,
    delta: String?,
    deltaPositive: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(eyebrowRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(descriptionRes),
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
private fun HourlyChartCard(points: List<HourPoint>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.statistics_chart_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.statistics_chart_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Altura basada en el aspecto: evita que el grafico se vea aplastado en
            // horizontal (180dp fijo quedaba muy corto en relaciones de ancho ~1.5+:1).
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val chartHeight = (maxWidth * 0.35f).coerceIn(180.dp, 280.dp)
                if (points.isEmpty() || points.all { it.kWh == 0.0 }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.statistics_chart_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LineChart(points = points, height = chartHeight)
                }
            }
        }
    }
}

@Composable
private fun LineChart(points: List<HourPoint>, height: androidx.compose.ui.unit.Dp) {
    val accent = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val fillBrush = Brush.verticalGradient(
        colors = listOf(accent.copy(alpha = 0.30f), accent.copy(alpha = 0.02f)),
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val maxKwh = (points.maxOf { it.kWh }).coerceAtLeast(0.0001)
        val yMax = niceCeiling(maxKwh)
        val gridLines = 5
        val xs = points.indices.map { i ->
            size.width * (i.toFloat() / (points.size - 1).coerceAtLeast(1))
        }
        val ys = points.map { p ->
            size.height - (p.kWh / yMax).toFloat() * size.height
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
            for (i in points.indices) lineTo(xs[i], ys[i])
            lineTo(xs.last(), size.height)
            close()
        }
        drawPath(path = areaPath, brush = fillBrush)
        val linePath = Path().apply {
            moveTo(xs.first(), ys.first())
            for (i in 1 until points.size) lineTo(xs[i], ys[i])
        }
        drawPath(
            path = linePath,
            color = accent,
            style = Stroke(width = 3.5f),
        )
        for (i in points.indices) {
            drawCircle(
                color = accent,
                radius = 3.5f,
                center = Offset(xs[i], ys[i]),
            )
        }
    }
}

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
private fun deltaLabel(deltaPct: Int): String? {
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
