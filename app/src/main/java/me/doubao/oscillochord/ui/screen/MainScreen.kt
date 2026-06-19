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
import org.koin.androidx.compose.koinViewModel
import me.doubao.oscillochord.domain.settings.*
import me.doubao.oscillochord.ui.info.InfoPanel
import me.doubao.oscillochord.ui.info.InfoViewModel
import me.doubao.oscillochord.ui.keyboard.KeyboardViewModel
import me.doubao.oscillochord.ui.keyboard.PianoKeyboard
import me.doubao.oscillochord.ui.oscilloscope.OscilloscopeView
import me.doubao.oscillochord.ui.oscilloscope.OscilloscopeViewModel
import me.doubao.oscillochord.ui.settings.SettingsPanel
import me.doubao.oscillochord.ui.settings.SettingsViewModel

@Composable
fun MainScreen(
    keyboardVM: KeyboardViewModel = koinViewModel(),
    oscilloscopeVM: OscilloscopeViewModel = koinViewModel(),
    infoVM: InfoViewModel = koinViewModel(),
    settingsVM: SettingsViewModel = koinViewModel()
) {
    val keyboardState by keyboardVM.state.collectAsStateWithLifecycle()
    val infoState by infoVM.state.collectAsStateWithLifecycle()
    val settingsState by settingsVM.state.collectAsStateWithLifecycle()

    // 按键变化时同步到 InfoViewModel（设置由 InfoViewModel 自行观察）
    LaunchedEffect(keyboardState.activeNotes) {
        infoVM.setActiveNotes(keyboardState.activeNotes)
    }

    val isWide = settingsState.viewMode == ViewModeSetting.WIDE

    val scopeBlock = @Composable {
        OscilloscopeView(
            activeNotes = keyboardState.activeNotes,
            baseFrequency = settingsState.baseFrequency,
            waveform = settingsState.waveform.waveform,
            tuningSystem = settingsState.tuningSystem.system,
            trailFadeEnabled = settingsState.trailFadeEnabled,
            trailLength = settingsState.trailLength,
            viewModel = oscilloscopeVM,
            modifier = Modifier.fillMaxSize()
        )
    }

    val settingsBlock = @Composable {
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
            onTrailLengthChange = { settingsVM.setTrailLength(it) },
            onViewModeChange = { settingsVM.setViewMode(it) },
            onNoteNamingChange = { settingsVM.setNoteNaming(it) },
            modifier = Modifier.width(240.dp).fillMaxHeight()
        )
    }

    val keyboardBlock = @Composable {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
            PianoKeyboard(
                state = keyboardState,
                onNoteOn = { keyboardVM.noteOn(it) },
                onNoteOff = { keyboardVM.noteOff(it) },
                onNoteSlide = { from, to -> keyboardVM.noteSlide(from, to) },
                onOctaveShift = { delta -> keyboardVM.shiftOctaveBy(delta) },
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 6.dp)
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (isWide) {
            Row(modifier = Modifier.fillMaxSize()) {
                InfoPanel(state = infoState, modifier = Modifier.width(240.dp).fillMaxHeight())
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Box(modifier = Modifier.weight(0.55f).fillMaxWidth(), contentAlignment = Alignment.Center) { scopeBlock() }
                    Box(modifier = Modifier.weight(0.45f).fillMaxWidth()) { keyboardBlock() }
                }
                settingsBlock()
            }
        } else {
            Row(modifier = Modifier.weight(0.55f).fillMaxWidth()) {
                InfoPanel(state = infoState, modifier = Modifier.width(240.dp).fillMaxHeight())
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) { scopeBlock() }
                settingsBlock()
            }
            Box(modifier = Modifier.weight(0.45f).fillMaxWidth()) { keyboardBlock() }
        }
    }
}
