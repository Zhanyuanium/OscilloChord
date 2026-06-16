package me.doubao.oscillochord.domain.lissajous

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class LissajousProjector {
    /**
     * Scheme A: Rotation projection.
     * N=1 → handled by caller (time ramp vs waveform).
     * N=2 → classic Lissajous: (x, y) = (s0, s1).
     * N≥3 → rotation projection with angles 2π·i/N.
     */
    fun project(samples: FloatArray): Pair<Float, Float> {
        val n = samples.size
        if (n == 0) return 0f to 0f
        if (n == 1) return samples[0] to 0f
        if (n == 2) return samples[0] to samples[1]

        // N ≥ 3: equally-spaced rotation projection
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
