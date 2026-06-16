package me.doubao.oscillochord.ui.oscilloscope

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.isActive
import me.doubao.oscillochord.domain.audio.Waveform

@Composable
fun OscilloscopeView(
    activeNotes: Set<Int>,
    baseFrequency: Double,
    waveform: Waveform,
    viewModel: OscilloscopeViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val trailColor = MaterialTheme.colorScheme.primary

    // Sync visual oscillators when notes change
    LaunchedEffect(activeNotes, baseFrequency, waveform) {
        viewModel.syncWith(activeNotes, baseFrequency, waveform)
    }

    // Generate samples on every display frame
    LaunchedEffect(Unit) {
        while (isActive) {
            viewModel.generateFrame()
            withFrameNanos { } // wait for next vsync
        }
    }

    Box(
        modifier = modifier.then(
            Modifier.drawBehind { drawRect(Color(0xFF0D0D1A)) }
        )
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            drawSubtleGrid()
            drawTrail(state.trailPoints, trailColor)
        }
    }
}

private fun DrawScope.drawSubtleGrid() {
    val gridColor = Color(0xFF141428)
    val cx = size.width / 2f
    val cy = size.height / 2f
    val step = 50f

    var x = cx % step
    while (x < size.width) {
        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.5f)
        x += step
    }
    var y = cy % step
    while (y < size.height) {
        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
        y += step
    }
    // Center crosshair
    val axisColor = Color(0xFF1E1E3A)
    drawLine(axisColor, Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 1f)
    drawLine(axisColor, Offset(0f, cy), Offset(size.width, cy), strokeWidth = 1f)
}

private fun DrawScope.drawTrail(points: List<TrailPoint>, color: Color) {
    if (points.size < 2) return

    val halfW = size.width / 2f
    val halfH = size.height / 2f
    val scale = minOf(halfW, halfH) * 0.85f

    val path = Path()
    var started = false

    for (i in 1 until points.size) {
        val p0 = points[i - 1]
        val p1 = points[i]
        val x0 = halfW + p0.x * scale
        val y0 = halfH + p0.y * scale
        val x1 = halfW + p1.x * scale
        val y1 = halfH + p1.y * scale

        // Skip out-of-bounds points (break the path on discontinuity)
        val outOfBounds = x0 < -scale * 2 || x0 > size.width + scale * 2 ||
            y0 < -scale * 2 || y0 > size.height + scale * 2
        if (outOfBounds) {
            if (started) {
                drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                path.reset()
                started = false
            }
            continue
        }

        if (!started) {
            path.moveTo(x0, y0)
            started = true
        }
        path.lineTo(x1, y1)
    }

    if (started) {
        drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
