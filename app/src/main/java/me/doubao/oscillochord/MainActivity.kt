package me.doubao.oscillochord

import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import me.doubao.oscillochord.domain.midi.MidiInputManager
import me.doubao.oscillochord.ui.keyboard.KeyboardViewModel
import me.doubao.oscillochord.ui.screen.MainScreen
import me.doubao.oscillochord.ui.theme.OscilloChordTheme

class MainActivity : ComponentActivity() {
    private lateinit var midiManager: MidiInputManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemUI()

        val keyboardVM: KeyboardViewModel by viewModel()

        try {
            midiManager = MidiInputManager(
                context = this,
                onNoteOn = { note -> keyboardVM.midiNoteOn(note) },
                onNoteOff = { note -> keyboardVM.midiNoteOff(note) }
            )
            midiManager.startScan()
        } catch (e: Exception) {
            Log.w("MainActivity", "Failed to initialize MIDI input", e)
        }

        setContent {
            OscilloChordTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(keyboardVM = keyboardVM)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        window.insetsController?.let { controller ->
            controller.hide(WindowInsets.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::midiManager.isInitialized) midiManager.destroy()
    }
}
