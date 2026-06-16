package me.doubao.oscillochord.ui.oscilloscope

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import me.doubao.oscillochord.domain.chord.TuningSystem

@Composable
fun OscilloscopeView(
    activeNotes: Set<Int>,
    baseFrequency: Double,
    waveform: Waveform,
    tuningSystem: TuningSystem = TuningSystem.EQUAL,
    trailFadeEnabled: Boolean = true,
    viewModel: OscilloscopeViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val trailColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(activeNotes, baseFrequency, waveform, tuningSystem) {
        viewModel.syncWith(activeNotes, baseFrequency, waveform, tuningSystem)
    }

    LaunchedEffect(Unit) {
        while (isActive) { viewModel.generateFrame(); withFrameNanos { } }
    }

    Card(
        modifier = modifier.padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            drawSubtleGrid()
            if (trailFadeEnabled) drawTrailFade(state.trailPoints, trailColor)
            else drawTrailSimple(state.trailPoints, trailColor)
        }
    }
}

private fun DrawScope.drawSubtleGrid() {
    val gridColor = Color(0xFF1A1A30)
    val cx = size.width / 2f; val cy = size.height / 2f; val step = 50f
    var x = cx % step; while (x < size.width) { drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.5f); x += step }
    var y = cy % step; while (y < size.height) { drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f); y += step }
    val axisColor = Color(0xFF2A2A4A)
    drawLine(axisColor, Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 1f)
    drawLine(axisColor, Offset(0f, cy), Offset(size.width, cy), strokeWidth = 1f)
}

private fun DrawScope.drawTrailSimple(points: List<TrailPoint>, color: Color) {
    if (points.size < 2) return
    val halfW = size.width / 2f; val halfH = size.height / 2f
    val scale = minOf(halfW, halfH) * 0.85f
    val path = Path(); path.moveTo(halfW + points[0].x * scale, halfH + points[0].y * scale)
    for (i in 1 until points.size) {
        val pt = points[i]; path.lineTo(halfW + pt.x * scale, halfH + pt.y * scale)
    }
    drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawTrailFade(points: List<TrailPoint>, color: Color) {
    if (points.size < 2) return
    val halfW = size.width / 2f; val halfH = size.height / 2f
    val scale = minOf(halfW, halfH) * 0.85f
    val chunkSize = 32
    val totalChunks = (points.size + chunkSize - 1) / chunkSize
    for (chunk in 0 until totalChunks) {
        val chunkStart = chunk * chunkSize
        val chunkEnd = minOf(chunkStart + chunkSize, points.size)
        if (chunkEnd - chunkStart < 2) continue
        val midIndex = (chunkStart + chunkEnd) / 2
        val alpha = if (points.size > 1) midIndex.toFloat() / (points.size - 1) else 1.0f
        val path = Path(); var started = false
        for (i in chunkStart + 1 until chunkEnd) {
            val p0 = points[i-1]; val p1 = points[i]
            val x0 = halfW + p0.x * scale; val y0 = halfH + p0.y * scale
            if (x0 < -scale * 2 || x0 > size.width + scale * 2 || y0 < -scale * 2 || y0 > size.height + scale * 2) {
                if (started) { drawPath(path, color.copy(alpha = alpha.coerceIn(0f,1f)), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)); path.reset(); started = false }
                continue
            }
            if (!started) { path.moveTo(x0, y0); started = true }
            path.lineTo(halfW + p1.x * scale, halfH + p1.y * scale)
        }
        if (started) drawPath(path, color.copy(alpha = alpha.coerceIn(0f,1f)), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
