package me.doubao.oscillochord.ui.keyboard

import androidx.lifecycle.ViewModel
import me.doubao.oscillochord.domain.audio.AudioEngine
import me.doubao.oscillochord.domain.audio.Waveform
import me.doubao.oscillochord.domain.chord.TuningSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BlackKeyLayout { PIANO, EQUAL_WIDTH }
enum class SlideMode { FOLLOW_KEYS, SHIFT_OCTAVE }

data class KeyboardState(
    val activeNotes: Set<Int> = emptySet(),
    val octaveStart: Int = 60,
    val octaveCount: Int = 1,
    val blackKeyLayout: BlackKeyLayout = BlackKeyLayout.PIANO,
    val showNoteLabels: Boolean = true,
    val slideMode: SlideMode = SlideMode.FOLLOW_KEYS,
    val noteNaming: String = "SHARP"
)

class KeyboardViewModel : ViewModel() {
    val audioEngine = AudioEngine()

    private val _state = MutableStateFlow(KeyboardState())
    val state: StateFlow<KeyboardState> = _state.asStateFlow()

    fun noteOn(midiNote: Int) = handleNoteOn(midiNote)

    fun noteOff(midiNote: Int) = handleNoteOff(midiNote)

    fun noteSlide(from: Int, to: Int) {
        if (from != to) {
            handleNoteOff(from)
            handleNoteOn(to)
        }
    }

    private fun handleNoteOn(midiNote: Int) {
        _state.value = _state.value.copy(
            activeNotes = _state.value.activeNotes + midiNote
        )
        audioEngine.noteOn(midiNote)
    }

    private fun handleNoteOff(midiNote: Int) {
        _state.value = _state.value.copy(
            activeNotes = _state.value.activeNotes - midiNote
        )
        audioEngine.noteOff(midiNote)
    }

    fun setOctaveStart(start: Int) {
        _state.value = _state.value.copy(octaveStart = start)
    }

    fun shiftOctaveUp() { shiftOctaveBy(1) }
    fun shiftOctaveDown() { shiftOctaveBy(-1) }

    fun shiftOctaveBy(delta: Int) {
        _state.value = _state.value.copy(
            octaveStart = (_state.value.octaveStart + delta * 12).coerceAtLeast(0)
        )
    }

    fun setOctaveCount(count: Int) {
        _state.value = _state.value.copy(octaveCount = count.coerceIn(1, 5))
    }

    fun setBlackKeyLayout(layout: BlackKeyLayout) {
        _state.value = _state.value.copy(blackKeyLayout = layout)
    }

    fun setShowNoteLabels(show: Boolean) {
        _state.value = _state.value.copy(showNoteLabels = show)
    }

    fun setSlideMode(mode: SlideMode) {
        _state.value = _state.value.copy(slideMode = mode)
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

    fun setNoteNaming(naming: String) {
        _state.value = _state.value.copy(noteNaming = naming)
    }

    // MIDI integration
    fun midiNoteOn(midiNote: Int) = handleNoteOn(midiNote)

    fun midiNoteOff(midiNote: Int) = handleNoteOff(midiNote)

    override fun onCleared() {
        super.onCleared()
        audioEngine.destroy()
    }
}
