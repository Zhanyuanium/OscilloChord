package me.doubao.oscillochord.ui.keyboard

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import me.doubao.oscillochord.domain.chord.PitchUtils
import me.doubao.oscillochord.domain.settings.NoteNamingSetting
import me.doubao.oscillochord.ui.theme.OscilloBlackKey
import me.doubao.oscillochord.ui.theme.OscilloWhiteKey
import kotlin.math.abs
import kotlin.math.roundToInt

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

    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }
    var dragPointerId by remember { mutableStateOf(-1) }
    var canvasWidth by remember { mutableFloatStateOf(1f) }
    val scrollAnim = remember { Animatable(0f) }
    var flingCounter by remember { mutableStateOf(0) }
    var pendingFling by remember { mutableStateOf<FlingRequest?>(null) }
    val velocitySamples = remember { mutableListOf<Pair<Long, Float>>() }

    fun octW(): Float = canvasWidth / state.octaveCount

    LaunchedEffect(flingCounter) {
        if (flingCounter == 0) return@LaunchedEffect
        val req = pendingFling ?: return@LaunchedEffect
        val ow = octW()
        if (ow <= 0f) {
            isDragging = false
            pendingFling = null
            return@LaunchedEffect
        }
        val projected = req.offset + req.velocityPxPerMs * 250f
        val snapTarget = (projected / ow).roundToInt() * ow
        scrollAnim.snapTo(req.offset)
        // Switch from drag bridge to animation mode only AFTER snapTo
        // so displayOffset transitions seamlessly.
        isDragging = false
        isAnimating = true
        pendingFling = null
        scrollAnim.animateTo(snapTarget,
            tween(durationMillis = 400, easing = EaseOutCubic),
            initialVelocity = req.velocityPxPerMs
        )
        // Reset visual FIRST, then shift octave
        scrollAnim.snapTo(0f)
        dragOffset = 0f
        isAnimating = false
        val absorbed = -(snapTarget / ow).roundToInt()
        if (absorbed != 0) onOctaveShift(absorbed)
    }

    val displayOffset = when {
        isDragging -> dragOffset
        isAnimating -> scrollAnim.value
        else -> 0f
    }

    // Pre-created Paint objects for label rendering — colors set once, textSize set per call
    val whiteKeyLabelPaint = remember {
        Paint().apply { color = 0xFF666666.toInt(); textAlign = Paint.Align.CENTER; isAntiAlias = true }
    }
    val activeKeyLabelPaint = remember {
        Paint().apply { color = 0xFFFFFFFF.toInt(); textAlign = Paint.Align.CENTER; isAntiAlias = true }
    }
    val blackKeyLabelPaint = remember {
        Paint().apply { color = 0xFFAAAAAA.toInt(); textAlign = Paint.Align.CENTER; isAntiAlias = true }
    }
    fun extraOctaves(): Int {
        val off = abs(displayOffset)
        val ow = octW()
        return if (ow > 0f && off > 1f) maxOf(1, (off / ow).toInt() + 1)
        else if (isDragging || isAnimating) 1 else 0
    }

    Canvas(
        modifier = modifier.fillMaxWidth()
            .pointerInput(state.octaveStart, state.octaveCount, state.blackKeyLayout, state.slideMode) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            for (pointer in event.changes) {
                                val pid = pointer.id.value.toInt()
                                // Read live state, not the snapshot val captured at compose time
                                val liveOffset = when {
                                    isDragging -> dragOffset
                                    isAnimating -> scrollAnim.value
                                    else -> 0f
                                }
                                fun liveExtra(): Int {
                                    val off = abs(liveOffset)
                                    val ow = octW()
                                    return if (ow > 0f && off > 1f) maxOf(1, (off / ow).toInt() + 1)
                                    else if (isDragging || isAnimating) 1 else 0
                                }
                                when {
                                    pointer.pressed && !pointer.previousPressed -> {
                                        if (!isDragging && !isAnimating) dragOffset = 0f
                                        hitTest(
                                            pointer.position.x - liveOffset, pointer.position.y,
                                            size.width.toFloat(), size.height.toFloat(),
                                            state, liveExtra()
                                        )?.let { pointerToNote[pid] = it; onNoteOn(it) }
                                    }
                                    pointer.pressed && pointer.previousPressed -> {
                                        val prev = pointerToNote[pid] ?: continue
                                        when (state.slideMode) {
                                            SlideMode.FOLLOW_KEYS -> {
                                                val cur = hitTest(
                                                    pointer.position.x - liveOffset, pointer.position.y,
                                                    size.width.toFloat(), size.height.toFloat(),
                                                    state, liveExtra()
                                                )
                                                if (cur != null && cur != prev) {
                                                    onNoteSlide(prev, cur); pointerToNote[pid] = cur
                                                }
                                            }
                                            SlideMode.SHIFT_OCTAVE -> {
                                                if (dragPointerId < 0) dragPointerId = pid
                                                if (pid == dragPointerId) {
                                                    dragOffset += pointer.position.x - pointer.previousPosition.x
                                                    velocitySamples.add(System.nanoTime() to pointer.position.x - pointer.previousPosition.x)
                                                    while (velocitySamples.size > 20) velocitySamples.removeAt(0)
                                                    isDragging = true
                                                }
                                            }
                                        }
                                    }
                                    !pointer.pressed && pointer.previousPressed -> {
                                        if (pid == dragPointerId) dragPointerId = -1
                                        pointerToNote.remove(pid)?.let { onNoteOff(it) }
                                    }
                                }
                            }
                            if (isDragging && event.changes.all { !it.pressed }) {
                                // Keep isDragging=true as a bridge — avoid displayOffset
                                // falling to 0 before the LaunchedEffect takes over.
                                dragPointerId = -1
                                val ow = octW()
                                if (ow > 0f) {
                                    val vel = computeVelocity(velocitySamples)
                                    velocitySamples.clear()
                                    pendingFling = FlingRequest(dragOffset, vel)
                                    flingCounter++
                                } else {
                                    isDragging = false
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
                if (state.blackKeyLayout == BlackKeyLayout.EQUAL_WIDTH) drawEqualWidthKeys(state, ext, primaryColor, whiteKeyLabelPaint, activeKeyLabelPaint, blackKeyLabelPaint)
                else drawPianoKeys(state, ext, primaryColor, whiteKeyLabelPaint, activeKeyLabelPaint, blackKeyLabelPaint)
            }
        }
}

