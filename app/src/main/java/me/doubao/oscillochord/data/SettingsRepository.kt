package me.doubao.oscillochord.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val KEY_OCTAVE_START = intPreferencesKey("octave_start")
        val KEY_OCTAVE_COUNT = intPreferencesKey("octave_count")
        val KEY_BLACK_KEY_LAYOUT = stringPreferencesKey("black_key_layout")
        val KEY_SLIDE_MODE = stringPreferencesKey("slide_mode")
        val KEY_SHOW_NOTE_LABELS = booleanPreferencesKey("show_note_labels")
        val KEY_WAVEFORM = stringPreferencesKey("waveform")
        val KEY_BASE_FREQUENCY = doublePreferencesKey("base_frequency")
        val KEY_TUNING_SYSTEM = stringPreferencesKey("tuning_system")
        val KEY_TRAIL_FADE = booleanPreferencesKey("trail_fade_enabled")
    }

    val settings: Flow<Map<String, Any>> = context.dataStore.data.map { prefs ->
        mapOf(
            "octave_start" to (prefs[KEY_OCTAVE_START] ?: 60),
            "octave_count" to (prefs[KEY_OCTAVE_COUNT] ?: 1),
            "black_key_layout" to (prefs[KEY_BLACK_KEY_LAYOUT] ?: "PIANO"),
            "slide_mode" to (prefs[KEY_SLIDE_MODE] ?: "FOLLOW_KEYS"),
            "show_note_labels" to (prefs[KEY_SHOW_NOTE_LABELS] ?: true),
            "waveform" to (prefs[KEY_WAVEFORM] ?: "SINE"),
            "base_frequency" to (prefs[KEY_BASE_FREQUENCY] ?: 440.0),
            "tuning_system" to (prefs[KEY_TUNING_SYSTEM] ?: "EQUAL"),
            "trail_fade_enabled" to (prefs[KEY_TRAIL_FADE] ?: true)
        )
    }

    suspend fun setOctaveStart(start: Int) { context.dataStore.edit { it[KEY_OCTAVE_START] = start } }
    suspend fun setOctaveCount(count: Int) { context.dataStore.edit { it[KEY_OCTAVE_COUNT] = count } }
    suspend fun setBlackKeyLayout(layout: String) { context.dataStore.edit { it[KEY_BLACK_KEY_LAYOUT] = layout } }
    suspend fun setSlideMode(mode: String) { context.dataStore.edit { it[KEY_SLIDE_MODE] = mode } }
    suspend fun setShowNoteLabels(show: Boolean) { context.dataStore.edit { it[KEY_SHOW_NOTE_LABELS] = show } }
    suspend fun setWaveform(waveform: String) { context.dataStore.edit { it[KEY_WAVEFORM] = waveform } }
    suspend fun setBaseFrequency(hz: Double) { context.dataStore.edit { it[KEY_BASE_FREQUENCY] = hz } }
    suspend fun setTuningSystem(system: String) { context.dataStore.edit { it[KEY_TUNING_SYSTEM] = system } }
    suspend fun setTrailFadeEnabled(enabled: Boolean) { context.dataStore.edit { it[KEY_TRAIL_FADE] = enabled } }
}
