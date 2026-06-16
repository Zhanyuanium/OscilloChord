package me.doubao.oscillochord.domain.audio

import kotlin.math.pow

class Oscillator(
    var midiNote: Int,
    var baseFrequency: Double = 440.0,
    var waveform: Waveform = Waveform.SINE,
    var amplitude: Float = 1.0f
) {
    private var phase: Double = 0.0

    val frequency: Double
        get() {
            val semitoneOffset = midiNote - 69 // A4 = MIDI 69
            return baseFrequency * 2.0.pow(semitoneOffset / 12.0)
        }

    fun nextSample(): Float {
        val sample = waveform.generate(phase).toFloat() * amplitude
        phase += frequency / SAMPLE_RATE
        if (phase >= 1.0) phase -= 1.0
        return sample
    }

    fun resetPhase() {
        phase = 0.0
    }

    companion object {
        const val SAMPLE_RATE = 44100
    }
}
