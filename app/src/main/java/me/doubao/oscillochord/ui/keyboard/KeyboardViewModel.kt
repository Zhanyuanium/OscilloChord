package me.doubao.oscillochord.ui.keyboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import me.doubao.oscillochord.data.SettingsRepository
import me.doubao.oscillochord.domain.audio.AudioEngine
import me.doubao.oscillochord.domain.audio.Waveform
import me.doubao.oscillochord.domain.chord.TuningSystem
import me.doubao.oscillochord.domain.settings.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BlackKeyLayout { PIANO, EQUAL_WIDTH }
enum class SlideMode { FOLLOW_KEYS, SHIFT_OCTAVE }

data class KeyboardState(
    val activeNotes: Set<Int> = emptySet(),
    val octaveStart: Int = 60,
    val octaveCount: Int = 1,
    val blackKeyLayout: BlackKeyLayout = BlackKeyLayout.PIANO,
    val showNoteLabels: Boolean = true,
    val slideMode: SlideMode = SlideMode.FOLLOW_KEYS,
    val noteNaming: NoteNamingSetting = NoteNamingSetting.SHARP
)

class KeyboardViewModel(
    private val audioEngine: AudioEngine,
    private val repository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(KeyboardState())
    val state: StateFlow<KeyboardState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settings.collect { s ->
                setOctaveCount(s.octaveCount)
                setBlackKeyLayout(s.blackKeyLayout.layout)
                setSlideMode(s.slideMode.mode)
                setShowNoteLabels(s.showNoteLabels)
                setWaveform(s.waveform.waveform)
                setBaseFrequency(s.baseFrequency)
                setTuningSystem(s.tuningSystem.system)
                setNoteNaming(s.noteNaming)
            }
        }
    }

    private fun handleNoteOn(midiNote: Int) {
        _state.update { it.copy(activeNotes = it.activeNotes + midiNote) }
        audioEngine.noteOn(midiNote)
    }

    private fun handleNoteOff(midiNote: Int) {
        _state.update { it.copy(activeNotes = it.activeNotes - midiNote) }
        audioEngine.noteOff(midiNote)
    }

    fun noteOn(midiNote: Int) = handleNoteOn(midiNote)
    fun noteOff(midiNote: Int) = handleNoteOff(midiNote)

    fun noteSlide(from: Int, to: Int) {
        if (from != to) {
            handleNoteOff(from)
            handleNoteOn(to)
        }
    }

    fun setOctaveStart(start: Int) {
        _state.update { it.copy(octaveStart = start) }
    }

    fun shiftOctaveUp() { shiftOctaveBy(1) }
    fun shiftOctaveDown() { shiftOctaveBy(-1) }

    fun shiftOctaveBy(delta: Int) {
        _state.update { it.copy(octaveStart = (it.octaveStart + delta * 12).coerceAtLeast(0)) }
    }

    fun setOctaveCount(count: Int) {
        _state.update { it.copy(octaveCount = count.coerceIn(1, 5)) }
    }

    fun setBlackKeyLayout(layout: BlackKeyLayout) {
        _state.update { it.copy(blackKeyLayout = layout) }
    }

    fun setShowNoteLabels(show: Boolean) {
        _state.update { it.copy(showNoteLabels = show) }
    }

    fun setSlideMode(mode: SlideMode) {
        _state.update { it.copy(slideMode = mode) }
    }

    fun setWaveform(waveform: Waveform) {
        audioEngine.setWaveform(waveform)
    }

    fun setBaseFrequency(hz: Double) {
        audioEngine.setBaseFrequency(hz)
    }

    fun setTuningSystem(system: TuningSystem) {
        audioEngine.setTuningSystem(system)
    }

    fun setNoteNaming(naming: NoteNamingSetting) {
        _state.update { it.copy(noteNaming = naming) }
    }

    fun midiNoteOn(midiNote: Int) = handleNoteOn(midiNote)
    fun midiNoteOff(midiNote: Int) = handleNoteOff(midiNote)

    override fun onCleared() {
        super.onCleared()
        audioEngine.destroy()
    }
}
