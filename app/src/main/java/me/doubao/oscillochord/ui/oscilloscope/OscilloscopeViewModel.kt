package me.doubao.oscillochord.ui.oscilloscope

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.doubao.oscillochord.domain.audio.Oscillator
import me.doubao.oscillochord.domain.audio.Waveform
import me.doubao.oscillochord.domain.lissajous.LissajousProjector

data class TrailPoint(val x: Float, val y: Float)

data class OscilloscopeState(
    val trailPoints: List<TrailPoint> = emptyList()
)

class OscilloscopeViewModel : ViewModel() {
    private val projector = LissajousProjector()
    private val visualOscillators = mutableMapOf<Int, Oscillator>()
    private val delayLine = ArrayDeque<Float>()
    private val _state = MutableStateFlow(OscilloscopeState())
    val state: StateFlow<OscilloscopeState> = _state.asStateFlow()

    companion object {
        private const val SAMPLES_PER_FRAME = 256
        private const val MAX_TRAIL = 8192
        private const val DELAY_LENGTH = 64
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
            delayLine.clear()
            _state.value = OscilloscopeState(trailPoints = emptyList())
            return
        }

        val oscs = active.map { it.second }
        val points = _state.value.trailPoints.toMutableList()

        for (i in 0 until SAMPLES_PER_FRAME) {
            val raw = FloatArray(oscs.size) { j -> oscs[j].nextSample() }

            val sampleArray = if (raw.size == 1) {
                // N=1: use delay line for quadrature → ellipse instead of line
                delayLine.addLast(raw[0])
                if (delayLine.size > DELAY_LENGTH) delayLine.removeFirst()
                val delayed = delayLine.first()
                floatArrayOf(raw[0], delayed)
            } else {
                raw
            }

            val (x, y) = projector.project(sampleArray)
            points.add(TrailPoint(x, y))
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
        delayLine.clear()
    }
}
