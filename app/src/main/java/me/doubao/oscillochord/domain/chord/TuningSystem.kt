package me.doubao.oscillochord.domain.chord

enum class TuningSystem(val displayName: String) {
    EQUAL("十二平均律"),
    JUST("纯律"),
    PYTHAGOREAN("五度相生率");

    fun frequencyForMidi(midiNote: Int, baseFrequency: Double = 440.0): Double {
        val a4 = 69
        val semitoneOffset = midiNote - a4
        return when (this) {
            EQUAL -> baseFrequency * Math.pow(2.0, semitoneOffset / 12.0)
            JUST -> justFrequency(midiNote, baseFrequency)
            PYTHAGOREAN -> pythagoreanFrequency(midiNote, baseFrequency)
        }
    }

    companion object {
        // Just intonation ratios relative to C (between 1 and 2)
        private val JUST_RATIOS = doubleArrayOf(
            1.0,                      // C
            16.0 / 15.0,              // C#
            9.0 / 8.0,                // D
            6.0 / 5.0,                // D#
            5.0 / 4.0,                // E
            4.0 / 3.0,                // F
            45.0 / 32.0,              // F#
            3.0 / 2.0,                // G
            8.0 / 5.0,                // G#
            5.0 / 3.0,                // A
            9.0 / 5.0,                // A#
            15.0 / 8.0                // B
        )

        // Pre-computed Pythagorean ratios (verified):
        // C=1/1, C#=2187/2048, D=9/8, D#=32/27, E=81/64, F=4/3,
        // F#=729/512, G=3/2, G#=128/81, A=27/16, A#=16/9, B=243/128
        private val PYTHAGOREAN_RATIOS = doubleArrayOf(
            1.0,                      // C = 1/1
            2187.0 / 2048.0,          // C#
            9.0 / 8.0,                // D
            32.0 / 27.0,              // D#
            81.0 / 64.0,              // E
            4.0 / 3.0,                // F
            729.0 / 512.0,            // F#
            3.0 / 2.0,                // G
            128.0 / 81.0,             // G#
            27.0 / 16.0,              // A
            16.0 / 9.0,               // A#
            243.0 / 128.0             // B
        )
    }

    private fun justFrequency(midiNote: Int, baseFreq: Double): Double {
        val pc = ((midiNote % 12) + 12) % 12
        val octave = (midiNote / 12) - 1
        // C4 (MIDI 60) relative to A4 (MIDI 69, 440 Hz)
        val c4Freq = baseFreq * Math.pow(2.0, (60 - 69) / 12.0)
        return c4Freq * Math.pow(2.0, octave - 4.0) * JUST_RATIOS[pc]
    }

    private fun pythagoreanFrequency(midiNote: Int, baseFreq: Double): Double {
        val pc = ((midiNote % 12) + 12) % 12
        val octave = (midiNote / 12) - 1
        // C4 (MIDI 60) relative to A4 (MIDI 69, 440 Hz)
        val c4Freq = baseFreq * Math.pow(2.0, (60 - 69) / 12.0)
        return c4Freq * Math.pow(2.0, octave - 4.0) * PYTHAGOREAN_RATIOS[pc]
    }
}