private fun DrawScope.drawPianoKeys(state: KeyboardState, extraOctaves: Int, primaryColor: Color, whiteKeyLabelPaint: Paint, activeKeyLabelPaint: Paint, blackKeyLabelPaint: Paint) {
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
            if (state.showNoteLabels) drawLabel(PitchUtils.midiNoteToName(note, state.noteNaming == NoteNamingSetting.FLAT), x + whiteKeyWidth / 2, size.height * 0.9f, whiteKeyWidth * 0.28f,
                if (act) activeKeyLabelPaint else whiteKeyLabelPaint)
        }
    }
    for (oct in -extraOctaves until baseCount + extraOctaves) {
        for ((wi, st) in BLACK_KEY_DATA) {
            val note = state.octaveStart + oct * 12 + st
            val x = (oct * 7 + wi) * whiteKeyWidth + whiteKeyWidth * 0.7f
            val act = state.activeNotes.contains(note)
            drawRoundRect(color = if (act) primaryColor else OscilloBlackKey,
                topLeft = Offset(x, 0f), size = Size(blackKeyWidth, blackKeyHeight), cornerRadius = bc)
            if (state.showNoteLabels) drawLabel(PitchUtils.midiNoteToName(note, state.noteNaming == NoteNamingSetting.FLAT), x + blackKeyWidth / 2, blackKeyHeight * 0.88f, blackKeyWidth * 0.32f,
                if (act) activeKeyLabelPaint else blackKeyLabelPaint)
        }
    }
}

private fun DrawScope.drawEqualWidthKeys(state: KeyboardState, extraOctaves: Int, primaryColor: Color, whiteKeyLabelPaint: Paint, activeKeyLabelPaint: Paint, blackKeyLabelPaint: Paint) {
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
            if (state.showNoteLabels) drawLabel(PitchUtils.midiNoteToName(note, state.noteNaming == NoteNamingSetting.FLAT), x + keyWidth / 2, size.height * 0.9f, keyWidth * 0.38f,
                when { act -> activeKeyLabelPaint; blk -> blackKeyLabelPaint; else -> whiteKeyLabelPaint })
        }
    }
}

private fun DrawScope.drawLabel(t: String, x: Float, y: Float, s: Float, paint: Paint) {
    paint.textSize = s
    drawContext.canvas.nativeCanvas.drawText(t, x, y, paint)
}

private fun hitTest(x: Float, y: Float, tw: Float, th: Float, state: KeyboardState, extra: Int): Int? {
    val bc = state.octaveCount
    if (state.blackKeyLayout == BlackKeyLayout.EQUAL_WIDTH) {
        val kw = tw / (bc * 12)
        val idx = (x / kw).toInt()
        // idx directly encodes octave and semitone: note = octaveStart + idx (same as drawing)
        if (idx < -extra * 12 || idx >= (bc + extra) * 12) return null
        return state.octaveStart + idx
    } else {
        val wk = tw / (bc * 7)
        if (y < th * 0.62f) {
            for (oct in -extra until bc + extra)
                for ((wi, st) in BLACK_KEY_DATA) {
                    val kl = (oct * 7 + wi) * wk + wk * 0.7f
                    if (x in kl..(kl + wk * 0.6f)) return state.octaveStart + oct * 12 + st
                }
        }
        for (oct in -extra until bc + extra)
            for ((wi, st) in WHITE_KEY_OFFSETS.withIndex()) {
                val kl = (oct * 7 + wi) * wk
                if (x in kl..(kl + wk)) return state.octaveStart + oct * 12 + st
            }
        return null
    }
}
