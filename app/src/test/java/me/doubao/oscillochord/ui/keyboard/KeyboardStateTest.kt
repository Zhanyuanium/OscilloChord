package me.doubao.oscillochord.ui.keyboard

import me.doubao.oscillochord.domain.settings.NoteNamingSetting
import org.junit.Assert.*
import org.junit.Test

class KeyboardStateTest {

    @Test
    fun `default state has empty notes`() {
        val state = KeyboardState()
        assertTrue(state.activeNotes.isEmpty())
    }

    @Test
    fun `default octave start is 60`() {
        val state = KeyboardState()
        assertEquals(60, state.octaveStart)
    }

    @Test
    fun `copy preserves unchanged fields`() {
        val state = KeyboardState(octaveStart = 48, octaveCount = 2)
        val copy = state.copy(octaveStart = 60)
        assertEquals(60, copy.octaveStart)
        assertEquals(2, copy.octaveCount)
        assertTrue(copy.activeNotes.isEmpty())
        assertEquals(BlackKeyLayout.PIANO, copy.blackKeyLayout)
        assertTrue(copy.showNoteLabels)
        assertEquals(SlideMode.FOLLOW_KEYS, copy.slideMode)
        assertEquals(NoteNamingSetting.SHARP, copy.noteNaming)
    }

    @Test
    fun `equals works for identical states`() {
        val a = KeyboardState()
        val b = KeyboardState()
        assertEquals(a, b)
    }

    @Test
    fun `equals distinguishes different active notes`() {
        val a = KeyboardState(activeNotes = setOf(60, 64))
        val b = KeyboardState(activeNotes = setOf(60, 65))
        assertNotEquals(a, b)
    }

    @Test
    fun `hashCode consistent with equals`() {
        val a = KeyboardState(octaveStart = 48, octaveCount = 2, activeNotes = setOf(60))
        val b = KeyboardState(octaveStart = 48, octaveCount = 2, activeNotes = setOf(60))
        assertEquals(a.hashCode(), b.hashCode())
    }
}
