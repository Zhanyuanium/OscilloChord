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
    fun `JUST C4 equals 261_63Hz`() {
        assertEquals(261.63, TuningSystem.JUST.frequencyForMidi(60, 440.0), 0.01)
    }

    @Test
    fun `PYTHAGOREAN A4 equals 440Hz`() {
        assertEquals(440.0, TuningSystem.PYTHAGOREAN.frequencyForMidi(69, 440.0), 0.01)
    }

    @Test
    fun `PYTHAGOREAN C4 equals 261_63Hz`() {
        assertEquals(261.63, TuningSystem.PYTHAGOREAN.frequencyForMidi(60, 440.0), 0.01)
    }

    @Test
    fun `JUST ratios produce different frequencies than equal`() {
        val justFreq = TuningSystem.JUST.frequencyForMidi(64, 440.0) // E4
        val equalFreq = TuningSystem.EQUAL.frequencyForMidi(64, 440.0)
        assertEquals(329.63, equalFreq, 0.01)
        // Just E = 330.0 (5/4 * 264)
        assertEquals(330.0, justFreq, 0.1)
    }

    @Test
    fun `PYTHAGOREAN ratios produce different frequencies than equal`() {
        val pythFreq = TuningSystem.PYTHAGOREAN.frequencyForMidi(64, 440.0) // E4
        val equalFreq = TuningSystem.EQUAL.frequencyForMidi(64, 440.0)
        assertEquals(329.63, equalFreq, 0.01)
        // Pythagorean E = 81/64 * C4
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
