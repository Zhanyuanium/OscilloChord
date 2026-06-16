package me.doubao.oscillochord.ui.oscilloscope

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import me.doubao.oscillochord.domain.lissajous.LissajousProjector
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TrailPoint(val x: Float, val y: Float)

data class OscilloscopeState(
    val trailPoints: List<TrailPoint> = emptyList(),
    val maxTrailLength: Int = 4096
)

class OscilloscopeViewModel : ViewModel() {
    private val projector = LissajousProjector()
    private val _state = MutableStateFlow(OscilloscopeState())
    val state: StateFlow<OscilloscopeState> = _state.asStateFlow()

    fun collectSamples(sampleFlow: SharedFlow<Map<Int, Float>>) {
        viewModelScope.launch {
            sampleFlow.collect { snapshot ->
                if (snapshot.isEmpty()) {
                    _state.value = _state.value.copy(trailPoints = emptyList())
                    return@collect
                }
                val samples = snapshot.values.toFloatArray()
                val (x, y) = projector.project(samples)

                val points = _state.value.trailPoints.toMutableList()
                points.add(TrailPoint(x, y))
                if (points.size > _state.value.maxTrailLength) {
                    points.removeAt(0)
                }
                _state.value = _state.value.copy(trailPoints = points)
            }
        }
    }

    fun clear() {
        _state.value = _state.value.copy(trailPoints = emptyList())
    }
}
