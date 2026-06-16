package me.doubao.oscillochord.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import me.doubao.oscillochord.data.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsState(
    val octaveStart: Int = 60,
    val octaveCount: Int = 1,
    val blackKeyLayout: String = "PIANO",
    val slideMode: String = "FOLLOW_KEYS",
    val showNoteLabels: Boolean = true,
    val waveform: String = "SINE",
    val baseFrequency: Double = 440.0,
    val tuningSystem: String = "EQUAL",
    val trailFadeEnabled: Boolean = true
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settings.collect { prefs ->
                _state.value = SettingsState(
                    octaveStart = prefs["octave_start"] as? Int ?: 60,
                    octaveCount = prefs["octave_count"] as? Int ?: 1,
                    blackKeyLayout = prefs["black_key_layout"] as? String ?: "PIANO",
                    slideMode = prefs["slide_mode"] as? String ?: "FOLLOW_KEYS",
                    showNoteLabels = prefs["show_note_labels"] as? Boolean ?: true,
                    waveform = prefs["waveform"] as? String ?: "SINE",
                    baseFrequency = prefs["base_frequency"] as? Double ?: 440.0,
                    tuningSystem = prefs["tuning_system"] as? String ?: "EQUAL",
                    trailFadeEnabled = prefs["trail_fade_enabled"] as? Boolean ?: true
                )
            }
        }
    }

    fun setOctaveCount(count: Int) { viewModelScope.launch { repository.setOctaveCount(count) } }
    fun setBlackKeyLayout(layout: String) { viewModelScope.launch { repository.setBlackKeyLayout(layout) } }
    fun setSlideMode(mode: String) { viewModelScope.launch { repository.setSlideMode(mode) } }
    fun setShowNoteLabels(show: Boolean) { viewModelScope.launch { repository.setShowNoteLabels(show) } }
    fun setWaveform(waveform: String) { viewModelScope.launch { repository.setWaveform(waveform) } }
    fun setBaseFrequency(hz: Double) { viewModelScope.launch { repository.setBaseFrequency(hz) } }
    fun setTuningSystem(system: String) { viewModelScope.launch { repository.setTuningSystem(system) } }
    fun setTrailFadeEnabled(enabled: Boolean) { viewModelScope.launch { repository.setTrailFadeEnabled(enabled) } }
}
