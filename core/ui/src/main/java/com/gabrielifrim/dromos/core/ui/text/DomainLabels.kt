package com.gabrielifrim.dromos.core.ui.text

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gabrielifrim.dromos.core.domain.model.Activity
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.ui.R
import java.time.ZoneId

/** Libellés des énumérations du domaine : le domaine reste sans ressource Android. */
@get:StringRes
val ActivityType.labelRes: Int
    get() = when (this) {
        ActivityType.WALK -> R.string.activity_type_walk
        ActivityType.RUN -> R.string.activity_type_run
    }

/**
 * Titre affiché : celui de l'utilisateur, sinon « Course du matin »,
 * « Marche du soir »… selon l'heure de départ.
 */
@Composable
fun Activity.displayTitle(): String = title ?: stringResource(defaultTitleRes(type, startedAt.atZone(ZoneId.systemDefault()).hour))

@StringRes
fun defaultTitleRes(type: ActivityType, hour: Int): Int {
    val run = type == ActivityType.RUN
    return when (hour) {
        in 5..11 -> if (run) R.string.title_run_morning else R.string.title_walk_morning
        in 12..17 -> if (run) R.string.title_run_afternoon else R.string.title_walk_afternoon
        in 18..22 -> if (run) R.string.title_run_evening else R.string.title_walk_evening
        else -> if (run) R.string.title_run_night else R.string.title_walk_night
    }
}
