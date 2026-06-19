package me.doubao.oscillochord.ui.info

import me.doubao.oscillochord.domain.chord.ChordDetector
import org.junit.Assert.*
import org.junit.Test

class InfoViewModelTest {
    private val viewModel = InfoViewModel(ChordDetector())

    @Test
    fun `empty notes produces empty state`() {
        viewModel.setActiveNotes(emptySet())
        val state = viewModel.state.value
        assertEquals("", state.chordAbbreviation)
        assertTrue(state.notes.isEmpty())
    }

    @Test
    fun `single C4 note shows correct info`() {
        viewModel.setActiveNotes(setOf(60))
        val state = viewModel.state.value
        assertEquals(1, state.notes.size)
        assertEquals("C4", state.notes[0].name)
        assertTrue(state.notes[0].isRoot)
        assertEquals("根音", state.notes[0].intervalFromRoot)
    }

    @Test
    fun `C major triad detected correctly`() {
        viewModel.setActiveNotes(setOf(60, 64, 67))
        val state = viewModel.state.value
        assertTrue(state.chordAbbreviation.contains("M"))
        assertEquals(3, state.notes.size)
        val rootNote = state.notes.find { it.isRoot }
        assertNotNull(rootNote)
        assertEquals("C4", rootNote?.name)
    }

    @Test
    fun `A minor triad detected correctly`() {
        viewModel.setActiveNotes(setOf(69, 72, 76))
        val state = viewModel.state.value
        assertTrue(state.chordAbbreviation.contains("m"))
    }

    @Test
    fun `two notes produces no chord but shows both notes`() {
        viewModel.setActiveNotes(setOf(60, 64))
        val state = viewModel.state.value
        assertEquals("", state.chordAbbreviation)
        assertEquals(2, state.notes.size)
    }

    @Test
    fun `G7 dominant chord detected`() {
        viewModel.setActiveNotes(setOf(67, 71, 74, 77))
        val state = viewModel.state.value
        assertTrue(state.chordAbbreviation.contains("7"))
    }
}
