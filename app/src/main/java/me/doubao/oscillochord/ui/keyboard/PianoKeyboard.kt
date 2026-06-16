package me.doubao.oscillochord.ui.keyboard

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import me.doubao.oscillochord.domain.chord.PitchUtils
import me.doubao.oscillochord.ui.theme.OscilloBlackKey
import me.doubao.oscillochord.ui.theme.OscilloWhiteKey
import kotlin.math.abs
import kotlin.math.roundToInt

private val WHITE_KEY_OFFSETS = listOf(0, 2, 4, 5, 7, 9, 11)
private val BLACK_KEY_DATA = listOf(0 to 1, 1 to 3, 3 to 6, 4 to 8, 5 to 10)

private data class FlingRequest(val offset: Float, val velocityPxPerSec: Float)

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

    var rawOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }
    var canvasWidth by remember { mutableFloatStateOf(1f) }
    val scrollAnim = remember { Animatable(0f) }
    val velocitySamples = remember { mutableListOf<Pair<Long, Float>>() }
    var flingRequest by remember { mutableStateOf<FlingRequest?>(null) }
    var resetAnim by remember { mutableStateOf(false) }

    fun octW(): Float =
        if (state.blackKeyLayout == BlackKeyLayout.EQUAL_WIDTH) canvasWidth / state.octaveCount
        else canvasWidth / state.octaveCount

    // Process fling → snap animation (outside restricted pointer scope)
    LaunchedEffect(flingRequest) {
        val req = flingRequest ?: return@LaunchedEffect
        flingRequest = null
        isAnimating = true
        val ow = octW()
        if (ow <= 0f) { isAnimating = false; return@LaunchedEffect }

        val flingDist = req.velocityPxPerSec * 0.15f
        val projected = req.offset + flingDist
        val tgtOct = (projected / ow).roundToInt()
        val snapTarget = tgtOct * ow

        scrollAnim.snapTo(req.offset)
        scrollAnim.animateTo(snapTarget,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
            initialVelocity = req.velocityPxPerSec / 1000f
        )
        val absorbed = (snapTarget / ow).roundToInt()
        if (absorbed != 0) onOctaveShift(-absorbed)
        scrollAnim.snapTo(0f)
        rawOffset = 0f
        isAnimating = false
    }

    // Handle simple reset (tiny drag movement)
    LaunchedEffect(resetAnim) {
        if (resetAnim) {
            scrollAnim.snapTo(0f)
            resetAnim = false
        }
    }

    val displayOffset = if (isDragging) rawOffset else scrollAnim.value
    val needsExtended = isDragging || isAnimating
    val drawState = if (needsExtended)
        state.copy(octaveStart = state.octaveStart - 12, octaveCount = state.octaveCount + 2)
    else state

    Canvas(
        modifier = modifier.fillMaxWidth()
            .pointerInput(state.octaveStart, state.octaveCount, state.blackKeyLayout) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val now = System.nanoTime()
                        for (pointer in event.changes) {
                            val pid = pointer.id.value.toInt()
                            when {
                                pointer.pressed && !pointer.previousPressed -> {
                                    velocitySamples.clear()
                                    hitTest(pointer.position.x - displayOffset, pointer.position.y,
                                        size.width.toFloat(), size.height.toFloat(), drawState
                                    )?.let { pointerToNote[pid] = it; onNoteOn(it) }
                                }
                                pointer.pressed && pointer.previousPressed -> {
                                    val prev = pointerToNote[pid] ?: continue
                                    when (state.slideMode) {
                                        SlideMode.FOLLOW_KEYS -> {
                                            val cur = hitTest(pointer.position.x - displayOffset,
                                                pointer.position.y,
                                                size.width.toFloat(), size.height.toFloat(), drawState
                                            )
                                            if (cur != null && cur != prev) {
                                                onNoteSlide(prev, cur); pointerToNote[pid] = cur
                                            }
                                        }
                                        SlideMode.SHIFT_OCTAVE -> {
                                            val dx = pointer.position.x - pointer.previousPosition.x
                                            rawOffset += dx
                                            velocitySamples.add(now to dx)
                                            while (velocitySamples.size > 20) velocitySamples.removeAt(0)
                                            isDragging = true
                                        }
                                    }
                                }
                                !pointer.pressed && pointer.previousPressed -> {
                                    pointerToNote.remove(pid)?.let { onNoteOff(it) }
                                }
                            }
                        }
                        // All pointers up → end drag, request fling
                        if (isDragging && event.changes.all { !it.pressed }) {
                            isDragging = false
                            val vel = computeVelocity(velocitySamples)
                            velocitySamples.clear()
                            val ow = octW()
                            if (ow > 0f && abs(rawOffset) > 2f) {
                                flingRequest = FlingRequest(rawOffset, vel)
                            } else {
                                rawOffset = 0f
                                resetAnim = true
                            }
                        }
                    }
                }
            }
    ) {
        canvasWidth = size.width
        withTransform({ translate(left = displayOffset) }) {
            if (state.blackKeyLayout == BlackKeyLayout.EQUAL_WIDTH) drawEqualWidthKeys(drawState, primaryColor)
            else drawPianoKeys(drawState, primaryColor)
        }
    }
}

private fun computeVelocity(samples: List<Pair<Long, Float>>): Float {
    if (samples.size < 2) return 0f
    val newest = samples.last()
    val oldest = samples.first()
    val dtNs = newest.first - oldest.first
    if (dtNs <= 0L) return 0f
    val totalDx = samples.sumOf { it.second.toDouble() }.toFloat()
    return totalDx / (dtNs / 1_000_000_000f)
}

