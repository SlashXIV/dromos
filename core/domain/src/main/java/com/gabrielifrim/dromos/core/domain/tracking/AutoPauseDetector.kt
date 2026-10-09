package com.gabrielifrim.dromos.core.domain.tracking

import com.gabrielifrim.dromos.core.domain.model.ActivityType
import java.time.Duration
import java.time.Instant

/**
 * Décide quand le chrono s'arrête et repart tout seul.
 *
 * Deux seuils (hystérésis) : on s'arrête sous [Thresholds.stopSpeed] pendant
 * [STOP_AFTER], on repart au-dessus de [Thresholds.goSpeed] pendant [GO_AFTER].
 * Avec un seul seuil, un coureur qui trottine à la limite ferait clignoter la pause.
 *
 * La décision est RÉTROACTIVE : le temps en mouvement s'arrête au premier
 * instant lent, pas au moment où la pause est confirmée.
 */
class AutoPauseDetector(type: ActivityType) {

    private data class Thresholds(val stopSpeed: Double, val goSpeed: Double)

    private val thresholds = when (type) {
        // Marche : un piéton lent avance à ~0,8 m/s ; à l'arrêt, le filtre retombe sous 0,3.
        ActivityType.WALK -> Thresholds(stopSpeed = 0.3, goSpeed = 0.6)
        // Course : sous 0,8 m/s (2,9 km/h), on ne court plus, on attend.
        ActivityType.RUN -> Thresholds(stopSpeed = 0.8, goSpeed = 1.4)
    }

    private var slowSince: Instant? = null
    private var fastSince: Instant? = null

    sealed interface Decision {
        data object None : Decision
        /** Arrêt confirmé ; le mouvement a cessé à [since]. */
        data class Stop(val since: Instant) : Decision
        /** Départ confirmé ; le mouvement a repris à [since]. */
        data class Go(val since: Instant) : Decision
    }

    fun onSpeed(now: Instant, speed: Double, autoPaused: Boolean): Decision {
        if (!autoPaused) {
            fastSince = null
            if (speed < thresholds.stopSpeed) {
                val since = slowSince ?: now.also { slowSince = it }
                if (Duration.between(since, now) >= STOP_AFTER) {
                    slowSince = null
                    return Decision.Stop(since)
                }
            } else {
                slowSince = null
            }
        } else {
            slowSince = null
            if (speed > thresholds.goSpeed) {
                val since = fastSince ?: now.also { fastSince = it }
                if (Duration.between(since, now) >= GO_AFTER) {
                    fastSince = null
                    return Decision.Go(since)
                }
            } else {
                fastSince = null
            }
        }
        return Decision.None
    }

    fun reset() {
        slowSince = null
        fastSince = null
    }

    companion object {
        val STOP_AFTER: Duration = Duration.ofSeconds(5)
        val GO_AFTER: Duration = Duration.ofSeconds(2)
    }
}
