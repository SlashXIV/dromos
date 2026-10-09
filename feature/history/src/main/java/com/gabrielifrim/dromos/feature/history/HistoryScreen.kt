package com.gabrielifrim.dromos.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.ui.component.EmptyState
import com.gabrielifrim.dromos.core.ui.text.Format
import com.gabrielifrim.dromos.core.ui.text.displayTitle
import com.gabrielifrim.dromos.core.ui.text.labelRes
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    trackingActive: Boolean,
    onOpenRecord: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenActivity: (Long) -> Unit,
    onFilter: (ActivityType?) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, stringResource(R.string.history_open_settings))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenRecord,
                icon = { Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null) },
                text = {
                    Text(stringResource(if (trackingActive) R.string.history_tracking_in_progress else R.string.history_new_activity))
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            FilterRow(state.filter, onFilter)
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }

                state.activities.isEmpty() && state.filter == null -> EmptyState(
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    title = stringResource(R.string.history_empty_title),
                    body = stringResource(R.string.history_empty_body),
                )

                state.activities.isEmpty() -> EmptyState(
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    title = stringResource(R.string.history_empty_filtered_title),
                    body = stringResource(R.string.history_empty_filtered_body),
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.activities, key = { it.id }) { activity ->
                        ActivityCard(activity, onClick = { onOpenActivity(activity.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(selected: ActivityType?, onFilter: (ActivityType?) -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onFilter(null) },
            label = { Text(stringResource(R.string.history_filter_all)) },
        )
        ActivityType.entries.forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onFilter(type) },
                label = { Text(stringResource(type.labelRes)) },
            )
        }
    }
}

@Composable
private fun ActivityCard(activity: Activity, onClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(locale)
        .format(activity.startedAt.atZone(ZoneId.systemDefault()))
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = activity.displayTitle(),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(UiR.string.unit_km, Format.kilometers(activity.distanceMeters, locale)))
                Text(Format.duration(activity.movingDuration))
                Format.pace(activity.movingDuration, activity.distanceMeters)?.let {
                    Text(stringResource(UiR.string.unit_pace, it))
                }
                if (activity.elevationGainMeters >= 1) {
                    Text(stringResource(UiR.string.unit_elevation_gain, Format.meters(activity.elevationGainMeters)))
                }
            }
        }
    }
}
