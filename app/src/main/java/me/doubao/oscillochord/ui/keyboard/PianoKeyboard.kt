package me.doubao.oscillochord.ui.keyboard

import android.graphics.Paint
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import me.doubao.oscillochord.domain.chord.PitchUtils
import me.doubao.oscillochord.ui.theme.OscilloBlackKey
import me.doubao.oscillochord.ui.theme.OscilloWhiteKey
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TAG = "PianoKeyboard"
private val WHITE_KEY_OFFSETS = listOf(0, 2, 4, 5, 7, 9, 11)
private val BLACK_KEY_DATA = listOf(0 to 1, 1 to 3, 3 to 6, 4 to 8, 5 to 10)

private data class FlingRequest(val offset: Float, val velocityPxPerMs: Float)

private fun computeVelocity(samples: List<Pair<Long, Float>>): Float {
    if (samples.size < 2) return 0f
    val dtNs = samples.last().first - samples.first().first
    if (dtNs <= 0L) return 0f
    val totalDx = samples.sumOf { it.second.toDouble() }.toFloat()
    return totalDx / (dtNs / 1_000_000_000f) / 1000f // px/ms
}

@Composable
fun PianoKeyboard(
    state: KeyboardState,
    onNoteOn: (Int) -> Unit,
    onNoteOff: (Int) -> Unit,
    onNoteSlide: (Int, Int) -> Unit,
    onOctaveShift: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pointerToNote = remember { mutableMapOf<Int, Int>() }
    val primaryColor = MaterialTheme.colorScheme.primary

    // Drag scroll state
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }
    var dragPointerId by remember { mutableStateOf(-1) }
    var canvasWidth by remember { mutableFloatStateOf(1f) }
    val scrollAnim = remember { Animatable(0f) }
    var flingRequest by remember { mutableStateOf<FlingRequest?>(null) }
    val velocitySamples = remember { mutableListOf<Pair<Long, Float>>() }

    fun octW(): Float = canvasWidth / state.octaveCount

    // Spring-animated fling + snap
    LaunchedEffect(flingRequest) {
        val req = flingRequest ?: return@LaunchedEffect
        flingRequest = null
        isAnimating = true
        val ow = octW()
        if (ow <= 0f) { isAnimating = false; return@LaunchedEffect }
        val projected = req.offset + req.velocityPxPerMs * 250f
        val snapTarget = (projected / ow).roundToInt() * ow
        Log.d(TAG, "FLING: offset=${req.offset} vel=${req.velocityPxPerMs} projected=$projected snapTarget=$snapTarget")
        scrollAnim.snapTo(req.offset)
        scrollAnim.animateTo(snapTarget,
            spring(dampingRatio = 0.9f, stiffness = 150f),
            initialVelocity = req.velocityPxPerMs
        )
        // Reset visual FIRST, then shift octave
        scrollAnim.snapTo(0f)
        dragOffset = 0f
        isAnimating = false
        val absorbed = -(snapTarget / ow).roundToInt()
        if (absorbed != 0) onOctaveShift(absorbed)
        Log.d(TAG, "FLING done: absorbed=$absorbed oct=${state.octaveStart}")
    }

    val displayOffset = when {
        isDragging -> dragOffset
        isAnimating -> scrollAnim.value
        else -> 0f
    }
    fun extraOctaves(): Int {
        val off = abs(displayOffset)
        val ow = octW()
        return if (ow > 0f && off > 1f) maxOf(1, (off / ow).toInt() + 1)
        else if (isDragging || isAnimating) 1 else 0
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier.fillMaxSize()
                .pointerInput(state.octaveStart, state.octaveCount, state.blackKeyLayout) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            for (pointer in event.changes) {
                                val pid = pointer.id.value.toInt()
                                when {
                                    pointer.pressed && !pointer.previousPressed -> {
                                        if (!isDragging && !isAnimating) dragOffset = 0f
                                        hitTest(
                                            pointer.position.x - displayOffset, pointer.position.y,
                                            size.width.toFloat(), size.height.toFloat(),
                                            state, extraOctaves()
                                        )?.let { pointerToNote[pid] = it; onNoteOn(it) }
                                    }
                                    pointer.pressed && pointer.previousPressed -> {
                                        val prev = pointerToNote[pid] ?: continue
                                        when (state.slideMode) {
                                            SlideMode.FOLLOW_KEYS -> {
                                                val cur = hitTest(
                                                    pointer.position.x - displayOffset, pointer.position.y,
                                                    size.width.toFloat(), size.height.toFloat(),
                                                    state, extraOctaves()
                                                )
                                                if (cur != null && cur != prev) {
                                                    onNoteSlide(prev, cur); pointerToNote[pid] = cur
                                                }
                                            }
                                            SlideMode.SHIFT_OCTAVE -> {
                                                if (dragPointerId < 0) dragPointerId = pid
                                                if (pid == dragPointerId) {
                                                    val dx = pointer.position.x - pointer.previousPosition.x
                                                    dragOffset += dx
                                                    velocitySamples.add(System.nanoTime() to dx)
                                                    while (velocitySamples.size > 20) velocitySamples.removeAt(0)
                                                    isDragging = true
                                                }
                                            }
                                        }
                                    }
                                    !pointer.pressed && pointer.previousPressed -> {
                                        pointerToNote.remove(pid)?.let { onNoteOff(it) }
                                    }
                                }
                            }
                            if (isDragging && event.changes.all { !it.pressed }) {
                                isDragging = false
                                dragPointerId = -1
                                val ow = octW()
                                val vel = computeVelocity(velocitySamples)
                                velocitySamples.clear()
                                Log.d(TAG, "SNAP: dragOffset=$dragOffset ow=$ow vel=$vel")
                                if (ow > 0f && abs(vel) > 0.1f && abs(dragOffset) > ow * 0.15f) {
                                    flingRequest = FlingRequest(dragOffset, vel)
                                } else {
                                    val delta = -(dragOffset / ow).roundToInt()
                                    if (delta != 0) onOctaveShift(delta)
                                    dragOffset = 0f
                                }
                            }
                        }
                    }
                }
        ) {
            canvasWidth = size.width
            val ext = extraOctaves()
            withTransform({ translate(left = displayOffset) }) {
                if (state.blackKeyLayout == BlackKeyLayout.EQUAL_WIDTH) drawEqualWidthKeys(state, ext, primaryColor)
                else drawPianoKeys(state, ext, primaryColor)
            }
        }

        // Debug overlay
        if (state.slideMode == SlideMode.SHIFT_OCTAVE) {
            Text(
                text = "off=%.0f ow=%.0f drg=$isDragging anim=$isAnimating ext=${extraOctaves()} oct=${state.octaveStart}".format(
                    displayOffset, octW()
                ),
                color = Color.Yellow, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
            )
        }
    }
}

