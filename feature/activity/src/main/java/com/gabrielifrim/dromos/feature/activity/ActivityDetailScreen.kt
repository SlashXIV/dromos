package com.gabrielifrim.dromos.feature.activity

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import com.gabrielifrim.dromos.core.ui.motion.screenTransitionRunning
import com.gabrielifrim.dromos.core.ui.motion.sharedTrace
import com.gabrielifrim.dromos.core.ui.motion.rememberHaptics
import com.gabrielifrim.dromos.core.ui.motion.MotionLevel
import com.gabrielifrim.dromos.core.ui.motion.CountUp
import com.gabrielifrim.dromos.core.map.TraceCanvas
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.Button
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrielifrim.dromos.core.domain.analysis.Split
import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.map.TrackMap
import com.gabrielifrim.dromos.core.ui.component.FitText
import com.gabrielifrim.dromos.core.ui.text.Format
import com.gabrielifrim.dromos.core.ui.text.displayTitle
import com.gabrielifrim.dromos.core.ui.motion.LocalMotionLevel
import com.gabrielifrim.dromos.core.ui.motion.Motion
import com.gabrielifrim.dromos.core.ui.motion.RisingText
import com.gabrielifrim.dromos.core.ui.motion.riseIn
import com.gabrielifrim.dromos.core.ui.theme.DromosTheme
import com.gabrielifrim.dromos.core.ui.visual.Rule
import com.gabrielifrim.dromos.core.ui.visual.TelemetryLabel
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.gabrielifrim.dromos.core.ui.R as UiR

