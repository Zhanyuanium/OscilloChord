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

private data class FlingRequest(val offset: Float, val velPxPerMs: Float)

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

    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }
    var dragPointerId by remember { mutableStateOf(-1) }
    var canvasWidth by remember { mutableFloatStateOf(1f) }
    val scrollAnim = remember { Animatable(0f) }
    var flingRequest by remember { mutableStateOf<FlingRequest?>(null) }
    val velocitySamples = remember { mutableListOf<Pair<Long, Float>>() }

    fun octW(): Float = canvasWidth / state.octaveCount

    // Spring-based fling + snap (one smooth motion)
    LaunchedEffect(flingRequest) {
        val req = flingRequest ?: return@LaunchedEffect
        flingRequest = null
        isAnimating = true
        val ow = octW()
        if (ow <= 0f) { isAnimating = false; return@LaunchedEffect }

        // Project landing point, snap to nearest octave
        val flingDist = req.velPxPerMs * 250f
        val projected = req.offset + flingDist
        val snapTarget = (projected / ow).roundToInt() * ow

        scrollAnim.snapTo(req.offset)
        scrollAnim.animateTo(snapTarget,
            spring(dampingRatio = 0.9f, stiffness = 150f),
            initialVelocity = req.velPxPerMs
        )
        // Absorb octave shift after animation settles
        scrollAnim.snapTo(0f)
        isAnimating = false
        dragOffset = 0f
        val absorbed = -(snapTarget / ow).roundToInt()
        if (absorbed != 0) onOctaveShift(absorbed)
    }

    val displayOffset = when {
        isDragging -> dragOffset
        isAnimating -> scrollAnim.value
        else -> 0f
    }

    fun extraOctaves(): Int {
        val off = abs(if (isDragging) dragOffset else if (isAnimating) scrollAnim.value else 0f)
        val ow = octW()
        return if (ow > 0f && off > 1f) maxOf(1, (off / ow).toInt() + 1) else if (isDragging || isAnimating) 1 else 0
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier.fillMaxSize()
                .pointerInput(state.octaveStart, state.octaveCount, state.blackKeyLayout) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val now = System.nanoTime()
                            for (pointer in event.changes) {
                                val pid = pointer.id.value.toInt()
                                when {
                                    pointer.pressed && !pointer.previousPressed -> {
                                        if (!isDragging && !isAnimating) dragOffset = 0f
                                        hitTest(pointer.position.x - displayOffset, pointer.position.y,
                                            size.width.toFloat(), size.height.toFloat(), state, extraOctaves()
                                        )?.let { pointerToNote[pid] = it; onNoteOn(it) }
                                    }
                                    pointer.pressed && pointer.previousPressed -> {
                                        val prev = pointerToNote[pid] ?: continue
                                        when (state.slideMode) {
                                            SlideMode.FOLLOW_KEYS -> {
                                                val cur = hitTest(pointer.position.x - displayOffset, pointer.position.y,
                                                    size.width.toFloat(), size.height.toFloat(), state, extraOctaves())
                                                if (cur != null && cur != prev) {
                                                    onNoteSlide(prev, cur); pointerToNote[pid] = cur
                                                }
                                            }
                                            SlideMode.SHIFT_OCTAVE -> {
                                                if (dragPointerId < 0) dragPointerId = pid
                                                if (pid == dragPointerId) {
                                                    val dx = pointer.position.x - pointer.previousPosition.x
                                                    dragOffset += dx
                                                    velocitySamples.add(now to dx)
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
                                isDragging = false; dragPointerId = -1
                                val ow = octW()
                                val vel = computeVelocity(velocitySamples)
                                velocitySamples.clear()
                                if (ow > 0f && abs(dragOffset) > ow * 0.15f && abs(vel) > 0.1f) {
                                    flingRequest = FlingRequest(dragOffset, vel)
                                } else {
                                    // Tiny movement: just snap directly
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
        // Debug
        if (state.slideMode == SlideMode.SHIFT_OCTAVE) {
            Text(text = "off=%.0f ow=%.0f drg=$isDragging anim=$isAnimating oct=${state.octaveStart}".format(displayOffset, octW()),
                color = Color.Yellow, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp))
        }
    }
}

private fun computeVelocity(samples: List<Pair<Long, Float>>): Float {
    if (samples.size < 2) return 0f
    val newest = samples.last(); val oldest = samples.first()
    val dtNs = newest.first - oldest.first
    if (dtNs <= 0L) return 0f
    val totalDx = samples.sumOf { it.second.toDouble() }.toFloat()
    return totalDx / (dtNs / 1_000_000_000f) / 1000f // px/ms
}

// --- Drawing (unchanged) ---

private fun DrawScope.drawPianoKeys(state: KeyboardState, extraOctaves: Int, primaryColor: Color) {
    val bc = state.octaveCount; val twk = bc * 7; val wk = size.width / twk; val bk = wk * 0.6f
    val bkh = size.height * 0.62f; val wc = CornerRadius(10f,10f); val bc2 = CornerRadius(8f,8f); val gap = 3f
    for (o in -extraOctaves until bc + extraOctaves) {
        for ((wi,st) in WHITE_KEY_OFFSETS.withIndex()) {
            val n = state.octaveStart + o*12 + st; val x = (o*7 + wi)*wk
            val a = state.activeNotes.contains(n)
            drawRoundRect(color = if (a) primaryColor else OscilloWhiteKey,
                topLeft = Offset(x+gap/2,gap), size = Size(wk-gap, size.height-gap*2), cornerRadius = wc)
            if (state.showNoteLabels) drawLabel(PitchUtils.midiNoteToName(n), x+wk/2, size.height*0.9f, wk*0.28f, if(a) 0xFFFFFFFF.toInt() else 0xFF666666.toInt())
        }
    }
    for (o in -extraOctaves until bc + extraOctaves) {
        for ((wi,st) in BLACK_KEY_DATA) {
            val n = state.octaveStart + o*12 + st; val x = (o*7 + wi)*wk + wk*0.7f
            val a = state.activeNotes.contains(n)
            drawRoundRect(color = if (a) primaryColor else OscilloBlackKey,
                topLeft = Offset(x,0f), size = Size(bk,bkh), cornerRadius = bc2)
            if (state.showNoteLabels) drawLabel(PitchUtils.midiNoteToName(n), x+bk/2, bkh*0.88f, bk*0.32f, if(a) 0xFFFFFFFF.toInt() else 0xFFAAAAAA.toInt())
        }
    }
}

private fun DrawScope.drawEqualWidthKeys(state: KeyboardState, extraOctaves: Int, primaryColor: Color) {
    val bc = state.octaveCount; val tk = bc*12; val kw = size.width/tk; val cr = CornerRadius(8f,8f); val gap = 3f
    for (o in -extraOctaves until bc + extraOctaves) {
        for (st in 0 until 12) {
            val n = state.octaveStart + o*12 + st; val x = (o*12+st)*kw
            val a = state.activeNotes.contains(n); val bl = st in setOf(1,3,6,8,10)
            drawRoundRect(color = when{a->primaryColor; bl->OscilloBlackKey; else->OscilloWhiteKey},
                topLeft = Offset(x+gap/2,gap), size = Size(kw-gap,size.height-gap*2), cornerRadius = cr)
            if (state.showNoteLabels) drawLabel(PitchUtils.midiNoteToName(n), x+kw/2, size.height*0.9f, kw*0.38f,
                when{a->0xFFFFFFFF.toInt(); bl->0xFFAAAAAA.toInt(); else->0xFF666666.toInt()})
        }
    }
}

private fun DrawScope.drawLabel(t: String, x: Float, y: Float, s: Float, c: Int) {
    drawContext.canvas.nativeCanvas.drawText(t, x, y, Paint().apply {
        color = c; textSize = s; textAlign = Paint.Align.CENTER; isAntiAlias = true
    })
}

private fun hitTest(x: Float, y: Float, tw: Float, th: Float, state: KeyboardState, extra: Int): Int? {
    val bc = state.octaveCount; val fo = -extra; val lo = bc+extra-1
    if (state.blackKeyLayout == BlackKeyLayout.EQUAL_WIDTH) {
        val kw = tw/(bc*12); val idx = (x/kw).toInt()
        val total = (lo-fo+1)*12; if (idx<0||idx>=total) return null
        return state.octaveStart + fo*12 + idx
    } else {
        val wk = tw/(bc*7); val bk = wk*0.6f
        if (y < th*0.62f) for (o in fo..lo) for ((wi,st) in BLACK_KEY_DATA) {
            val kl = (o*7+wi)*wk + wk*0.7f; if (x in kl..(kl+bk)) return state.octaveStart + o*12 + st
        }
        val total = (lo-fo+1)*7; val wi = (x/wk).toInt()
        if (wi<0||wi>=total) return null
        return state.octaveStart + (wi/7+fo)*12 + WHITE_KEY_OFFSETS[wi%7]
    }
}
