package me.doubao.oscillochord.domain.audio

import me.doubao.oscillochord.domain.chord.TuningSystem

class Oscillator(
    var midiNote: Int,
    var baseFrequency: Double = 440.0,
    var waveform: Waveform = Waveform.SINE,
    var amplitude: Float = 1.0f,
    var tuningSystem: TuningSystem = TuningSystem.EQUAL
) {
    private var phase: Double = 0.0

    val frequency: Double
        get() {
            return tuningSystem.frequencyForMidi(midiNote, baseFrequency)
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
