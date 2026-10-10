package com.gabrielifrim.dromos.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabrielifrim.dromos.core.domain.model.AppearanceSettings
import com.gabrielifrim.dromos.core.ui.component.FitText
import com.gabrielifrim.dromos.core.domain.model.MapPreferences
import com.gabrielifrim.dromos.core.domain.model.MotionPreference
import com.gabrielifrim.dromos.core.domain.model.ThemeMode
import com.gabrielifrim.dromos.core.domain.model.TrackingPreferences
import com.gabrielifrim.dromos.core.tracking.BatteryOptimization
import com.gabrielifrim.dromos.core.ui.theme.DromosPalette
import com.gabrielifrim.dromos.core.ui.theme.DromosTheme
import com.gabrielifrim.dromos.core.ui.theme.Palettes

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val tracking by viewModel.tracking.collectAsStateWithLifecycle()
    val map by viewModel.map.collectAsStateWithLifecycle()
    SettingsScreen(appearance, tracking, map, viewModel::onAction, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appearance: AppearanceSettings,
    tracking: TrackingPreferences,
    map: MapPreferences,
    onAction: (SettingsAction) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(stringResource(R.string.settings_tracking))
            SwitchRow(
                title = stringResource(R.string.settings_auto_pause),
                body = stringResource(R.string.settings_auto_pause_body),
                checked = tracking.autoPause,
            ) { onAction(SettingsAction.SetAutoPause(it)) }

            BatteryRow()

            SectionTitle(stringResource(R.string.settings_map), Modifier.padding(top = 12.dp))
            SwitchRow(
                title = stringResource(R.string.settings_online_basemap),
                body = stringResource(R.string.settings_online_basemap_body),
                checked = map.onlineBasemap,
            ) { onAction(SettingsAction.SetOnlineBasemap(it)) }

            SectionTitle(stringResource(R.string.settings_theme_mode), Modifier.padding(top = 12.dp))
            ThemeModeSelector(appearance.themeMode) { onAction(SettingsAction.SelectThemeMode(it)) }

            SectionTitle(stringResource(R.string.settings_motion), Modifier.padding(top = 12.dp))
            MotionSelector(appearance.motion) { onAction(SettingsAction.SelectMotion(it)) }

            SectionTitle(stringResource(R.string.settings_palette), Modifier.padding(top = 12.dp))
            val selected = Palettes.byId(appearance.paletteId)
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Palettes.all.forEach { palette ->
                    PaletteCard(
                        palette = palette,
                        selected = palette.id == selected.id,
                        onClick = { onAction(SettingsAction.SelectPalette(palette.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = DromosTheme.type.eyebrow,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

/** État relu à chaque retour sur l'écran : l'utilisateur revient des paramètres Android. */
@Composable
private fun BatteryRow() {
    val context = LocalContext.current
    var unrestricted by remember { mutableStateOf(BatteryOptimization.isUnrestricted(context)) }
    LifecycleResumeEffect(Unit) {
        unrestricted = BatteryOptimization.isUnrestricted(context)
        onPauseOrDispose { }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = if (unrestricted) BatteryOptimization.manufacturerIntent(context)
                else BatteryOptimization.requestIntent(context)
                BatteryOptimization.open(context, intent)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_battery), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(if (unrestricted) R.string.settings_battery_ok else R.string.settings_battery_restricted),
                style = MaterialTheme.typography.bodyMedium,
                color = if (unrestricted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
        }
        Icon(
            if (unrestricted) Icons.Outlined.BatteryFull else Icons.Outlined.BatteryAlert,
            contentDescription = null,
            tint = if (unrestricted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun SwitchRow(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onChange, role = Role.Switch),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Rôle porté par la ligne entière : l'interrupteur seul n'est pas cliquable.
        Switch(checked = checked, onCheckedChange = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MotionSelector(current: MotionPreference, onSelect: (MotionPreference) -> Unit) {
    val options = listOf(
        MotionPreference.EXPRESSIVE to R.string.settings_motion_expressive,
        MotionPreference.CALM to R.string.settings_motion_calm,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (motion, label) ->
                SegmentedButton(
                    selected = current == motion,
                    onClick = { onSelect(motion) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) { FitText(stringResource(label)) }
            }
        }
        Text(
            stringResource(
                if (current == MotionPreference.EXPRESSIVE) R.string.settings_motion_expressive_body
                else R.string.settings_motion_calm_body,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeSelector(current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val modes = listOf(
        ThemeMode.SYSTEM to R.string.settings_theme_system,
        ThemeMode.LIGHT to R.string.settings_theme_light,
        ThemeMode.DARK to R.string.settings_theme_dark,
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = current == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, modes.size),
            ) { FitText(stringResource(label)) }
        }
    }
}

@Composable
private fun PaletteCard(palette: DromosPalette, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    OutlinedCard(
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(palette.nameRes), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    palette.swatches.forEach { Swatch(it) }
                }
            }
            if (selected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.settings_palette_selected),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun Swatch(color: Color) {
    Box(
        Modifier
            .size(28.dp)
            .background(color, CircleShape),
    )
}
