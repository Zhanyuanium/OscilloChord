package me.doubao.oscillochord.ui.info

import androidx.lifecycle.ViewModel
import me.doubao.oscillochord.domain.chord.ChordDetector
import me.doubao.oscillochord.domain.chord.ChordResult
import me.doubao.oscillochord.domain.chord.PitchUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NoteInfo(
    val midiNote: Int,
    val name: String,
    val frequencyHz: Double,
    val intervalFromRoot: String,
    val isRoot: Boolean
)

data class InfoPanelState(
    val chordAbbreviation: String = "",
    val notes: List<NoteInfo> = emptyList()
)

class InfoViewModel : ViewModel() {
    private val detector = ChordDetector()
    private val _state = MutableStateFlow(InfoPanelState())
    val state: StateFlow<InfoPanelState> = _state.asStateFlow()

    fun updateNotes(activeNotes: Set<Int>, baseFrequency: Double = 440.0) {
        if (activeNotes.isEmpty()) {
            _state.value = InfoPanelState()
            return
        }

        val sortedNotes = activeNotes.sorted()
        val chordResult: ChordResult? = if (activeNotes.size >= 3) {
            detector.identify(activeNotes)
        } else null

        val rootNote = chordResult?.root ?: sortedNotes.first()

        val notes = sortedNotes.map { midi ->
            val semitonesFromRoot = midi - rootNote
            val interval = if (midi == rootNote) "根音"
            else PitchUtils.intervalName(((semitonesFromRoot % 12) + 12) % 12)

            NoteInfo(
                midiNote = midi,
                name = PitchUtils.midiNoteToName(midi),
                frequencyHz = PitchUtils.midiNoteToFrequency(midi, baseFrequency),
                intervalFromRoot = interval,
                isRoot = midi == rootNote
            )
        }

        _state.value = InfoPanelState(
            chordAbbreviation = chordResult?.let { result ->
                val rootName = PitchUtils.midiNoteToName(result.root).dropLast(1)
                "${rootName}${result.abbreviation}"
            } ?: "",
            notes = notes
        )
    }
}
