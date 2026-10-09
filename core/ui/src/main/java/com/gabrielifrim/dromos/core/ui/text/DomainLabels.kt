package com.gabrielifrim.dromos.core.ui.text

import androidx.annotation.StringRes
import com.gabrielifrim.dromos.core.domain.model.ActivityType
import com.gabrielifrim.dromos.core.ui.R

/** Libellés des énumérations du domaine : le domaine reste sans ressource Android. */
@get:StringRes
val ActivityType.labelRes: Int
    get() = when (this) {
        ActivityType.WALK -> R.string.activity_type_walk
        ActivityType.RUN -> R.string.activity_type_run
    }
