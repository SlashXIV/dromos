package com.gabrielifrim.dromos.core.gpx

import java.math.BigDecimal

internal fun String.escapeXml(): String = buildString(length) {
    for (c in this@escapeXml) {
        when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            else -> append(c)
        }
    }
}

// Écriture décimale la plus courte qui se relit à l'identique : ni arrondi qui
// fausserait l'aller-retour, ni notation « 1.0E-5 » refusée par le schéma GPX.
internal fun Double.plain(): String = BigDecimal.valueOf(this).toPlainString()

internal fun Float.plain(): String = BigDecimal(toString()).toPlainString()
