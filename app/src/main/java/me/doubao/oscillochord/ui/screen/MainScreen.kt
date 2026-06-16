package me.doubao.oscillochord.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.doubao.oscillochord.ui.keyboard.KeyboardViewModel
import me.doubao.oscillochord.ui.keyboard.PianoKeyboard

@Composable
fun MainScreen(
    keyboardVM: KeyboardViewModel = viewModel()
) {
    val keyboardState by keyboardVM.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top half: info + oscilloscope + settings (placeholder)
        Row(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .weight(0.18f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Text("和弦信息", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
            Box(
                modifier = Modifier
                    .weight(0.62f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text("示波器", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            }
            Box(
                modifier = Modifier
                    .weight(0.20f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Text("设置", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        // Bottom: piano keyboard
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
