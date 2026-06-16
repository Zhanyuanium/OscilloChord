package me.doubao.oscillochord.domain.lissajous

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class LissajousProjector {
    /**
     * Scheme A: Rotation projection.
     * Maps N signals to 2D by assigning each an equally-spaced angle.
     * N=2 degenerates to classic Lissajous: x = s0, y = s1.
     */
    fun project(samples: FloatArray): Pair<Float, Float> {
        val n = samples.size
        if (n == 0) return 0f to 0f

        var x = 0f
        var y = 0f

        for (i in 0 until n) {
            val angle = 2.0 * PI * i / n
            x += samples[i] * cos(angle).toFloat()
            y += samples[i] * sin(angle).toFloat()
        }

        return x to y
    }
}