@Composable
fun ActivityDetailRoute(
    activityId: Long,
    justFinished: Boolean,
    onBack: () -> Unit,
    viewModel: ActivityDetailViewModel = hiltViewModel<ActivityDetailViewModel, ActivityDetailViewModel.Factory>(
        key = "activity-$activityId",
    ) { it.create(activityId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val chooserTitle = stringResource(R.string.activity_share_chooser)
    val savedMessage = stringResource(R.string.activity_export_saved)
    val failedMessage = stringResource(R.string.activity_export_failed)
    LaunchedEffect(viewModel) {
        viewModel.exports.collect { event ->
            when (event) {
                is ExportEvent.Share -> {
                    val send = Intent(Intent.ACTION_SEND)
                        .setType(event.format.mimeType)
                        .putExtra(Intent.EXTRA_STREAM, event.uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(Intent.createChooser(send, chooserTitle))
                }
                ExportEvent.Saved -> snackbar.showSnackbar(savedMessage)
                ExportEvent.Failed -> snackbar.showSnackbar(failedMessage)
            }
        }
    }
    ActivityDetailScreen(state, justFinished, snackbar, viewModel::onAction, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    state: ActivityDetailUiState,
    justFinished: Boolean,
    snackbar: SnackbarHostState,
    onAction: (ActivityDetailAction) -> Unit,
    onBack: () -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var exportMenu by remember { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    // Curseur des profils : un échantillon, retrouvé sur les graphiques et sur la carte.
    var cursor by rememberSaveable { mutableStateOf<Int?>(null) }
    val activity = state.activity
    // Séquence d'arrivée : jouée une fois, interrompue d'un appui. Couper le mouvement
    // met chaque brique (titre, compteurs, barres) directement dans son état final.
    var settled by rememberSaveable { mutableStateOf(!justFinished) }
    val ambient = LocalMotionLevel.current
    var sweeping by remember { mutableStateOf(!settled && ambient.decorative) }
    val haptics = rememberHaptics()
    val exportName = activity?.displayTitle()
    val saveLaunchers = ExportFormat.entries.associateWith { format ->
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(format.mimeType)) { uri ->
            if (uri != null && exportName != null) onAction(ActivityDetailAction.Export(format, exportName, uri))
        }
    }
    LaunchedEffect(activity != null) {
        if (activity == null || settled) return@LaunchedEffect
        delay(if (ambient.decorative) SWEEP_MS.toLong() else 0L)
        haptics.confirm()
        delay(FINISH_SEQUENCE_MS)
        settled = true
    }
    CompositionLocalProvider(LocalMotionLevel provides if (settled && justFinished) MotionLevel.NONE else ambient) {
        Box(Modifier.fillMaxSize().onAnyTouch(enabled = !settled) { settled = true; sweeping = false }) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    if (justFinished) {
                        Button(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).heightIn(min = 56.dp),
                            shape = CircleShape,
                        ) {
                            FitText(stringResource(R.string.activity_done).uppercase(), style = DromosTheme.type.action)
                        }
                    }
                },
                topBar = {
                    TopAppBar(
                        // Le titre vit en affiche dans la page ; la barre ne garde que les actions.
                        title = {},
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.activity_back))
                            }
                        },
                        actions = {
                            Box {
                                // Les points doivent être chargés : l'analyse l'atteste.
                                IconButton(onClick = { exportMenu = true }, enabled = activity != null && state.analysis != null) {
                                    Icon(Icons.Outlined.Share, stringResource(R.string.activity_export))
                                }
                                DropdownMenu(expanded = exportMenu, onDismissRequest = { exportMenu = false }) {
                                    if (activity != null && exportName != null) {
                                        ExportFormat.entries.forEach { format ->
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.activity_share_as, format.name)) },
                                                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                                                onClick = {
                                                    exportMenu = false
                                                    onAction(ActivityDetailAction.Share(format, exportName))
                                                },
                                            )
                                        }
                                        ExportFormat.entries.forEach { format ->
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.activity_save_as, format.name)) },
                                                leadingIcon = { Icon(Icons.Outlined.SaveAlt, contentDescription = null) },
                                                onClick = {
                                                    exportMenu = false
                                                    saveLaunchers.getValue(format).launch(exportFileName(activity, format))
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                            IconButton(onClick = { editing = true }, enabled = activity != null) {
                                Icon(Icons.Outlined.Edit, stringResource(R.string.activity_edit))
                            }
                            IconButton(onClick = { confirmDelete = true }, enabled = activity != null) {
                                Icon(Icons.Outlined.Delete, stringResource(R.string.activity_delete))
                            }
                        },
                    )
                },
            ) { padding ->
                if (activity == null) {
                    Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
                    return@Scaffold
                }
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item { if (justFinished) FinishHeader(activity) else PosterTitle(activity) }
                    val profile = state.analysis?.profile.orEmpty()
                    val cursorPoint = cursor?.let { profile.getOrNull(it)?.position }
                    item {
                        val trace = state.analysis?.trace.orEmpty()
                        if (justFinished) {
                            // Arrivée : le tracé se dessine, sans fond de carte (planche « Arrivée »).
                            TraceCanvas(
                                segments = trace,
                                traceColor = DromosTheme.metrics.trace,
                                progress = drawProgress(trace.isNotEmpty()),
                                startColor = DromosTheme.metrics.faster,
                                cursor = cursorPoint,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(MaterialTheme.shapes.large),
                            )
                        } else {
                            // Arrivée depuis l'historique : la vignette grandit jusqu'ici. Le temps de la
                            // transition, le tracé seul, comme la vignette ; la carte ensuite, car sa
                            // surface OpenGL ne suit ni découpe ni déformation.
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .sharedTrace(activity.id)
                                    .clip(MaterialTheme.shapes.large),
                            ) {
                                if (screenTransitionRunning()) {
                                    TraceCanvas(trace, DromosTheme.metrics.trace, Modifier.fillMaxSize(), cursor = cursorPoint)
                                } else {
                                    TrackMap(
                                        segments = trace,
                                        traceColor = DromosTheme.metrics.trace,
                                        online = state.onlineBasemap,
                                        live = false,
                                        cursor = cursorPoint,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                    }
                    // Les profils sous la carte, pour voir le curseur s'y déplacer ; à l'arrivée,
                    // après les totaux, qui restent le premier chiffre lu.
                    val profiles: LazyListScope.() -> Unit = {
                        if (profile.size >= 2) {
                            item {
                                SectionTitle(R.string.activity_profiles)
                            }
                            item {
                                // La moyenne de la sortie (temps / distance), la même que dans les totaux.
                                val average = if (activity.distanceMeters >= 10) activity.movingDuration.seconds * 1000.0 / activity.distanceMeters else null
                                Profiles(profile, cursor, onCursor = { cursor = it }, average, delayMs = if (justFinished) 2_000L else 300L)
                            }
                        }
                    }
                    if (!justFinished) profiles()
                    item { Totals(activity, Modifier.riseIn(baseDelayMs = if (justFinished && ambient.decorative) 1_400 else 250), counting = justFinished) }
                    if (justFinished) profiles()
                    activity.notes?.let { notes -> item { NotesCard(notes) } }
                    val splits = state.analysis?.splits.orEmpty()
                    if (splits.isNotEmpty()) {
                        item { SectionTitle(R.string.activity_splits) }
                        item { SplitHeader() }
                        val paces = splits.mapNotNull { it.paceSecondsPerKm }
                        val average = if (activity.distanceMeters > 0) activity.movingDuration.seconds / activity.distanceMeters * 1000 else null
                        itemsIndexed(splits, key = { _, it -> it.index }) { index, split ->
                            SplitRow(
                                split, index, average, fastest = paces.minOrNull(), slowest = paces.maxOrNull(),
                                baseDelayMs = if (justFinished) 2_000L else 400L,
                            )
                        }
                    }
                }
            }
            if (sweeping) FinishLineSweep(onEnd = { sweeping = false })
        }
    }
    if (editing && activity != null) {
        EditDialog(
            activity = activity,
            onDismiss = { editing = false },
            onSave = { title, notes ->
                editing = false
                onAction(ActivityDetailAction.Save(title, notes))
            },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.activity_delete_title)) },
            text = { Text(stringResource(R.string.activity_delete_body)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onAction(ActivityDetailAction.Delete) }) {
                    Text(stringResource(R.string.activity_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.activity_cancel)) }
            },
        )
    }
}

