package com.gabrielifrim.dromos.core.domain.tracking

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Transforme des positions GNSS bruitées en distance parcourue.
 *
 * Deux filtres tournent en parallèle sur des coordonnées locales en mètres :
 * - avec vitesse Doppler (le cas des téléphones) : Kalman à vitesse constante,
 *   ≈ 1 % d'erreur par ciel dégagé, ≈ 3 % en conditions difficiles ;
 * - sans vitesse : Kalman à position constante + seuil de déplacement, qui
 *   ignore le tremblement à l'arrêt, ≈ 4 % / 10 %.
 * Chiffres mesurés sur traces synthétiques (cf. ReferenceTracesTest).
 *
 * Le pas de distance d'un point vient du filtre adapté à CE point ; l'ancre de
 * l'autre filtre est recalée pour qu'un changement de mode ne compte rien deux fois.
 */
internal class DistanceFilter {
    private val cvX = AxisKalman(CV_ACCELERATION_NOISE)
    private val cvY = AxisKalman(CV_ACCELERATION_NOISE)
    private val cp = ConstantPositionKalman(CP_PROCESS_NOISE)
    private var cvAnchorX = 0.0
    private var cvAnchorY = 0.0
    private var cpAnchorX = 0.0
    private var cpAnchorY = 0.0
    private var lastSeconds = 0.0
    private var started = false
    private var lastX = 0.0
    private var lastY = 0.0
    private var lastWithVelocity = false
    private val anchorX get() = if (lastWithVelocity) cvAnchorX else cpAnchorX
    private val anchorY get() = if (lastWithVelocity) cvAnchorY else cpAnchorY

    /** Vitesse estimée par le filtre, en m/s. */
    val speed: Double get() = if (started) hypot(cvX.velocity, cvY.velocity) else 0.0

    /** Repart de zéro (nouveau segment, saut aberrant) : aucune distance comptée. */
    fun reset() {
        started = false
    }

    /**
     * Reliquat de fin de segment : le filtre suit le coureur avec quelques mètres
     * de retard (≈ 8 m à 3 m/s sans Doppler). À l'arrêt définitif du segment, on
     * ajoute l'écart entre la dernière ancre et la dernière position mesurée.
     */
    fun tail(): Double = if (started) hypot(lastX - anchorX, lastY - anchorY) else 0.0

    /**
     * @param x, y position locale en mètres (est, nord)
     * @param velocity vitesse Doppler (est, nord) en m/s, ou null
     * @return la distance à ajouter pour ce point, en mètres
     */
    fun add(seconds: Double, x: Double, y: Double, accuracy: Double, velocity: Pair<Double, Double>?): Double {
        val std = maxOf(accuracy, MIN_POSITION_STD)
        lastX = x; lastY = y
        lastWithVelocity = velocity != null
        if (!started) {
            cvX.reset(x, std, velocity?.first)
            cvY.reset(y, std, velocity?.second)
            cp.reset(x, y, std)
            cvAnchorX = x; cvAnchorY = y
            cpAnchorX = x; cpAnchorY = y
            lastSeconds = seconds
            started = true
            return 0.0
        }
        val dt = (seconds - lastSeconds).coerceAtLeast(0.0)
        lastSeconds = seconds

        cvX.predict(dt); cvY.predict(dt)
        cvX.updatePosition(x, std); cvY.updatePosition(y, std)
        if (velocity != null) {
            cvX.updateVelocity(velocity.first, DOPPLER_STD)
            cvY.updateVelocity(velocity.second, DOPPLER_STD)
        }
        cp.update(dt, x, y, std)

        return if (velocity != null) {
            val step = hypot(cvX.position - cvAnchorX, cvY.position - cvAnchorY)
            cpAnchorX = cp.x; cpAnchorY = cp.y
            if (step >= CV_MIN_STEP) {
                cvAnchorX = cvX.position; cvAnchorY = cvY.position
                step
            } else {
                0.0
            }
        } else {
            val step = hypot(cp.x - cpAnchorX, cp.y - cpAnchorY)
            cvAnchorX = cvX.position; cvAnchorY = cvY.position
            if (step >= CP_MIN_STEP) {
                cpAnchorX = cp.x; cpAnchorY = cp.y
                step
            } else {
                0.0
            }
        }
    }

    companion object {
        /** Le GNSS d'un téléphone ne fait jamais mieux que ~5 m, quoi qu'il annonce. */
        const val MIN_POSITION_STD = 5.0
        const val DOPPLER_STD = 0.3
        const val CV_ACCELERATION_NOISE = 0.3
        const val CV_MIN_STEP = 1.0
        const val CP_PROCESS_NOISE = 1.8
        const val CP_MIN_STEP = 4.0

        /** Vecteur vitesse (est, nord) depuis une vitesse et un cap GNSS. */
        fun velocityOf(speed: Float?, bearingDegrees: Float?): Pair<Double, Double>? {
            // Vitesse nulle pile : certains récepteurs l'annoncent au lieu de « inconnue ».
            if (speed == null || bearingDegrees == null || speed <= 0f) return null
            val rad = Math.toRadians(bearingDegrees.toDouble())
            return speed * sin(rad) to speed * cos(rad)
        }
    }
}
