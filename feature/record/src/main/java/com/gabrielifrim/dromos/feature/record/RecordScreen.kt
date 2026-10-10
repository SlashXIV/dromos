package com.gabrielifrim.dromos.feature.record

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.launch
import com.gabrielifrim.dromos.core.ui.motion.RollingDigits
import com.gabrielifrim.dromos.core.map.TraceCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.clickable
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
        KmBanner(
            state.kmBanner,
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
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
 * Écran de course en deux pages : les chiffres, puis la carte plein écran. Les boutons
 * restent en bas des deux pages. Sur la page des chiffres, la miniature du tracé prend la
 * hauteur qui reste ; si rien ne tient (petit écran, grande police), la page défile.
 * Une seule carte MapLibre, chargée seulement quand on glisse vers elle.
 */
@Composable
private fun ActiveContent(
    state: RecordUiState,
    active: TrackingState.Active,
    onAction: (RecordAction) -> Unit,
    onStopRequest: () -> Unit,
) {
    val pager = rememberPagerState { PAGES }
    val scope = rememberCoroutineScope()
    val showPage: (Int) -> Unit = { page -> scope.launch { pager.animateScrollToPage(page) } }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalPager(pager, Modifier.weight(1f), key = { it }) { page ->
            if (page == 0) {
                NumbersPage(state, active, onShowMap = { showPage(1) })
            } else {
                TrackMap(
                    segments = state.trace,
                    traceColor = DromosTheme.metrics.trace,
                    online = state.onlineBasemap,
                    modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.large),
                )
            }
        }
        // La carte capte les glissements : les points ramènent aussi aux chiffres.
        val switchLabel = stringResource(R.string.record_switch_page)
        PageDots(
            PAGES,
            pager.currentPage,
            Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(onClickLabel = switchLabel) { showPage(1 - pager.currentPage) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Controls(active, onAction, onStopRequest)
    }
}

@Composable
private fun NumbersPage(state: RecordUiState, active: TrackingState.Active, onShowMap: () -> Unit) {
    val stopped = active.paused || active.autoPaused
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < COMPACT_HEIGHT
        val gap = if (compact) 10.dp else 16.dp
        val density = LocalDensity.current
        // Hauteur de tout ce qui précède la miniature : elle prend le reste.
        var abovePx by remember { mutableIntStateOf(0) }
        val cardHeight = with(density) { maxHeight - abovePx.toDp() - gap }.coerceAtLeast(MAP_MIN_HEIGHT)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(gap)) {
            Column(Modifier.onSizeChanged { abovePx = it.height }, verticalArrangement = Arrangement.spacedBy(gap)) {
                if (active.recovered) RecoveredBanner()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { RecIndicator(stopped) }
                    GpsStatus(active)
                }
                if (active.autoPaused) {
                    TelemetryLabel(stringResource(R.string.record_status_auto_paused), color = MaterialTheme.colorScheme.tertiary)
                }
                Metrics(active, state.movingDuration, compact, gap, Modifier.desaturate(stopped))
            }
            TraceCanvas(
                segments = state.trace,
                traceColor = DromosTheme.metrics.trace,
                label = stringResource(R.string.record_swipe_to_map).uppercase(),
                pulse = !stopped,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .clip(MaterialTheme.shapes.large)
                    .clickable(onClickLabel = stringResource(R.string.record_switch_page), onClick = onShowMap)
                    .desaturate(stopped),
            )
        }
    }
}

private const val PAGES = 2

private val START_SIZE = 184.dp

/** En dessous, la carte et les chiffres pleine taille ne tiennent plus ensemble. */
private val COMPACT_HEIGHT = 600.dp

/** Plus petite, la miniature ne montre plus assez de tracé pour s'orienter. */
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

/** État du signal seulement : la pause, elle, est dite par [RecIndicator]. */
@Composable
private fun GpsStatus(active: TrackingState.Active) {
    val text = when {
        active.paused -> return
        active.locationError -> stringResource(R.string.record_status_gps_error)
        active.lastFixAt == null -> stringResource(R.string.record_status_searching)
        else -> active.accuracyMeters?.let { stringResource(R.string.record_status_accuracy, it.toInt()) }
            ?: stringResource(R.string.record_status_ok)
    }
    val color = if (active.locationError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    TelemetryLabel(text, color = color)
}

@Composable
private fun Metrics(active: TrackingState.Active, moving: Duration, compact: Boolean, gap: Dp, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val size = if (compact) 34.sp else 44.sp
    val average = if (active.distanceMeters >= 10) moving.seconds * 1000.0 / active.distanceMeters else null
    Column(modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        BigMetric(
            label = stringResource(R.string.record_metric_duration),
            value = Format.duration(moving),
            size = if (compact) 72.sp else DromosTheme.type.metricHuge.fontSize,
            rolling = true,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigMetric(
                label = stringResource(R.string.record_metric_distance),
                value = Format.kilometers(active.distanceMeters, locale),
                unit = stringResource(R.string.record_unit_km),
                modifier = Modifier.weight(1.2f),
                size = size,
                color = MaterialTheme.colorScheme.primary,
                rolling = true,
            )
            BigMetric(
                label = stringResource(R.string.record_metric_current_pace),
                value = active.currentSpeedMetersPerSecond?.let { Format.paceFromSpeed(it) } ?: PLACEHOLDER,
                unit = stringResource(R.string.record_unit_pace),
                modifier = Modifier.weight(1.2f),
                size = size,
            )
            BigMetric(
                label = stringResource(R.string.record_metric_elevation),
                value = Format.meters(active.elevationGainMeters),
                unit = stringResource(R.string.record_unit_meters),
                modifier = Modifier.weight(0.8f),
                size = size,
            )
        }
        PaceGauge(secondsPerKm(active.currentSpeedMetersPerSecond), average)
    }
}

@Composable
private fun BigMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    size: TextUnit = 48.sp,
    color: Color = Color.Unspecified,
    rolling: Boolean = false,
) {
    Column(modifier) {
        // Sur une ligne : un libellé qui passe à la ligne décalerait les chiffres de la rangée.
        FitText(label.uppercase(), style = DromosTheme.type.telemetry, color = MaterialTheme.colorScheme.onSurfaceVariant, minFontSize = 8.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            // Barlow Condensed tabulaire : le chrono ne fait pas danser la ligne à chaque seconde.
            val style = DromosTheme.type.metricLarge.copy(fontSize = size)
            val valueModifier = Modifier.weight(1f, fill = false)
            if (rolling) {
                RollingDigits(value, style, valueModifier, color = color)
            } else {
                FitText(value, valueModifier, style = style, color = color, minFontSize = size * 0.5f)
            }
            if (unit != null) {
                Text(
                    unit,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
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
