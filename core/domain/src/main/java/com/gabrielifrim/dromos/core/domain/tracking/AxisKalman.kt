package com.gabrielifrim.dromos.core.domain.tracking

/**
 * Filtre de Kalman à une dimension, modèle « vitesse constante » : l'état est
 * (position, vitesse), le bruit de processus une accélération blanche
 * d'intensité [accelerationNoise].
 *
 * Il accepte une mesure de position (GNSS) et, si disponible, une mesure de
 * vitesse (Doppler). C'est la vitesse Doppler qui fait la précision : elle
 * contraint le mouvement d'un point à l'autre, là où les positions seules
 * oscillent de plusieurs mètres.
 */
internal class AxisKalman(private val accelerationNoise: Double) {
    var position = 0.0
        private set
    var velocity = 0.0
        private set
    private var p00 = 0.0
    private var p01 = 0.0
    private var p11 = 0.0
    var initialized = false
        private set

    fun reset(position: Double, positionStd: Double, velocity: Double?) {
        this.position = position
        this.velocity = velocity ?: 0.0
        p00 = positionStd * positionStd
        p01 = 0.0
        p11 = if (velocity != null) 1.0 else INITIAL_VELOCITY_VARIANCE
        initialized = true
    }

    fun predict(dt: Double) {
        position += velocity * dt
        val q2 = accelerationNoise * accelerationNoise
        val n00 = p00 + dt * 2 * p01 + dt * dt * p11 + q2 * dt * dt * dt / 3
        val n01 = p01 + dt * p11 + q2 * dt * dt / 2
        val n11 = p11 + q2 * dt
        p00 = n00; p01 = n01; p11 = n11
    }

    fun updatePosition(measured: Double, std: Double) {
        val s = p00 + std * std
        val k0 = p00 / s
        val k1 = p01 / s
        val innovation = measured - position
        position += k0 * innovation
        velocity += k1 * innovation
        val n00 = (1 - k0) * p00
        val n01 = (1 - k0) * p01
        val n11 = p11 - k1 * p01
        p00 = n00; p01 = n01; p11 = n11
    }

    fun updateVelocity(measured: Double, std: Double) {
        val s = p11 + std * std
        val k0 = p01 / s
        val k1 = p11 / s
        val innovation = measured - velocity
        position += k0 * innovation
        velocity += k1 * innovation
        val n00 = p00 - k0 * p01
        val n01 = p01 - k0 * p11
        val n11 = (1 - k1) * p11
        p00 = n00; p01 = n01; p11 = n11
    }

    private companion object {
        /** Vitesse initiale inconnue : (5 m/s)². */
        const val INITIAL_VELOCITY_VARIANCE = 25.0
    }
}

/**
 * Filtre « position constante » : la position estimée suit les mesures avec une
 * inertie qui dépend de la précision annoncée. Utilisé quand le récepteur ne
 * fournit pas de vitesse : un filtre à vitesse constante sans Doppler suit
 * alors trop le bruit.
 */
internal class ConstantPositionKalman(private val processNoise: Double) {
    var x = 0.0
        private set
    var y = 0.0
        private set
    private var variance = -1.0

    fun reset(x: Double, y: Double, std: Double) {
        this.x = x; this.y = y; variance = std * std
    }

    val initialized: Boolean get() = variance >= 0

    fun update(dt: Double, mx: Double, my: Double, std: Double) {
        variance += dt * processNoise * processNoise
        val k = variance / (variance + std * std)
        x += k * (mx - x)
        y += k * (my - y)
        variance *= (1 - k)
    }
}