// --- Drawing (unchanged logic with extraOctaves) ---

private fun DrawScope.drawPianoKeys(state: KeyboardState, extraOctaves: Int, primaryColor: Color) {
    val baseCount = state.octaveCount
    val totalWhiteKeys = baseCount * 7
    val whiteKeyWidth = size.width / totalWhiteKeys
    val blackKeyWidth = whiteKeyWidth * 0.6f
    val blackKeyHeight = size.height * 0.62f
    val wc = CornerRadius(10f, 10f); val bc = CornerRadius(8f, 8f); val gap = 3f
    for (oct in -extraOctaves until baseCount + extraOctaves) {
        for ((wi, st) in WHITE_KEY_OFFSETS.withIndex()) {
            val note = state.octaveStart + oct * 12 + st
            val x = (oct * 7 + wi) * whiteKeyWidth
            val act = state.activeNotes.contains(note)
            drawRoundRect(color = if (act) primaryColor else OscilloWhiteKey,
                topLeft = Offset(x + gap / 2, gap), size = Size(whiteKeyWidth - gap, size.height - gap * 2), cornerRadius = wc)
            if (state.showNoteLabels) drawLabel(PitchUtils.midiNoteToName(note), x + whiteKeyWidth / 2, size.height * 0.9f, whiteKeyWidth * 0.28f,
                if (act) 0xFFFFFFFF.toInt() else 0xFF666666.toInt())
        }
    }
    for (oct in -extraOctaves until baseCount + extraOctaves) {
        for ((wi, st) in BLACK_KEY_DATA) {
            val note = state.octaveStart + oct * 12 + st
            val x = (oct * 7 + wi) * whiteKeyWidth + whiteKeyWidth * 0.7f
            val act = state.activeNotes.contains(note)
            drawRoundRect(color = if (act) primaryColor else OscilloBlackKey,
                topLeft = Offset(x, 0f), size = Size(blackKeyWidth, blackKeyHeight), cornerRadius = bc)
            if (state.showNoteLabels) drawLabel(PitchUtils.midiNoteToName(note), x + blackKeyWidth / 2, blackKeyHeight * 0.88f, blackKeyWidth * 0.32f,
                if (act) 0xFFFFFFFF.toInt() else 0xFFAAAAAA.toInt())
        }
    }
}

