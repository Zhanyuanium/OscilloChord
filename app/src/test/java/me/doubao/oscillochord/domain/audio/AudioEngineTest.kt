package me.doubao.oscillochord.domain.audio

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
        assertTrue(engine.activeNotes.contains(60))
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
    fun `allNotesOff clears all`() {
        engine.noteOn(60)
        engine.noteOn(64)
        engine.noteOn(67)
        engine.allNotesOff()
        assertEquals(0, engine.activeNoteCount)
    }

    @Test
    fun `setWaveform does not crash`() {
        engine.noteOn(60)
        engine.setWaveform(Waveform.SQUARE)
    }

    @Test
    fun `setBaseFrequency does not crash`() {
        engine.noteOn(69)
        engine.setBaseFrequency(432.0)
    }
}
