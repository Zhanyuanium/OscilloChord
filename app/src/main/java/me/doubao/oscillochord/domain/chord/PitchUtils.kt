package me.doubao.oscillochord.domain.chord

import kotlin.math.pow

object PitchUtils {
    private const val A4_MIDI = 69

    private val NOTE_NAMES_SHARP = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
    private val NOTE_NAMES_FLAT = arrayOf("C", "D♭", "D", "E♭", "E", "F", "G♭", "G", "A♭", "A", "B♭", "B")

    fun midiNoteToFrequency(
        midiNote: Int,
        baseFrequency: Double = 440.0,
        tuningSystem: TuningSystem = TuningSystem.EQUAL
    ): Double {
        return tuningSystem.frequencyForMidi(midiNote, baseFrequency)
    }

    fun midiNoteToName(midiNote: Int, preferFlat: Boolean = false): String {
        val pc = pitchClass(midiNote)
        val octave = Math.floorDiv(midiNote, 12) - 1
        val noteNames = if (preferFlat) NOTE_NAMES_FLAT else NOTE_NAMES_SHARP
        return "${noteNames[pc]}$octave"
    }

    fun pitchClass(midiNote: Int): Int = Math.floorMod(midiNote, 12)

    fun intervalName(semitones: Int): String = when (semitones) {
        0 -> "根音"
        1 -> "小二度"
        2 -> "大二度"
        3 -> "小三度"
        4 -> "大三度"
        5 -> "纯四度"
        6 -> "增四度"
        7 -> "纯五度"
        8 -> "小六度"
        9 -> "大六度"
        10 -> "小七度"
        11 -> "大七度"
        else -> "${semitones}半音"
    }
}
