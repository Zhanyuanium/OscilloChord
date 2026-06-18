package me.doubao.oscillochord.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.doubao.oscillochord.domain.settings.*
import me.doubao.oscillochord.ui.settings.SettingsState

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
        val KEY_TRAIL_LENGTH = intPreferencesKey("trail_length")
        val KEY_VIEW_MODE = stringPreferencesKey("view_mode")
        val KEY_NOTE_NAMING = stringPreferencesKey("note_naming")
    }

    val settings: Flow<SettingsState> = context.dataStore.data.map { prefs ->
        SettingsState(
            octaveStart = prefs[KEY_OCTAVE_START] ?: 60,
            octaveCount = prefs[KEY_OCTAVE_COUNT] ?: 1,
            blackKeyLayout = try {
                BlackKeyLayoutSetting.valueOf(prefs[KEY_BLACK_KEY_LAYOUT] ?: "PIANO")
            } catch (_: Exception) { BlackKeyLayoutSetting.PIANO },
            slideMode = try {
                SlideModeSetting.valueOf(prefs[KEY_SLIDE_MODE] ?: "FOLLOW_KEYS")
            } catch (_: Exception) { SlideModeSetting.FOLLOW_KEYS },
            showNoteLabels = prefs[KEY_SHOW_NOTE_LABELS] ?: true,
            waveform = try {
                WaveformSetting.valueOf(prefs[KEY_WAVEFORM] ?: "SINE")
            } catch (_: Exception) { WaveformSetting.SINE },
            baseFrequency = prefs[KEY_BASE_FREQUENCY] ?: 440.0,
            tuningSystem = try {
                TuningSetting.valueOf(prefs[KEY_TUNING_SYSTEM] ?: "EQUAL")
            } catch (_: Exception) { TuningSetting.EQUAL },
            trailFadeEnabled = prefs[KEY_TRAIL_FADE] ?: true,
            trailLength = prefs[KEY_TRAIL_LENGTH] ?: 4096,
            viewMode = try {
                ViewModeSetting.valueOf(prefs[KEY_VIEW_MODE] ?: "SQUARE")
            } catch (_: Exception) { ViewModeSetting.SQUARE },
            noteNaming = try {
                NoteNamingSetting.valueOf(prefs[KEY_NOTE_NAMING] ?: "SHARP")
            } catch (_: Exception) { NoteNamingSetting.SHARP }
        )
    }

    suspend fun setOctaveStart(start: Int) { context.dataStore.edit { it[KEY_OCTAVE_START] = start } }
    suspend fun setOctaveCount(count: Int) { context.dataStore.edit { it[KEY_OCTAVE_COUNT] = count } }
    suspend fun setBlackKeyLayout(layout: BlackKeyLayoutSetting) { context.dataStore.edit { it[KEY_BLACK_KEY_LAYOUT] = layout.name } }
    suspend fun setSlideMode(mode: SlideModeSetting) { context.dataStore.edit { it[KEY_SLIDE_MODE] = mode.name } }
    suspend fun setShowNoteLabels(show: Boolean) { context.dataStore.edit { it[KEY_SHOW_NOTE_LABELS] = show } }
    suspend fun setWaveform(waveform: WaveformSetting) { context.dataStore.edit { it[KEY_WAVEFORM] = waveform.name } }
    suspend fun setBaseFrequency(hz: Double) { context.dataStore.edit { it[KEY_BASE_FREQUENCY] = hz } }
    suspend fun setTuningSystem(system: TuningSetting) { context.dataStore.edit { it[KEY_TUNING_SYSTEM] = system.name } }
    suspend fun setTrailFadeEnabled(enabled: Boolean) { context.dataStore.edit { it[KEY_TRAIL_FADE] = enabled } }
    suspend fun setTrailLength(length: Int) { context.dataStore.edit { it[KEY_TRAIL_LENGTH] = length } }
    suspend fun setViewMode(mode: ViewModeSetting) { context.dataStore.edit { it[KEY_VIEW_MODE] = mode.name } }
    suspend fun setNoteNaming(naming: NoteNamingSetting) { context.dataStore.edit { it[KEY_NOTE_NAMING] = naming.name } }
}
