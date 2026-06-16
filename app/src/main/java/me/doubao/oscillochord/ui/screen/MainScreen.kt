package me.doubao.oscillochord.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.doubao.oscillochord.domain.audio.Waveform
import me.doubao.oscillochord.domain.chord.TuningSystem
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
    val infoState by infoVM.state.collectAsStateWithLifecycle()
    val settingsState by settingsVM.state.collectAsStateWithLifecycle()

    // Wire active notes → info panel
    LaunchedEffect(keyboardState.activeNotes, settingsState.tuningSystem) {
        infoVM.updateNotes(keyboardState.activeNotes, settingsState.baseFrequency,
            TuningSystem.valueOf(settingsState.tuningSystem))
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
    LaunchedEffect(settingsState.tuningSystem) {
        keyboardVM.setTuningSystem(TuningSystem.valueOf(settingsState.tuningSystem))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top half — side panels fixed width, oscilloscope fills remainder
        Row(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxWidth()
        ) {
            InfoPanel(
                state = infoState,
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight()
            )
            // Oscilloscope fills remaining width
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                OscilloscopeView(
                    activeNotes = keyboardState.activeNotes,
                    baseFrequency = settingsState.baseFrequency,
                    waveform = Waveform.valueOf(settingsState.waveform),
                    tuningSystem = TuningSystem.valueOf(settingsState.tuningSystem),
                    trailFadeEnabled = settingsState.trailFadeEnabled,
                    viewModel = oscilloscopeVM,
                    modifier = Modifier.fillMaxSize()
                )
            }
            SettingsPanel(
                state = settingsState,
                onOctaveCountChange = { settingsVM.setOctaveCount(it) },
                onBlackKeyLayoutChange = { settingsVM.setBlackKeyLayout(it) },
                onSlideModeChange = { settingsVM.setSlideMode(it) },
                onShowNoteLabelsChange = { settingsVM.setShowNoteLabels(it) },
                onWaveformChange = { settingsVM.setWaveform(it) },
                onBaseFrequencyChange = { settingsVM.setBaseFrequency(it) },
                onTuningSystemChange = { settingsVM.setTuningSystem(it) },
                onTrailFadeChange = { settingsVM.setTrailFadeEnabled(it) },
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight()
            )
        }

        // Bottom: piano keyboard with background
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().weight(0.45f)
        ) {
            PianoKeyboard(
                state = keyboardState,
                onNoteOn = { keyboardVM.noteOn(it) },
                onNoteOff = { keyboardVM.noteOff(it) },
                onNoteSlide = { from, to -> keyboardVM.noteSlide(from, to) },
                onOctaveShift = { delta ->
                    if (delta > 0) keyboardVM.shiftOctaveUp()
                    else keyboardVM.shiftOctaveDown()
                },
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 6.dp)
            )
        }
    }
}
