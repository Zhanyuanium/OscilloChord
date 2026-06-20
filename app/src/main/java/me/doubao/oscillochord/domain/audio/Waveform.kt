package me.doubao.oscillochord.domain.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

enum class Waveform {
    SINE,
    SQUARE,
    TRIANGLE,
    SAWTOOTH;

    fun generate(phase: Double): Double {
        // phase in [0, 1)
        return when (this) {
            SINE -> sin(2.0 * PI * phase)
            SQUARE -> if (phase < 0.5) 1.0 else -1.0
            TRIANGLE -> 4.0 * abs(phase - 0.5) - 1.0
            SAWTOOTH -> 2.0 * phase - 1.0
        }
    }
}
