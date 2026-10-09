package com.gabrielifrim.dromos.core.tracking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * Fenêtre proposée avant la première sortie quand Android peut couper Dromos en
 * arrière-plan. Quel que soit le choix, la sortie démarre ensuite ([onDone]).
 */
@Composable
fun BatteryPromptDialog(onDone: (neverAskAgain: Boolean) -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = { onDone(false) },
        icon = { Icon(Icons.Outlined.BatteryAlert, contentDescription = null) },
        title = { Text(stringResource(R.string.battery_prompt_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.battery_prompt_body))
                Text(stringResource(R.string.battery_prompt_manufacturer))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                BatteryOptimization.open(context, BatteryOptimization.requestIntent(context))
                onDone(false)
            }) { Text(stringResource(R.string.battery_prompt_allow)) }
        },
        dismissButton = {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                TextButton(onClick = {
                    BatteryOptimization.open(context, BatteryOptimization.manufacturerIntent(context))
                    onDone(false)
                }) { Text(stringResource(R.string.battery_prompt_manufacturer_button)) }
                TextButton(onClick = { onDone(true) }) { Text(stringResource(R.string.battery_prompt_never)) }
            }
        },
    )
}
