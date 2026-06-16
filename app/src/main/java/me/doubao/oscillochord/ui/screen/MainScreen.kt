package me.doubao.oscillochord.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.doubao.oscillochord.domain.audio.Waveform
import me.doubao.oscillochord.ui.info.InfoPanel
import me.doubao.oscillochord.ui.info.InfoViewModel
import me.doubao.oscillochord.ui.keyboard.BlackKeyLayout
import me.doubao.oscillochord.ui.keyboard.KeyboardViewModel
import me.doubao.oscillochord.ui.keyboard.PianoKeyboard
import me.doubao.oscillochord.ui.keyboard.SlideMode
import me.doubao.oscillochord.ui.oscilloscope.OscilloscopeView
import me.doubao.oscillochord.ui.oscilloscope.OscilloscopeViewModel
import me.doubao.oscillochord.ui.settings.SettingsPanel
import me.doubao.oscillochord.ui.settings.SettingsViewModel

@Composable
fun MainScreen(
    keyboardVM: KeyboardViewModel = viewModel(),
    oscilloscopeVM: OscilloscopeViewModel = viewModel(),
    infoVM: InfoViewModel = viewModel(),
    settingsVM: SettingsViewModel = viewModel()
) {
    val keyboardState by keyboardVM.state.collectAsStateWithLifecycle()
    val oscilloscopeState by oscilloscopeVM.state.collectAsStateWithLifecycle()
    val infoState by infoVM.state.collectAsStateWithLifecycle()
    val settingsState by settingsVM.state.collectAsStateWithLifecycle()

    // Wire audio samples → oscilloscope
    LaunchedEffect(Unit) {
        oscilloscopeVM.collectSamples(keyboardVM.sampleSnapshot)
    }

    // Wire active notes → info panel
    LaunchedEffect(keyboardState.activeNotes) {
        infoVM.updateNotes(keyboardState.activeNotes, settingsState.baseFrequency)
    }

    // Wire settings → keyboard VM
    LaunchedEffect(settingsState.octaveCount) {
        keyboardVM.setOctaveCount(settingsState.octaveCount)
    }
    LaunchedEffect(settingsState.blackKeyLayout) {
        keyboardVM.setBlackKeyLayout(
            if (settingsState.blackKeyLayout == "PIANO") BlackKeyLayout.PIANO else BlackKeyLayout.EQUAL_WIDTH
        )
    }
    LaunchedEffect(settingsState.slideMode) {
        keyboardVM.setSlideMode(
            if (settingsState.slideMode == "FOLLOW_KEYS") SlideMode.FOLLOW_KEYS else SlideMode.SHIFT_OCTAVE
        )
    }
    LaunchedEffect(settingsState.showNoteLabels) {
        keyboardVM.setShowNoteLabels(settingsState.showNoteLabels)
    }
    LaunchedEffect(settingsState.waveform) {
        keyboardVM.setWaveform(Waveform.valueOf(settingsState.waveform))
    }
    LaunchedEffect(settingsState.baseFrequency) {
        keyboardVM.setBaseFrequency(settingsState.baseFrequency)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top half
        Row(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxWidth()
        ) {
            InfoPanel(
                state = infoState,
                modifier = Modifier.weight(0.18f).fillMaxHeight()
            )
            OscilloscopeView(
                state = oscilloscopeState,
                modifier = Modifier.weight(0.62f).fillMaxHeight()
            )
            SettingsPanel(
                state = settingsState,
                onOctaveCountChange = { settingsVM.setOctaveCount(it) },
                onBlackKeyLayoutChange = { settingsVM.setBlackKeyLayout(it) },
                onSlideModeChange = { settingsVM.setSlideMode(it) },
                onShowNoteLabelsChange = { settingsVM.setShowNoteLabels(it) },
                onWaveformChange = { settingsVM.setWaveform(it) },
                onBaseFrequencyChange = { settingsVM.setBaseFrequency(it) },
                modifier = Modifier.weight(0.20f).fillMaxHeight()
            )
        }

        // Bottom: piano keyboard (fills remaining space)
        PianoKeyboard(
            state = keyboardState,
            onNoteOn = { keyboardVM.noteOn(it) },
            onNoteOff = { keyboardVM.noteOff(it) },
            onNoteSlide = { from, to -> keyboardVM.noteSlide(from, to) },
            onOctaveShift = { delta ->
                if (delta > 0) keyboardVM.shiftOctaveUp()
                else keyboardVM.shiftOctaveDown()
            },
            modifier = Modifier.fillMaxWidth().weight(0.45f)
        )
    }
}
