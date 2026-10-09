package com.gabrielifrim.dromos.core.tracking

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Optimisation de batterie : Android, et surtout les surcouches constructeur
 * (Xiaomi, Samsung, Huawei, Oppo…), coupent les applications en arrière-plan
 * pour gagner de l'autonomie, service de premier plan compris. Pour un suivi
 * GPS d'une heure écran éteint, il faut lever cette restriction.
 */
object BatteryOptimization {

    fun isUnrestricted(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) ?: true

    /**
     * Dialogue système « Autoriser à ignorer l'optimisation de la batterie ».
     * Usage légitime selon la politique Android : suivi d'activité physique.
     */
    @SuppressLint("BatteryLife")
    fun requestIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri())

    /**
     * Écran du constructeur qui gère, en plus d'Android, l'arrêt des applications
     * en arrière-plan. Repli : la fiche de l'application dans les paramètres.
     */
    fun manufacturerIntent(context: Context): Intent {
        val pkg = context.packageName
        val candidates = when (Build.MANUFACTURER.lowercase()) {
            "xiaomi", "redmi", "poco" -> listOf(
                component("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")
                    .putExtra("package_name", pkg)
                    .putExtra("package_label", context.applicationInfo.loadLabel(context.packageManager)),
                component("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            )
            "samsung" -> listOf(component("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"))
            "huawei", "honor" -> listOf(
                component("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            )
            "oppo", "realme", "oneplus" -> listOf(
                component("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            )
            "vivo" -> listOf(
                component("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            )
            else -> emptyList()
        }
        return candidates.firstOrNull { it.resolveActivity(context.packageManager) != null }
            ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$pkg".toUri())
    }

    /**
     * Ouvre l'écran voulu ; certains écrans constructeur refusent d'être ouverts
     * par une autre application : on retombe alors sur la fiche de l'application.
     */
    fun open(context: Context, intent: Intent) {
        val flagged = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(flagged) }.onFailure {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private fun component(pkg: String, cls: String) = Intent().setComponent(ComponentName(pkg, cls))
}
