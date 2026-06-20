package me.doubao.oscillochord.domain.chord

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

    fun intervalSemitones(midiFromRoot: Int): Int = PitchUtils.pitchClass(midiFromRoot)
}
