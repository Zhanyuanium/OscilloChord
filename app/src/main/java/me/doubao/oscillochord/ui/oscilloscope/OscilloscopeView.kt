package me.doubao.oscillochord.ui.oscilloscope

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun OscilloscopeView(
    state: OscilloscopeState,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .background(Color(0xFF0A0A1A))
            .padding(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val points = state.trailPoints
            if (points.size < 2) return@Canvas

            val halfW = size.width / 2f
            val halfH = size.height / 2f
            val scale = minOf(halfW, halfH) * 0.9f

            val path = Path()
            val first = points.first()
            path.moveTo(halfW + first.x * scale, halfH + first.y * scale)

            for (i in 1 until points.size) {
                val pt = points[i]
                path.lineTo(halfW + pt.x * scale, halfH + pt.y * scale)
            }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
