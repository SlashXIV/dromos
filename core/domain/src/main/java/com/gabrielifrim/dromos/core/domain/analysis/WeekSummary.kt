package com.gabrielifrim.dromos.core.domain.analysis

import com.gabrielifrim.dromos.core.domain.model.Activity
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

/** Totaux de la semaine en cours (lundi → dimanche, heure locale). */
data class WeekSummary(
    val weekNumber: Int,
    val distanceMeters: Double,
    val count: Int,
    val elevationGainMeters: Double,
) {
    companion object {
        fun of(activities: List<Activity>, now: Instant, zone: ZoneId): WeekSummary {
            val today = now.atZone(zone).toLocalDate()
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(zone).toInstant()
            val week = activities.filter { !it.startedAt.isBefore(monday) && !it.startedAt.isAfter(now) }
            return WeekSummary(
                weekNumber = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),
                distanceMeters = week.sumOf { it.distanceMeters },
                count = week.size,
                elevationGainMeters = week.sumOf { it.elevationGainMeters },
            )
        }
    }
}
