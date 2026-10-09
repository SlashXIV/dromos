package com.gabrielifrim.dromos.core.domain.geo

import com.gabrielifrim.dromos.core.domain.model.GeoPoint
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot

/**
 * Simplifie un tracé pour une miniature : algorithme de Douglas-Peucker, dont la
 * tolérance augmente jusqu'à tenir en [maxPoints]. La forme reste reconnaissable
 * (virages, aller-retour) avec quelques dizaines de points au lieu de milliers.
 */
object TraceSimplifier {

    fun simplify(points: List<GeoPoint>, maxPoints: Int = 64): List<GeoPoint> {
        if (points.size <= maxPoints) return points
        val lat0 = points.first().latitude
        val kx = cos(Math.toRadians(lat0))
        val xs = DoubleArray(points.size) { points[it].longitude * kx }
        val ys = DoubleArray(points.size) { points[it].latitude }
        val span = maxOf(xs.max() - xs.min(), ys.max() - ys.min()).coerceAtLeast(1e-9)
        var tolerance = span / 1000
        var kept: List<Int>
        do {
            kept = douglasPeucker(xs, ys, tolerance)
            tolerance *= 1.6
        } while (kept.size > maxPoints)
        return kept.map { points[it] }
    }

    private fun douglasPeucker(xs: DoubleArray, ys: DoubleArray, tolerance: Double): List<Int> {
        val keep = BooleanArray(xs.size)
        keep[0] = true
        keep[xs.size - 1] = true
        val stack = ArrayDeque<Pair<Int, Int>>().apply { add(0 to xs.size - 1) }
        while (stack.isNotEmpty()) {
            val (a, b) = stack.removeLast()
            var maxDist = -1.0
            var index = -1
            for (i in a + 1 until b) {
                val d = distanceToSegment(xs[i], ys[i], xs[a], ys[a], xs[b], ys[b])
                if (d > maxDist) { maxDist = d; index = i }
            }
            if (index >= 0 && maxDist > tolerance) {
                keep[index] = true
                stack.add(a to index)
                stack.add(index to b)
            }
        }
        return keep.indices.filter { keep[it] }
    }

    private fun distanceToSegment(px: Double, py: Double, ax: Double, ay: Double, bx: Double, by: Double): Double {
        val dx = bx - ax
        val dy = by - ay
        val len = dx * dx + dy * dy
        if (len == 0.0) return hypot(px - ax, py - ay)
        val t = (((px - ax) * dx + (py - ay) * dy) / len).coerceIn(0.0, 1.0)
        return abs(hypot(px - (ax + t * dx), py - (ay + t * dy)))
    }
}
