package com.gabrielifrim.dromos.feature.record

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.tracking.TrackingState
import com.gabrielifrim.dromos.core.ui.text.Format
import com.gabrielifrim.dromos.core.ui.text.labelRes
import java.time.Duration
import com.gabrielifrim.dromos.core.ui.R as UiR

@Composable
fun RecordRoute(
    onBack: () -> Unit,
    onFinished: () -> Unit,
    viewModel: RecordViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RecordScreen(
        state = state,
        onAction = { viewModel.onAction(it, onFinished) },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    state: RecordUiState,
    onAction: (RecordAction) -> Unit,
    onBack: () -> Unit,
) {
    var confirmStop by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val active = state.active
                    Text(
                        if (active != null) stringResource(active.type.labelRes)
                        else stringResource(R.string.record_title),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.record_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val active = state.active
            if (active == null) {
                IdleContent(state.selectedType, onAction)
            } else {
                if (active.recovered) RecoveredBanner()
                GpsStatus(active)
                Metrics(active, state.movingDuration)
                Spacer(Modifier.weight(1f))
                Controls(active, onAction, onStopRequest = { confirmStop = true })
            }
        }
    }
    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text(stringResource(R.string.record_stop_title)) },
            text = { Text(stringResource(R.string.record_stop_body)) },
            confirmButton = {
                TextButton(onClick = { confirmStop = false; onAction(RecordAction.Stop) }) {
                    Text(stringResource(R.string.record_stop_confirm))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { confirmStop = false; onAction(RecordAction.Discard) }) {
                        Text(stringResource(R.string.record_discard), color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { confirmStop = false }) { Text(stringResource(R.string.record_stop_cancel)) }
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IdleContent(selected: ActivityType, onAction: (RecordAction) -> Unit) {
    val context = LocalContext.current
    val permissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        // La position précise est indispensable ; la notification ne l'est pas.
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            denied = false
            onAction(RecordAction.Start)
        } else {
            denied = true
        }
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val types = ActivityType.entries
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            types.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = selected == type,
                    onClick = { onAction(RecordAction.SelectType(type)) },
                    shape = SegmentedButtonDefaults.itemShape(index, types.size),
                ) { Text(stringResource(type.labelRes)) }
            }
        }
        Button(
            onClick = {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) onAction(RecordAction.Start) else launcher.launch(permissions.toTypedArray())
            },
            modifier = Modifier.size(160.dp),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Text(stringResource(R.string.record_start), style = MaterialTheme.typography.titleLarge)
        }
        if (denied) {
            Text(
                stringResource(R.string.record_permission_denied),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RecoveredBanner() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Text(
            stringResource(R.string.record_recovered),
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
private fun GpsStatus(active: TrackingState.Active) {
    val text = when {
        active.paused -> stringResource(R.string.record_status_paused)
        active.autoPaused -> stringResource(R.string.record_status_auto_paused)
        active.locationError -> stringResource(R.string.record_status_gps_error)
        active.lastFixAt == null -> stringResource(R.string.record_status_searching)
        else -> active.accuracyMeters?.let { stringResource(R.string.record_status_accuracy, it.toInt()) }
            ?: stringResource(R.string.record_status_ok)
    }
    val color = when {
        active.locationError -> MaterialTheme.colorScheme.error
        active.paused || active.autoPaused -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text, color = color, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun Metrics(active: TrackingState.Active, moving: Duration) {
    val locale = LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        BigMetric(
            label = stringResource(R.string.record_metric_duration),
            value = Format.duration(moving),
            size = 64.sp,
        )
        Row(Modifier.fillMaxWidth()) {
            BigMetric(
                label = stringResource(R.string.record_metric_distance),
                value = Format.kilometers(active.distanceMeters, locale),
                unit = stringResource(R.string.record_unit_km),
                modifier = Modifier.weight(1f),
            )
            BigMetric(
                label = stringResource(R.string.record_metric_avg_pace),
                value = Format.pace(moving, active.distanceMeters) ?: PLACEHOLDER,
                unit = stringResource(R.string.record_unit_pace),
                modifier = Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth()) {
            BigMetric(
                label = stringResource(R.string.record_metric_current_pace),
                value = active.currentSpeedMetersPerSecond?.let { Format.paceFromSpeed(it) } ?: PLACEHOLDER,
                unit = stringResource(R.string.record_unit_pace),
                modifier = Modifier.weight(1f),
            )
            BigMetric(
                label = stringResource(R.string.record_metric_elevation),
                value = Format.meters(active.elevationGainMeters),
                unit = stringResource(R.string.record_unit_meters),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BigMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    size: TextUnit = 40.sp,
) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                fontSize = size,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                // Chiffres à chasse fixe : le chrono ne fait pas danser la ligne à chaque seconde.
                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                modifier = Modifier.weight(1f, fill = false),
            )
            if (unit != null) {
                Text(
                    unit,
                    modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun Controls(active: TrackingState.Active, onAction: (RecordAction) -> Unit, onStopRequest: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (active.paused) {
            Button(onClick = { onAction(RecordAction.Resume) }, modifier = Modifier.weight(1f).height(64.dp)) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(stringResource(R.string.record_resume), Modifier.padding(start = 8.dp))
            }
        } else {
            FilledTonalButton(onClick = { onAction(RecordAction.Pause) }, modifier = Modifier.weight(1f).height(64.dp)) {
                Icon(Icons.Filled.Pause, contentDescription = null)
                Text(stringResource(R.string.record_pause), Modifier.padding(start = 8.dp))
            }
        }
        // Terminer reste secondaire : un geste de fin ne doit pas ressembler au geste principal.
        OutlinedButton(
            onClick = onStopRequest,
            modifier = Modifier.weight(1f).height(64.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        ) {
            Icon(Icons.Filled.Stop, contentDescription = null)
            Text(stringResource(R.string.record_stop), Modifier.padding(start = 8.dp))
        }
    }
}

private const val PLACEHOLDER = "–:––" // texte-technique : tiret d'attente, pas une phrase
