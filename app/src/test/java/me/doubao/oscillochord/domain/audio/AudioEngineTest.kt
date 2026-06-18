package me.doubao.oscillochord.domain.audio

import me.doubao.oscillochord.domain.chord.TuningSystem
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class AudioEngineTest {
    private val engine = AudioEngine()

    @After
    fun tearDown() {
        engine.destroy()
    }

    @Test
    fun `noteOn adds oscillator`() {
        engine.noteOn(60)
        assertEquals(1, engine.activeNoteCount)
    }

    @Test
    fun `noteOff removes oscillator`() {
        engine.noteOn(60)
        engine.noteOff(60)
        assertEquals(0, engine.activeNoteCount)
    }

    @Test
    fun `duplicate noteOn is idempotent`() {
        engine.noteOn(60)
        engine.noteOn(60)
        assertEquals(1, engine.activeNoteCount)
    }

    @Test
    fun `clearing all oscillators works`() {
        engine.noteOn(60)
        engine.noteOn(64)
        engine.noteOn(67)
        engine.noteOff(60)
        engine.noteOff(64)
        engine.noteOff(67)
        assertEquals(0, engine.activeNoteCount)
    }

    @Test
    fun `setWaveform updates active oscillators`() {
        engine.noteOn(60)
        engine.noteOn(64)
        engine.setWaveform(Waveform.SQUARE)
        assertEquals(2, engine.activeNoteCount)
    }

    @Test
    fun `setBaseFrequency does not change active note count`() {
        engine.noteOn(69)
        engine.setBaseFrequency(432.0)
        assertEquals(1, engine.activeNoteCount)
    }

    @Test
    fun `setTuningSystem does not change active note count`() {
        engine.noteOn(69)
        engine.setTuningSystem(TuningSystem.JUST)
        assertEquals(1, engine.activeNoteCount)
    }

    @Test
    fun `noteOff nonexistent note is no-op`() {
        engine.noteOn(60)
        engine.noteOff(999)
        assertEquals(1, engine.activeNoteCount)
    }

    @Test
    fun `destroy clears all oscillators`() {
        engine.noteOn(60)
        engine.noteOn(64)
        engine.noteOn(67)
        engine.destroy()
        assertEquals(0, engine.activeNoteCount)
    }

    @Test
    fun `intermediate state checks pass while clearing`() {
        engine.noteOn(60)
        engine.noteOn(64)
        engine.noteOn(67)
        assertEquals(3, engine.activeNoteCount)
        engine.noteOff(60)
        assertEquals(2, engine.activeNoteCount)
        engine.noteOff(64)
        assertEquals(1, engine.activeNoteCount)
        engine.noteOff(67)
        assertEquals(0, engine.activeNoteCount)
    }
}
