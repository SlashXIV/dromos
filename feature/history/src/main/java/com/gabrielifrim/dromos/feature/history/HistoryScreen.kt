package com.gabrielifrim.dromos.feature.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrielifrim.dromos.core.domain.analysis.WeekSummary
import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.ui.component.EmptyState
import com.gabrielifrim.dromos.core.ui.motion.CountUp
import com.gabrielifrim.dromos.core.ui.motion.RisingText
import com.gabrielifrim.dromos.core.ui.motion.pressScale
import com.gabrielifrim.dromos.core.ui.motion.riseIn
import com.gabrielifrim.dromos.core.ui.text.Format
import com.gabrielifrim.dromos.core.ui.text.displayTitle
import com.gabrielifrim.dromos.core.ui.text.labelRes
import com.gabrielifrim.dromos.core.ui.theme.DromosTheme
import com.gabrielifrim.dromos.core.ui.visual.ContourField
import com.gabrielifrim.dromos.core.ui.visual.Rule
import com.gabrielifrim.dromos.core.ui.visual.TelemetryLabel
import com.gabrielifrim.dromos.core.ui.visual.TraceThumbnail
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.gabrielifrim.dromos.core.ui.R as UiR

@Composable
fun HistoryRoute(
    trackingActive: Boolean,
    onOpenRecord: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenActivity: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(state, trackingActive, onOpenRecord, onOpenSettings, onOpenActivity, viewModel::setFilter)
}

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    trackingActive: Boolean,
    onOpenRecord: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenActivity: (Long) -> Unit,
    onFilter: (ActivityType?) -> Unit,
) {
    val fabInteraction = remember { MutableInteractionSource() }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenRecord,
                interactionSource = fabInteraction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.pressScale(fabInteraction),
                icon = {
                    Icon(
                        if (trackingActive) Icons.AutoMirrored.Filled.DirectionsRun else Icons.Filled.PlayArrow,
                        contentDescription = null,
                    )
                },
                text = {
                    Text(
                        stringResource(if (trackingActive) R.string.history_tracking_in_progress else R.string.history_new_activity)
                            .uppercase(),
                        style = DromosTheme.type.action,
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 112.dp + padding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "entete") { Header(state.week, onOpenSettings) }
            item(key = "filtres") { FilterRow(state.filter, onFilter) }
            when {
                state.isLoading -> item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) { CircularProgressIndicator() }
                }

                state.activities.isEmpty() -> item {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.DirectionsRun,
                        title = stringResource(
                            if (state.filter == null) R.string.history_empty_title else R.string.history_empty_filtered_title,
                        ),
                        body = stringResource(
                            if (state.filter == null) R.string.history_empty_body else R.string.history_empty_filtered_body,
                        ),
                        modifier = Modifier.height(280.dp),
                    )
                }

                else -> itemsIndexed(state.activities, key = { _, it -> it.id }) { index, activity ->
                    ActivityCard(
                        activity = activity,
                        index = index,
                        onClick = { onOpenActivity(activity.id) },
                        modifier = Modifier.padding(horizontal = 16.dp).riseIn(index, baseDelayMs = 450),
                    )
                }
            }
        }
    }
}

/**
 * En-tête « affiche » posé sur les courbes de niveau vivantes : le motif
 * signature de Dromos. Le relief s'efface vers le fond pour ne jamais gêner la lecture.
 */
@Composable
private fun Header(week: WeekSummary?, onOpenSettings: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth()) {
        ContourField(
            ink = colors.onBackground,
            accent = colors.primary,
            modifier = Modifier.fillMaxWidth().height(HEADER_RELIEF),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(HEADER_RELIEF)
                .background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to colors.background)),
        )
        Column(
            modifier = Modifier.statusBarsPadding().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TelemetryLabel(
                    week?.let { stringResource(R.string.history_eyebrow_week, it.weekNumber) }
                        ?: stringResource(R.string.history_eyebrow),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, stringResource(R.string.history_open_settings))
                }
            }
            RisingText(
                text = stringResource(R.string.history_title).uppercase(),
                style = DromosTheme.type.poster,
                color = colors.onBackground,
            )
            if (week != null) WeekStrip(week)
        }
    }
}

private val HEADER_RELIEF = 300.dp

@Composable
private fun WeekStrip(week: WeekSummary) {
    val locale = LocalConfiguration.current.locales[0]
    val figure = DromosTheme.type.metricLarge.copy(fontSize = DromosTheme.type.metric.fontSize * 1.5f)
    Column(Modifier.riseIn(baseDelayMs = 600), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Rule(delayMs = 600)
        Row(Modifier.fillMaxWidth()) {
            WeekFigure(R.string.history_week_distance, Modifier.weight(1f)) {
                CountUp(week.distanceMeters / 1000, { String.format(locale, "%.1f", it) }, style = figure)
            }
            WeekFigure(R.string.history_week_count, Modifier.weight(1f)) {
                CountUp(week.count.toDouble(), { Math.round(it).toString() }, style = figure)
            }
            WeekFigure(R.string.history_week_gain, Modifier.weight(1f)) {
                CountUp(week.elevationGainMeters, { "+" + Math.round(it) }, style = figure, color = DromosTheme.metrics.elevation)
            }
        }
        Rule(delayMs = 700)
    }
}

@Composable
private fun WeekFigure(label: Int, modifier: Modifier, value: @Composable () -> Unit) {
    Column(modifier) {
        value()
        TelemetryLabel(stringResource(label), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FilterRow(selected: ActivityType?, onFilter: (ActivityType?) -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Pill(selected == null, stringResource(R.string.history_filter_all)) { onFilter(null) }
        ActivityType.entries.forEach { type ->
            Pill(selected == type, stringResource(type.labelRes)) { onFilter(type) }
        }
    }
}

@Composable
private fun Pill(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.onBackground,
            selectedLabelColor = MaterialTheme.colorScheme.background,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

@Composable
private fun ActivityCard(activity: Activity, index: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val type = DromosTheme.type
    val interaction = remember { MutableInteractionSource() }
    val start = activity.startedAt.atZone(ZoneId.systemDefault())
    val meta = listOf(
        DateTimeFormatter.ofPattern("EEE d MMM", locale).format(start).replace(".", ""),
        DateTimeFormatter.ofPattern("HH:mm", locale).format(start),
        stringResource(activity.type.labelRes),
    ).joinToString(" · ")
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth().pressScale(interaction),
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(76.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp)),
            ) {
                TraceThumbnail(
                    points = activity.preview.orEmpty(),
                    color = DromosTheme.metrics.trace,
                    modifier = Modifier.fillMaxSize(),
                    delayMs = 500 + 60 * index.coerceAtMost(8),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    activity.displayTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TelemetryLabel(meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(stringResource(UiR.string.unit_km, Format.kilometers(activity.distanceMeters, locale)), style = type.metric)
                    Text(Format.duration(activity.movingDuration), style = type.metric)
                    Format.pace(activity.movingDuration, activity.distanceMeters)?.let {
                        Text(stringResource(UiR.string.unit_pace, it), style = type.metric, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
