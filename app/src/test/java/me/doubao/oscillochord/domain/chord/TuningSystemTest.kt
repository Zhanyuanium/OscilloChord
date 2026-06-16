package me.doubao.oscillochord.domain.chord

import org.junit.Assert.assertEquals
import org.junit.Test

class TuningSystemTest {
    @Test
    fun `EQUAL A4 equals 440Hz`() {
        assertEquals(440.0, TuningSystem.EQUAL.frequencyForMidi(69, 440.0), 0.01)
    }

    @Test
    fun `EQUAL C4 equals 261_63Hz`() {
        assertEquals(261.63, TuningSystem.EQUAL.frequencyForMidi(60, 440.0), 0.01)
    }

    @Test
    fun `JUST A4 equals 440Hz`() {
        assertEquals(440.0, TuningSystem.JUST.frequencyForMidi(69, 440.0), 0.01)
    }

    @Test
    fun `JUST C4 equals 264Hz`() {
        // Just: C = 1/1, A = 5/3. C4 = A4 * (1 / (5/3)) = 440 * 3/5 = 264
        assertEquals(264.0, TuningSystem.JUST.frequencyForMidi(60, 440.0), 0.01)
    }

    @Test
    fun `PYTHAGOREAN A4 equals 440Hz`() {
        assertEquals(440.0, TuningSystem.PYTHAGOREAN.frequencyForMidi(69, 440.0), 0.01)
    }

    @Test
    fun `PYTHAGOREAN C4 equals 260_74Hz`() {
        // Pythagorean: C = 1/1, A = 27/16. C4 = A4 * (1 / (27/16)) = 440 * 16/27
        assertEquals(260.74, TuningSystem.PYTHAGOREAN.frequencyForMidi(60, 440.0), 0.1)
    }

    @Test
    fun `JUST E4 differs from equal`() {
        val equalFreq = TuningSystem.EQUAL.frequencyForMidi(64, 440.0)
        val justFreq = TuningSystem.JUST.frequencyForMidi(64, 440.0)
        assertEquals(329.63, equalFreq, 0.01)
        // Just: E = 5/4 relative to C. E4 = 264 * 5/4 = 330
        assertEquals(330.0, justFreq, 0.1)
    }

    @Test
    fun `PYTHAGOREAN E4 differs from equal`() {
        val pythFreq = TuningSystem.PYTHAGOREAN.frequencyForMidi(64, 440.0)
        val equalFreq = TuningSystem.EQUAL.frequencyForMidi(64, 440.0)
        assertEquals(329.63, equalFreq, 0.01)
        // Pythagorean E = 81/64, E4 = 260.74 * 81/64 ≈ 330.0
        assertEquals(330.0, pythFreq, 1.0)
    }

    @Test
    fun `PitchUtils delegates to TuningSystem`() {
        val equalFreq = PitchUtils.midiNoteToFrequency(69, 440.0, TuningSystem.EQUAL)
        val justFreq = PitchUtils.midiNoteToFrequency(69, 440.0, TuningSystem.JUST)
        assertEquals(440.0, equalFreq, 0.01)
        assertEquals(440.0, justFreq, 0.01)
    }
}
