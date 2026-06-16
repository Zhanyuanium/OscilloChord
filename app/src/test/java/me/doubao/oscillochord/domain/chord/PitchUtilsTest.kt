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
    fun `intervalName returns correct names`() {
        assertEquals("根音", PitchUtils.intervalName(0))
        assertEquals("小二度", PitchUtils.intervalName(1))
        assertEquals("大二度", PitchUtils.intervalName(2))
        assertEquals("小三度", PitchUtils.intervalName(3))
        assertEquals("大三度", PitchUtils.intervalName(4))
        assertEquals("纯四度", PitchUtils.intervalName(5))
        assertEquals("增四度", PitchUtils.intervalName(6))
        assertEquals("纯五度", PitchUtils.intervalName(7))
        assertEquals("小六度", PitchUtils.intervalName(8))
        assertEquals("大六度", PitchUtils.intervalName(9))
        assertEquals("小七度", PitchUtils.intervalName(10))
        assertEquals("大七度", PitchUtils.intervalName(11))
    }
}
