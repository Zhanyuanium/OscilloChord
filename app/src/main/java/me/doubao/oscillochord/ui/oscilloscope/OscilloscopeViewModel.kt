package me.doubao.oscillochord.ui.oscilloscope

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.doubao.oscillochord.domain.audio.Oscillator
import me.doubao.oscillochord.domain.audio.Waveform
import kotlin.math.sqrt
import me.doubao.oscillochord.domain.lissajous.LissajousProjector

data class TrailPoint(val x: Float, val y: Float)

data class OscilloscopeState(
    val trailPoints: List<TrailPoint> = emptyList()
)

class OscilloscopeViewModel : ViewModel() {
    private val projector = LissajousProjector()
    private val visualOscillators = mutableMapOf<Int, Oscillator>()
    private val _state = MutableStateFlow(OscilloscopeState())
    val state: StateFlow<OscilloscopeState> = _state.asStateFlow()

    companion object {
        private const val SAMPLES_PER_FRAME = 256
        private const val MAX_TRAIL = 16384
    }

    fun syncWith(
        activeNotes: Set<Int>,
        baseFrequency: Double,
        waveform: Waveform
    ) {
        // Add new oscillators
        for (note in activeNotes) {
            if (!visualOscillators.containsKey(note)) {
                visualOscillators[note] = Oscillator(note, baseFrequency, waveform)
            }
        }
        // Remove released oscillators
        visualOscillators.keys.retainAll(activeNotes)
        // Sync parameters
        visualOscillators.values.forEach {
            it.baseFrequency = baseFrequency
            it.waveform = waveform
            it.amplitude = 1.0f
        }
    }

    fun generateFrame() {
        val active = visualOscillators.toList().sortedBy { it.first }

        if (active.isEmpty()) {
            _state.value = OscilloscopeState(trailPoints = emptyList())
            return
        }

        val oscs = active.map { it.second }
        val n = oscs.size
        // Normalization: for N≥3, projection sum can exceed [-1,1]; divide by √N
        val norm = if (n >= 3) 1.0f / sqrt(n.toFloat()) else 1.0f
        val points = _state.value.trailPoints.toMutableList()

        for (i in 0 until SAMPLES_PER_FRAME) {
            val raw = FloatArray(n) { j -> oscs[j].nextSample() }

            val projected = if (n == 1) {
                val ramp = (i.toFloat() / SAMPLES_PER_FRAME) * 2f - 1f
                ramp to raw[0]
            } else {
                val (px, py) = projector.project(raw)
                (px * norm) to (py * norm)
            }

            points.add(TrailPoint(projected.first, projected.second))
        }

        // Trim trail
        while (points.size > MAX_TRAIL) {
            points.subList(0, SAMPLES_PER_FRAME).clear()
        }

        _state.value = OscilloscopeState(trailPoints = points)
    }

    override fun onCleared() {
        super.onCleared()
        visualOscillators.clear()
    }
}
