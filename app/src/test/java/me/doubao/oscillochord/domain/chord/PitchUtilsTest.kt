package me.doubao.oscillochord.domain.chord

import org.junit.Assert.assertEquals
import org.junit.Test

class PitchUtilsTest {
    @Test
    fun `midiNoteToFrequency A4 equals 440Hz`() {
        assertEquals(440.0, PitchUtils.midiNoteToFrequency(69, 440.0), 0.01)
    }

    @Test
    fun `midiNoteToFrequency C4 equals 261_63Hz`() {
        assertEquals(261.63, PitchUtils.midiNoteToFrequency(60, 440.0), 0.01)
    }

    @Test
    fun `midiNoteToFrequency A4 equals 432Hz when base is 432`() {
        assertEquals(432.0, PitchUtils.midiNoteToFrequency(69, 432.0), 0.01)
    }

    @Test
    fun `midiNoteToName C4`() {
        assertEquals("C4", PitchUtils.midiNoteToName(60))
    }

    @Test
    fun `midiNoteToName A4`() {
        assertEquals("A4", PitchUtils.midiNoteToName(69))
    }

    @Test
    fun `midiNoteToName CSharp4`() {
        assertEquals("C♯4", PitchUtils.midiNoteToName(61))
    }

    @Test
    fun `midiNoteToName with flat`() {
        assertEquals("B♭3", PitchUtils.midiNoteToName(58, preferFlat = true))
    }

    @Test
    fun `midiNoteToName sharp is default`() {
        assertEquals("A♯3", PitchUtils.midiNoteToName(58))
    }

    @Test
    fun `pitchClass returns 0 for C`() {
        assertEquals(0, PitchUtils.pitchClass(60))
        assertEquals(0, PitchUtils.pitchClass(72))
    }

    @Test
    fun `intervalSemitones returns correct pitch class distances`() {
        assertEquals(0, PitchUtils.intervalSemitones(0))
        assertEquals(1, PitchUtils.intervalSemitones(1))
        assertEquals(2, PitchUtils.intervalSemitones(2))
        assertEquals(3, PitchUtils.intervalSemitones(3))
        assertEquals(4, PitchUtils.intervalSemitones(4))
        assertEquals(5, PitchUtils.intervalSemitones(5))
        assertEquals(6, PitchUtils.intervalSemitones(6))
        assertEquals(7, PitchUtils.intervalSemitones(7))
        assertEquals(8, PitchUtils.intervalSemitones(8))
        assertEquals(9, PitchUtils.intervalSemitones(9))
        assertEquals(10, PitchUtils.intervalSemitones(10))
        assertEquals(11, PitchUtils.intervalSemitones(11))
    }

    @Test
    fun `intervalSemitones wraps around for values above 11`() {
        assertEquals(0, PitchUtils.intervalSemitones(12))
        assertEquals(1, PitchUtils.intervalSemitones(13))
        assertEquals(5, PitchUtils.intervalSemitones(17))
    }
}