/** Titre de section : filet qui se trace, surtitre en capitales espacées. */
@Composable
private fun SectionTitle(@StringRes title: Int) {
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Rule(delayMs = 300)
        Text(
            stringResource(title).uppercase(),
            style = DromosTheme.type.eyebrow,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
    }
}

/** Avancement du tracé qui se dessine à l'arrivée, après le balayage. */
@Composable
private fun drawProgress(ready: Boolean): Float {
    val decorative = LocalMotionLevel.current.decorative
    val progress = remember { Animatable(if (decorative) 0f else 1f) }
    LaunchedEffect(ready) {
        if (!ready || !decorative) return@LaunchedEffect
        delay(SWEEP_MS.toLong())
        progress.animateTo(1f, tween(Motion.DRAW_MS, easing = Motion.Transition))
    }
    return if (decorative) progress.value else 1f
}

/** Date en télémétrie, titre en affiche : l'identité de la sortie. */
@Composable
private fun PosterTitle(activity: Activity) {
    val locale = LocalConfiguration.current.locales[0]
    val start = activity.startedAt.atZone(ZoneId.systemDefault())
    val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(start)
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(start)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TelemetryLabel(
            stringResource(R.string.activity_date_time, date, time),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RisingText(
            text = activity.displayTitle().uppercase(locale),
            style = DromosTheme.type.poster.copy(fontSize = DromosTheme.type.poster.fontSize * 0.8f),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun Totals(activity: Activity, modifier: Modifier = Modifier, counting: Boolean = false) {
    val locale = LocalConfiguration.current.locales[0]
    if (counting) {
        CountingTotals(activity, modifier)
        return
    }
    val elapsed = activity.endedAt?.let { Duration.between(activity.startedAt, it) }
    val stats = buildList {
        add(stringResource(R.string.activity_distance) to stringResource(UiR.string.unit_km, Format.kilometers(activity.distanceMeters, locale)))
        add(stringResource(R.string.activity_moving_time) to Format.duration(activity.movingDuration))
        add(
            stringResource(R.string.activity_avg_pace) to
                (Format.pace(activity.movingDuration, activity.distanceMeters)?.let { stringResource(UiR.string.unit_pace, it) } ?: DASH),
        )
        add(stringResource(R.string.activity_elapsed_time) to (elapsed?.let { Format.duration(it) } ?: DASH))
        add(stringResource(R.string.activity_gain) to elevation(activity.elevationGainMeters, UiR.string.unit_elevation_gain))
        add(stringResource(R.string.activity_loss) to elevation(activity.elevationLossMeters, UiR.string.unit_elevation_loss))
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        stats.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { (label, value) ->
                    Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                        TelemetryLabel(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FitText(value, style = DromosTheme.type.metric.copy(fontSize = DromosTheme.type.metric.fontSize * 1.45f))
                    }
                }
            }
        }
    }
}

/**
 * Totaux de l'arrivée, sur la planche : distance et temps en grand, qui comptent
 * depuis zéro ; allure et dénivelé en dessous.
 */
@Composable
private fun CountingTotals(activity: Activity, modifier: Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val big = DromosTheme.type.metricLarge
    val medium = DromosTheme.type.metricLarge.copy(fontSize = DromosTheme.type.metricLarge.fontSize * 0.75f)
    // Gabarits d'unité lus une fois : le compteur reformate à chaque image.
    val kmTemplate = stringResource(UiR.string.unit_km, VALUE)
    val gainTemplate = stringResource(UiR.string.unit_elevation_gain, VALUE)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TotalCell(stringResource(R.string.activity_distance), Modifier.weight(1f)) {
                CountUp(activity.distanceMeters, { kmTemplate.replace(VALUE, Format.kilometers(it, locale)) }, style = big, durationMs = 1_200)
            }
            TotalCell(stringResource(R.string.activity_moving_time), Modifier.weight(1f)) {
                CountUp(activity.movingDuration.seconds.toDouble(), { Format.duration(Duration.ofSeconds(it.toLong())) }, style = big, durationMs = 1_200)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TotalCell(stringResource(R.string.activity_avg_pace), Modifier.weight(1f)) {
                FitText(
                    Format.pace(activity.movingDuration, activity.distanceMeters)?.let { stringResource(UiR.string.unit_pace, it) } ?: DASH,
                    style = medium,
                )
            }
            TotalCell(stringResource(R.string.activity_gain), Modifier.weight(1f)) {
                CountUp(
                    activity.elevationGainMeters,
                    { gainTemplate.replace(VALUE, Format.meters(it)) },
                    style = medium,
                    color = DromosTheme.metrics.elevation,
                    durationMs = 1_200,
                )
            }
        }
    }
}

