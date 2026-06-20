package me.doubao.oscillochord.domain.chord

enum class TuningSystem {
    EQUAL,
    JUST,
    PYTHAGOREAN;

    fun frequencyForMidi(midiNote: Int, baseFrequency: Double = 440.0): Double {
        val pc = Math.floorMod(midiNote, 12)
        val octave = Math.floorDiv(midiNote, 12) - 1  // A4 → octave 4
        val a4Octave = Math.floorDiv(69, 12) - 1      // = 4
        val octaveDiff = octave - a4Octave

        return when (this) {
            EQUAL -> baseFrequency * Math.pow(2.0, (midiNote - 69) / 12.0)
            JUST -> {
                // A4 = baseFrequency, other notes relative to A via ratio table
                val ratioA = JUST_RATIOS[9] // A = 5/3
                baseFrequency * (JUST_RATIOS[pc] / ratioA) * Math.pow(2.0, octaveDiff.toDouble())
            }
            PYTHAGOREAN -> {
                val ratioA = PYTHAGOREAN_RATIOS[9] // A = 27/16
                baseFrequency * (PYTHAGOREAN_RATIOS[pc] / ratioA) * Math.pow(2.0, octaveDiff.toDouble())
            }
        }
    }

    companion object {
        private val JUST_RATIOS = doubleArrayOf(
            1.0, 16.0/15.0, 9.0/8.0, 6.0/5.0, 5.0/4.0, 4.0/3.0,
            45.0/32.0, 3.0/2.0, 8.0/5.0, 5.0/3.0, 9.0/5.0, 15.0/8.0
        )
        private val PYTHAGOREAN_RATIOS = doubleArrayOf(
            1.0, 2187.0/2048.0, 9.0/8.0, 32.0/27.0, 81.0/64.0, 4.0/3.0,
            729.0/512.0, 3.0/2.0, 128.0/81.0, 27.0/16.0, 16.0/9.0, 243.0/128.0
        )
    }
}