private fun DrawScope.drawEqualWidthKeys(state: KeyboardState, extraOctaves: Int, primaryColor: Color) {
    val baseCount = state.octaveCount
    val totalKeys = baseCount * 12
    val keyWidth = size.width / totalKeys
    val cr = CornerRadius(8f, 8f); val gap = 3f
    for (oct in -extraOctaves until baseCount + extraOctaves) {
        for (st in 0 until 12) {
            val note = state.octaveStart + oct * 12 + st
            val x = (oct * 12 + st) * keyWidth
            val act = state.activeNotes.contains(note); val blk = st in setOf(1,3,6,8,10)
            drawRoundRect(color = when { act -> primaryColor; blk -> OscilloBlackKey; else -> OscilloWhiteKey },
                topLeft = Offset(x + gap / 2, gap), size = Size(keyWidth - gap, size.height - gap * 2), cornerRadius = cr)
            if (state.showNoteLabels) drawLabel(PitchUtils.midiNoteToName(note), x + keyWidth / 2, size.height * 0.9f, keyWidth * 0.38f,
                when { act -> 0xFFFFFFFF.toInt(); blk -> 0xFFAAAAAA.toInt(); else -> 0xFF666666.toInt() })
        }
    }
}

private fun DrawScope.drawLabel(t: String, x: Float, y: Float, s: Float, c: Int) {
    drawContext.canvas.nativeCanvas.drawText(t, x, y, Paint().apply {
        color = c; textSize = s; textAlign = Paint.Align.CENTER; isAntiAlias = true
    })
}

private fun hitTest(x: Float, y: Float, tw: Float, th: Float, state: KeyboardState, extra: Int): Int? {
    val bc = state.octaveCount
    val fo = -extra; val lo = bc + extra - 1
    if (state.blackKeyLayout == BlackKeyLayout.EQUAL_WIDTH) {
        val kw = tw / (bc * 12); val idx = (x / kw).toInt()
        val total = (lo - fo + 1) * 12
        if (idx < 0 || idx >= total) return null
        return state.octaveStart + fo * 12 + idx
    } else {
        val wk = tw / (bc * 7); val bk = wk * 0.6f
        if (y < th * 0.62f)
            for (o in fo..lo) for ((wi, st) in BLACK_KEY_DATA) {
                val kl = (o * 7 + wi) * wk + wk * 0.7f
                if (x in kl..(kl + bk)) return state.octaveStart + o * 12 + st
            }
        val total = (lo - fo + 1) * 7
        val wi = (x / wk).toInt()
        if (wi < 0 || wi >= total) return null
        return state.octaveStart + (wi / 7 + fo) * 12 + WHITE_KEY_OFFSETS[wi % 7]
    }
}