@Composable
private fun TotalCell(label: String, modifier: Modifier, value: @Composable () -> Unit) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        TelemetryLabel(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        value()
    }
}

private const val VALUE = "\u0000"

/** « +84 m », mais « 0 m » sans signe quand il n'y a rien : « −0 m » ne veut rien dire. */
@Composable
private fun elevation(meters: Double, @StringRes signed: Int): String {
    val rounded = Format.meters(meters)
    return if (rounded == "0") stringResource(UiR.string.unit_meters, rounded) else stringResource(signed, rounded)
}

@Composable
private fun NotesCard(notes: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(notes, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SplitHeader() {
    // En-têtes visuels seulement : chaque ligne se dit en entier.
    Row(Modifier.fillMaxWidth().clearAndSetSemantics {}, verticalAlignment = Alignment.CenterVertically) {
        // Mêmes colonnes fixes que les lignes : l'en-tête se réduit avec elles plutôt que de se couper.
        SplitHeading(R.string.activity_split_km, Modifier.width(splitColumn(48.dp)))
        SplitHeading(R.string.activity_split_pace, Modifier.width(splitColumn(72.dp)))
        Box(Modifier.weight(1f))
        SplitHeading(R.string.activity_split_gain, Modifier.width(splitColumn(56.dp)))
    }
}

/** Colonne de split : elle s'élargit avec la police système (jusqu'à ×1,5), la barre garde le reste. */
@Composable
private fun splitColumn(base: Dp): Dp = base * LocalDensity.current.fontScale.coerceIn(1f, 1.5f)

@Composable
private fun SplitHeading(@StringRes text: Int, modifier: Modifier) {
    FitText(stringResource(text).uppercase(), modifier, style = DromosTheme.type.telemetry, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * Une ligne de split : la barre est d'autant plus longue que le kilomètre est
 * rapide, verte s'il est plus rapide que la moyenne, saumon s'il est plus lent.
 */
@Composable
private fun SplitRow(split: Split, index: Int, average: Double?, fastest: Double?, slowest: Double?, baseDelayMs: Long = 400) {
    val pace = split.paceSecondsPerKm
    val color = when {
        pace == null || average == null -> MaterialTheme.colorScheme.outline
        pace <= average -> DromosTheme.metrics.faster
        else -> DromosTheme.metrics.slower
    }
    // Échelle : le plus lent occupe 35 % de la largeur, le plus rapide 100 %.
    val fraction = if (pace == null || fastest == null || slowest == null || slowest <= fastest) 1f
    else (0.35 + 0.65 * (slowest - pace) / (slowest - fastest)).toFloat()
    // Les barres poussent l'une après l'autre (rebond léger), une seule fois.
    val level = LocalMotionLevel.current
    val grow = remember { Animatable(if (level.decorative) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!level.decorative) return@LaunchedEffect
        delay(baseDelayMs + 70L * index.coerceAtMost(12))
        grow.animateTo(1f, tween(Motion.RELEASE_MS, easing = Motion.Release))
    }
    val label = if (split.distanceMeters < 999.0) {
        stringResource(R.string.activity_split_partial, Format.kilometers(split.distanceMeters, LocalConfiguration.current.locales[0]))
    } else {
        split.index.toString()
    }
    // « plus rapide / plus lent » est dit en toutes lettres : couleur et longueur ne suffisent pas.
    val paceText = pace?.let { Format.paceFromSeconds(it) } ?: DASH
    val gain = elevation(split.elevationGainMeters, UiR.string.unit_elevation_gain)
    val description = when {
        pace == null || average == null -> stringResource(R.string.activity_split_a11y, label, paceText, gain)
        pace <= average -> stringResource(R.string.activity_split_a11y_faster, label, paceText, gain)
        else -> stringResource(R.string.activity_split_a11y_slower, label, paceText, gain)
    }
    // Hauteur minimale, pas fixe : avec une grande police, la ligne grandit au lieu de rogner.
    Row(
        Modifier.fillMaxWidth().heightIn(min = 28.dp).clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Colonnes à largeur fixe pour rester alignées : le texte se réduit si la police système est grande.
        FitText(label, Modifier.width(splitColumn(48.dp)), style = DromosTheme.type.metric, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FitText(
            paceText,
            Modifier.width(splitColumn(72.dp)),
            style = DromosTheme.type.metric,
        )
        Box(Modifier.weight(1f).height(28.dp).padding(vertical = 6.dp)) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction * (if (level.decorative) grow.value.coerceAtLeast(0f) else 1f))
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(color),
            )
        }
        FitText(
            gain,
            Modifier.width(splitColumn(56.dp)).padding(start = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EditDialog(activity: Activity, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    val defaultTitle = activity.copy(title = null).displayTitle()
    var title by remember { mutableStateOf(activity.title.orEmpty()) }
    var notes by remember { mutableStateOf(activity.notes.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.activity_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.activity_field_title)) },
                    placeholder = { Text(defaultTitle) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.activity_field_notes)) },
                    minLines = 3,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(title, notes) }) { Text(stringResource(R.string.activity_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.activity_cancel)) } },
    )
}

private const val DASH = "–" // texte-technique : tiret de valeur absente
