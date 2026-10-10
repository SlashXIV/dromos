package com.gabrielifrim.dromos.feature.record

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.gabrielifrim.dromos.core.ui.motion.RisingText
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.domain.tracking.TrackingState
import com.gabrielifrim.dromos.core.map.TrackMap
import com.gabrielifrim.dromos.core.tracking.BatteryOptimization
import com.gabrielifrim.dromos.core.tracking.BatteryPromptDialog
import com.gabrielifrim.dromos.core.ui.component.FitText
import com.gabrielifrim.dromos.core.ui.theme.DromosTheme
import com.gabrielifrim.dromos.core.ui.visual.TelemetryLabel
import com.gabrielifrim.dromos.core.ui.text.Format
import com.gabrielifrim.dromos.core.ui.text.labelRes
import java.time.Duration
import com.gabrielifrim.dromos.core.ui.R as UiR

@Composable
fun RecordRoute(
    onBack: () -> Unit,
    onFinished: (Long?) -> Unit,
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
    var counting by remember { mutableStateOf(false) }
    var sweeping by remember { mutableStateOf(false) }
    val go = {
        onAction(RecordAction.Start)
        sweeping = true
    }
    Box(Modifier.fillMaxSize()) {
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
            ) {
                val active = state.active
                if (active == null) {
                    IdleContent(state, onAction, onGo = { if (state.countdown) counting = true else go() })
                } else {
                    ActiveContent(state, active, onAction, onStopRequest = { confirmStop = true })
                }
            }
        }
        BackHandler(enabled = counting) { counting = false }
        if (counting) {
            Countdown(onDone = { counting = false; go() }, onCancel = { counting = false })
        }
        if (sweeping) StartSweep(onEnd = { sweeping = false })
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
                // Passe à la ligne plutôt que d'écraser les libellés sur un écran étroit.
                FlowRow(horizontalArrangement = Arrangement.End) {
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
private fun IdleContent(state: RecordUiState, onAction: (RecordAction) -> Unit, onGo: () -> Unit) {
    val context = LocalContext.current
    var askBattery by remember { mutableStateOf(false) }
    // Démarrage effectif, précédé une fois de la fenêtre batterie si Android peut couper l'appli.
    val start = {
        if (!state.batteryPromptDismissed && !BatteryOptimization.isUnrestricted(context)) askBattery = true
        else onGo()
    }
    if (askBattery) {
        BatteryPromptDialog { neverAgain ->
            askBattery = false
            if (neverAgain) onAction(RecordAction.NeverAskBattery)
            onGo()
        }
    }
    // Permission déjà accordée : le GPS chauffe pendant que l'on choisit le type de sortie.
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            onAction(RecordAction.LocationGranted)
        }
    }
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
            onAction(RecordAction.LocationGranted)
            start()
        } else {
            denied = true
        }
    }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GpsTelemetry(state.gps)
        Column {
            RisingText(stringResource(R.string.record_ready_line1).uppercase(), DromosTheme.type.poster)
            RisingText(
                stringResource(R.string.record_ready_line2).uppercase(),
                DromosTheme.type.poster,
                color = MaterialTheme.colorScheme.primary,
                startDelayMs = 300,
            )
        }
        val selected = state.selectedType
        val types = ActivityType.entries
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            types.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = selected == type,
                    onClick = { onAction(RecordAction.SelectType(type)) },
                    shape = SegmentedButtonDefaults.itemShape(index, types.size),
                ) { FitText(stringResource(type.labelRes)) }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            GpsSonar(state.gps.readiness, Modifier.fillMaxSize())
            BreathingHalo(START_SIZE, Modifier.size(START_SIZE + 48.dp))
            Button(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) start() else launcher.launch(permissions.toTypedArray())
                },
                modifier = Modifier.size(START_SIZE),
                shape = CircleShape,
                contentPadding = PaddingValues(8.dp),
            ) {
                FitText(
                    stringResource(R.string.record_start).uppercase(),
                    style = DromosTheme.type.action.copy(fontSize = DromosTheme.type.action.fontSize * 1.25f),
                )
            }
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

/**
 * Écran de course. Les boutons restent toujours visibles en bas ; au-dessus, la carte prend
 * la hauteur que laissent les chiffres. Si chiffres et carte minimale ne tiennent pas
 * ensemble (petit écran, grande police, bandeau de récupération), cette partie défile au
 * lieu d'écraser la carte.
 */
