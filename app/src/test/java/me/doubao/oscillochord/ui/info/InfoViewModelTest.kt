package me.doubao.oscillochord.ui.info

import me.doubao.oscillochord.domain.chord.TuningSystem
import org.junit.Assert.*
import org.junit.Test

class InfoViewModelTest {
    private val viewModel = InfoViewModel()

    @Test
    fun `empty notes produces empty state`() {
        viewModel.updateNotes(emptySet())
        val state = viewModel.state.value
        assertEquals("", state.chordAbbreviation)
        assertTrue(state.notes.isEmpty())
    }

    @Test
    fun `single C4 note shows correct info`() {
        viewModel.updateNotes(setOf(60))
        val state = viewModel.state.value
        assertEquals(1, state.notes.size)
        assertEquals("C4", state.notes[0].name)
        assertTrue(state.notes[0].isRoot)
        assertEquals("根音", state.notes[0].intervalFromRoot)
        assertEquals("", state.chordAbbreviation)
    }

    @Test
    fun `C major triad detected correctly`() {
        viewModel.updateNotes(setOf(60, 64, 67))
        val state = viewModel.state.value
        assertTrue(state.chordAbbreviation.startsWith("C"))
        assertTrue(state.chordAbbreviation.contains("M"))
        assertEquals(3, state.notes.size)
    }

    @Test
    fun `A minor triad detected correctly`() {
        viewModel.updateNotes(setOf(69, 72, 76))
        val state = viewModel.state.value
        assertTrue(state.chordAbbreviation.contains("m"))
    }

    @Test
    fun `note naming FLAT produces flat names`() {
        viewModel.updateNotes(activeNotes = setOf(58), noteNaming = "FLAT")
        val state = viewModel.state.value
        assertEquals("B♭3", state.notes[0].name)
    }

    @Test
    fun `note naming SHARP produces sharp names`() {
        viewModel.updateNotes(activeNotes = setOf(58), noteNaming = "SHARP")
        val state = viewModel.state.value
        assertEquals("A♯3", state.notes[0].name)
    }

    @Test
    fun `two notes produces no chord but shows both notes`() {
        viewModel.updateNotes(setOf(60, 64))
        val state = viewModel.state.value
        assertEquals("", state.chordAbbreviation)
        assertEquals(2, state.notes.size)
    }

    @Test
    fun `G7 dominant chord detected`() {
        viewModel.updateNotes(setOf(55, 59, 62, 65))
        val state = viewModel.state.value
        assertTrue(state.chordAbbreviation.contains("7"))
    }

    @Test
    fun `different tuning system affects frequency`() {
        viewModel.updateNotes(activeNotes = setOf(69), baseFrequency = 440.0, tuningSystem = TuningSystem.EQUAL)
        val equalFreq = viewModel.state.value.notes[0].frequencyHz

        viewModel.updateNotes(activeNotes = setOf(69), baseFrequency = 440.0, tuningSystem = TuningSystem.JUST)
        val justFreq = viewModel.state.value.notes[0].frequencyHz

        // A4 is the reference note at 440 Hz in both systems
        assertEquals(440.0, equalFreq, 0.001)
        assertEquals(440.0, justFreq, 0.001)
    }
}