// --- Drawing ---

private fun DrawScope.drawPianoKeys(state: KeyboardState, primaryColor: androidx.compose.ui.graphics.Color) {
    val totalWhiteKeys = state.octaveCount * 7
    val whiteKeyWidth = size.width / totalWhiteKeys
    val blackKeyWidth = whiteKeyWidth * 0.6f
    val blackKeyHeight = size.height * 0.62f
    val whiteCorner = CornerRadius(10f, 10f)
    val blackCorner = CornerRadius(8f, 8f)
    val gap = 3f
    for (octave in 0 until state.octaveCount) {
        for ((wi, semitone) in WHITE_KEY_OFFSETS.withIndex()) {
            val midiNote = state.octaveStart + octave * 12 + semitone
            val x = (octave * 7 + wi) * whiteKeyWidth
            val isActive = state.activeNotes.contains(midiNote)
            drawRoundRect(color = if (isActive) primaryColor else OscilloWhiteKey,
                topLeft = Offset(x + gap / 2, gap), size = Size(whiteKeyWidth - gap, size.height - gap * 2),
                cornerRadius = whiteCorner)
            if (state.showNoteLabels) drawNoteLabel(PitchUtils.midiNoteToName(midiNote),
                x + whiteKeyWidth / 2, size.height * 0.9f, whiteKeyWidth * 0.28f,
                if (isActive) 0xFFFFFFFF.toInt() else 0xFF666666.toInt())
        }
    }
    for (octave in 0 until state.octaveCount) {
        for ((whiteIndex, semitone) in BLACK_KEY_DATA) {
            val midiNote = state.octaveStart + octave * 12 + semitone
            val x = (octave * 7 + whiteIndex) * whiteKeyWidth + whiteKeyWidth * 0.7f
            val isActive = state.activeNotes.contains(midiNote)
            drawRoundRect(color = if (isActive) primaryColor else OscilloBlackKey,
                topLeft = Offset(x, 0f), size = Size(blackKeyWidth, blackKeyHeight), cornerRadius = blackCorner)
            if (state.showNoteLabels) drawNoteLabel(PitchUtils.midiNoteToName(midiNote),
                x + blackKeyWidth / 2, blackKeyHeight * 0.88f, blackKeyWidth * 0.32f,
                if (isActive) 0xFFFFFFFF.toInt() else 0xFFAAAAAA.toInt())
        }
    }
}

private fun DrawScope.drawEqualWidthKeys(state: KeyboardState, primaryColor: androidx.compose.ui.graphics.Color) {
    val totalKeys = state.octaveCount * 12
    val keyWidth = size.width / totalKeys
    val cornerRadius = CornerRadius(8f, 8f)
    val gap = 3f
    for (octave in 0 until state.octaveCount) {
        for (semitone in 0 until 12) {
            val midiNote = state.octaveStart + octave * 12 + semitone
            val x = (octave * 12 + semitone) * keyWidth
            val isActive = state.activeNotes.contains(midiNote)
            drawRoundRect(color = when { isActive -> primaryColor; semitone in setOf(1,3,6,8,10) -> OscilloBlackKey; else -> OscilloWhiteKey },
                topLeft = Offset(x + gap / 2, gap), size = Size(keyWidth - gap, size.height - gap * 2),
                cornerRadius = cornerRadius)
            if (state.showNoteLabels) drawNoteLabel(PitchUtils.midiNoteToName(midiNote),
                x + keyWidth / 2, size.height * 0.9f, keyWidth * 0.38f,
                when { isActive -> 0xFFFFFFFF.toInt(); semitone in setOf(1,3,6,8,10) -> 0xFFAAAAAA.toInt(); else -> 0xFF666666.toInt() })
        }
    }
}

private fun DrawScope.drawNoteLabel(text: String, x: Float, y: Float, tSize: Float, color: Int) {
    drawContext.canvas.nativeCanvas.drawText(text, x, y, Paint().apply {
        this.color = color; this.textSize = tSize; textAlign = Paint.Align.CENTER; isAntiAlias = true
    })
}

private fun hitTest(x: Float, y: Float, totalWidth: Float, totalHeight: Float, state: KeyboardState): Int? {
    if (x < 0f || x > totalWidth) return null
    return if (state.blackKeyLayout == BlackKeyLayout.EQUAL_WIDTH) {
        val keyWidth = totalWidth / (state.octaveCount * 12)
        state.octaveStart + (x / keyWidth).toInt().coerceIn(0, state.octaveCount * 12 - 1)
    } else {
        val whiteKeyWidth = totalWidth / (state.octaveCount * 7)
        val blackKeyWidth = whiteKeyWidth * 0.6f
        if (y < totalHeight * 0.62f) {
            for (octave in 0 until state.octaveCount)
                for ((wi, st) in BLACK_KEY_DATA) {
                    val keyLeft = (octave * 7 + wi) * whiteKeyWidth + whiteKeyWidth * 0.7f
                    if (x in keyLeft..(keyLeft + blackKeyWidth)) return state.octaveStart + octave * 12 + st
                }
        }
        val wi = (x / whiteKeyWidth).toInt().coerceIn(0, state.octaveCount * 7 - 1)
        state.octaveStart + (wi / 7) * 12 + WHITE_KEY_OFFSETS[wi % 7]
    }
}