@Composable
private fun ActiveContent(
    state: RecordUiState,
    active: TrackingState.Active,
    onAction: (RecordAction) -> Unit,
    onStopRequest: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < COMPACT_HEIGHT
        val gap = if (compact) 10.dp else 16.dp
        val density = LocalDensity.current
        // Hauteurs mesurées autour de la carte : elle prend le reste.
        var bannerPx by remember { mutableIntStateOf(0) }
        var belowPx by remember { mutableIntStateOf(0) }
        var controlsPx by remember { mutableIntStateOf(0) }
        val gaps = if (active.recovered) 3 else 2
        val aroundPx = (if (active.recovered) bannerPx else 0) + belowPx + controlsPx
        val mapHeight = with(density) { maxHeight - aroundPx.toDp() - gap * gaps }.coerceAtLeast(MAP_MIN_HEIGHT)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                if (active.recovered) {
                    Box(Modifier.onSizeChanged { bannerPx = it.height }) { RecoveredBanner() }
                }
                TrackMap(
                    segments = state.trace,
                    traceColor = DromosTheme.metrics.trace,
                    online = state.onlineBasemap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(mapHeight)
                        .clip(MaterialTheme.shapes.large),
                )
                Column(
                    Modifier.onSizeChanged { belowPx = it.height },
                    verticalArrangement = Arrangement.spacedBy(gap),
                ) {
                    GpsStatus(active)
                    Metrics(active, state.movingDuration, compact, gap)
                }
            }
            Box(Modifier.onSizeChanged { controlsPx = it.height }) {
                Controls(active, onAction, onStopRequest)
            }
        }
    }
}

private val START_SIZE = 184.dp

/** En dessous, la carte et les chiffres pleine taille ne tiennent plus ensemble. */
private val COMPACT_HEIGHT = 600.dp

/** Plus petite, la carte ne montre plus assez de tracé pour s'orienter. */
private val MAP_MIN_HEIGHT = 140.dp

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
    TelemetryLabel(text, color = color)
}

@Composable
private fun Metrics(active: TrackingState.Active, moving: Duration, compact: Boolean, gap: Dp) {
    val locale = LocalConfiguration.current.locales[0]
    val size = if (compact) 38.sp else 48.sp
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        BigMetric(
            label = stringResource(R.string.record_metric_duration),
            value = Format.duration(moving),
            size = if (compact) 68.sp else 88.sp,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigMetric(
                label = stringResource(R.string.record_metric_distance),
                value = Format.kilometers(active.distanceMeters, locale),
                unit = stringResource(R.string.record_unit_km),
                modifier = Modifier.weight(1f),
                size = size,
            )
            BigMetric(
                label = stringResource(R.string.record_metric_avg_pace),
                value = Format.pace(moving, active.distanceMeters) ?: PLACEHOLDER,
                unit = stringResource(R.string.record_unit_pace),
                modifier = Modifier.weight(1f),
                size = size,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigMetric(
                label = stringResource(R.string.record_metric_current_pace),
                value = active.currentSpeedMetersPerSecond?.let { Format.paceFromSpeed(it) } ?: PLACEHOLDER,
                unit = stringResource(R.string.record_unit_pace),
                modifier = Modifier.weight(1f),
                size = size,
            )
            BigMetric(
                label = stringResource(R.string.record_metric_elevation),
                value = Format.meters(active.elevationGainMeters),
                unit = stringResource(R.string.record_unit_meters),
                modifier = Modifier.weight(1f),
                size = size,
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
    size: TextUnit = 48.sp,
) {
    Column(modifier) {
        // Sur une ligne : un libellé qui passe à la ligne décalerait les chiffres de la rangée.
        FitText(label.uppercase(), style = DromosTheme.type.telemetry, color = MaterialTheme.colorScheme.onSurfaceVariant, minFontSize = 8.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            FitText(
                value,
                // Barlow Condensed tabulaire : le chrono ne fait pas danser la ligne à chaque seconde.
                style = DromosTheme.type.metricLarge.copy(fontSize = size),
                minFontSize = size * 0.5f,
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
        val button = Modifier.weight(1f).heightIn(min = 64.dp)
        if (active.paused) {
            Button(onClick = { onAction(RecordAction.Resume) }, modifier = button, shape = CircleShape, contentPadding = ControlPadding) {
                ControlLabel(Icons.Filled.PlayArrow, stringResource(R.string.record_resume))
            }
        } else {
            FilledTonalButton(onClick = { onAction(RecordAction.Pause) }, modifier = button, shape = CircleShape, contentPadding = ControlPadding) {
                ControlLabel(Icons.Filled.Pause, stringResource(R.string.record_pause))
            }
        }
        // Terminer reste secondaire : un geste de fin ne doit pas ressembler au geste principal.
        OutlinedButton(
            onClick = onStopRequest,
            modifier = button,
            shape = CircleShape,
            contentPadding = ControlPadding,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        ) {
            ControlLabel(Icons.Filled.Stop, stringResource(R.string.record_stop))
        }
    }
}

/**
 * Icône et libellé d'un bouton de demi-largeur. Syne en capitales est large : sur un
 * petit écran ou avec une grande police système, le libellé rétrécit plutôt que de se
 * couper en plein mot (« REPR ENDR E »).
 */
@Composable
private fun ControlLabel(icon: ImageVector, label: String) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
    FitText(label.uppercase(), Modifier.padding(start = 6.dp), style = DromosTheme.type.action, minFontSize = 8.sp)
}

/** Marges latérales réduites : la place va au libellé, pas au vide autour. */
private val ControlPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

private const val PLACEHOLDER = "–:––" // texte-technique : tiret d'attente, pas une phrase
