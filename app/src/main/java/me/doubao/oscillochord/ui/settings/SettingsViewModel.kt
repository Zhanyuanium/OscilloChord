package me.doubao.oscillochord.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import me.doubao.oscillochord.data.SettingsRepository
import me.doubao.oscillochord.domain.settings.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsState(
    val octaveStart: Int = 60,
    val octaveCount: Int = 1,
    val blackKeyLayout: BlackKeyLayoutSetting = BlackKeyLayoutSetting.PIANO,
    val slideMode: SlideModeSetting = SlideModeSetting.FOLLOW_KEYS,
    val showNoteLabels: Boolean = true,
    val waveform: WaveformSetting = WaveformSetting.SINE,
    val baseFrequency: Double = 440.0,
    val tuningSystem: TuningSetting = TuningSetting.EQUAL,
    val trailFadeEnabled: Boolean = true,
    val trailLength: Int = 4096,
    val viewMode: ViewModeSetting = ViewModeSetting.SQUARE,
    val noteNaming: NoteNamingSetting = NoteNamingSetting.SHARP
)

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settings.collect { settings ->
                _state.value = settings
            }
        }
    }

    fun setOctaveCount(count: Int) { viewModelScope.launch { repository.setOctaveCount(count) } }
    fun setBlackKeyLayout(layout: BlackKeyLayoutSetting) { viewModelScope.launch { repository.setBlackKeyLayout(layout) } }
    fun setSlideMode(mode: SlideModeSetting) { viewModelScope.launch { repository.setSlideMode(mode) } }
    fun setShowNoteLabels(show: Boolean) { viewModelScope.launch { repository.setShowNoteLabels(show) } }
    fun setWaveform(waveform: WaveformSetting) { viewModelScope.launch { repository.setWaveform(waveform) } }
    fun setBaseFrequency(hz: Double) { viewModelScope.launch { repository.setBaseFrequency(hz) } }
    fun setTuningSystem(system: TuningSetting) { viewModelScope.launch { repository.setTuningSystem(system) } }
    fun setTrailFadeEnabled(enabled: Boolean) { viewModelScope.launch { repository.setTrailFadeEnabled(enabled) } }
    fun setTrailLength(length: Int) { viewModelScope.launch { repository.setTrailLength(length) } }
    fun setViewMode(mode: ViewModeSetting) { viewModelScope.launch { repository.setViewMode(mode) } }
    fun setNoteNaming(naming: NoteNamingSetting) { viewModelScope.launch { repository.setNoteNaming(naming) } }
}
