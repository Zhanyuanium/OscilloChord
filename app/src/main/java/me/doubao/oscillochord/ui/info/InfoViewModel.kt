package me.doubao.oscillochord.ui.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import me.doubao.oscillochord.data.SettingsRepository
import me.doubao.oscillochord.domain.chord.ChordDetector
import me.doubao.oscillochord.domain.chord.ChordResult
import me.doubao.oscillochord.domain.chord.PitchUtils
import me.doubao.oscillochord.domain.settings.NoteNamingSetting
import me.doubao.oscillochord.ui.settings.SettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

class InfoViewModel(
    private val detector: ChordDetector,
    private val repository: SettingsRepository? = null
) : ViewModel() {
    private val _state = MutableStateFlow(InfoPanelState())
    val state: StateFlow<InfoPanelState> = _state.asStateFlow()

    private var activeNotes: Set<Int> = emptySet()
    private var settings: SettingsState = SettingsState()

    init {
        if (repository != null) {
            viewModelScope.launch {
                repository.settings.collect { s ->
                    settings = s
                    recompute()
                }
            }
        }
    }

    fun setActiveNotes(notes: Set<Int>) {
        activeNotes = notes
        recompute()
    }

    private fun recompute() {
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
            else PitchUtils.intervalName(PitchUtils.pitchClass(semitonesFromRoot))

            NoteInfo(
                midiNote = midi,
                name = PitchUtils.midiNoteToName(midi, settings.noteNaming == NoteNamingSetting.FLAT),
                frequencyHz = PitchUtils.midiNoteToFrequency(midi, settings.baseFrequency, settings.tuningSystem.system),
                intervalFromRoot = interval,
                isRoot = midi == rootNote
            )
        }

        _state.value = InfoPanelState(
            chordAbbreviation = chordResult?.let { result ->
                val rootName = PitchUtils.midiNoteToName(result.root, settings.noteNaming == NoteNamingSetting.FLAT).dropLast(1)
                "${rootName}${result.abbreviation}"
            } ?: "",
            notes = notes
        )
    }
}
