package me.doubao.oscillochord.domain.audio

import org.junit.Assert.*
import org.junit.Test

class OscillatorTest {
    @Test
    fun `A4 oscillator has frequency 440Hz`() {
        val osc = Oscillator(midiNote = 69, baseFrequency = 440.0)
        assertEquals(440.0, osc.frequency, 0.01)
    }

    @Test
    fun `C4 oscillator has correct frequency`() {
        val osc = Oscillator(midiNote = 60, baseFrequency = 440.0)
        assertEquals(261.63, osc.frequency, 0.1)
    }

    @Test
    fun `sine wave generates samples in range`() {
        val osc = Oscillator(midiNote = 69, amplitude = 0.5f)
        val samples = (1..1000).map { osc.nextSample() }
        assertTrue(samples.all { it in -0.5f..0.5f })
    }

    @Test
    fun `square wave only generates 1 or -1`() {
        val osc = Oscillator(midiNote = 69, waveform = Waveform.SQUARE, amplitude = 0.5f)
        val samples = (1..100).map { osc.nextSample() }
        assertTrue(samples.all { it == 0.5f || it == -0.5f })
    }

    @Test
    fun `changing waveform works`() {
        val osc = Oscillator(midiNote = 69, waveform = Waveform.SINE)
        val sineSample = osc.nextSample()
        osc.waveform = Waveform.SQUARE
        val squareSample = osc.nextSample()
        assertNotEquals(sineSample, squareSample)
    }
}
