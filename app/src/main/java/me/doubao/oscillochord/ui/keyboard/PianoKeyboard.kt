package me.doubao.oscillochord.ui.keyboard

import android.graphics.Paint
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import me.doubao.oscillochord.domain.chord.PitchUtils
import me.doubao.oscillochord.ui.theme.OscilloBlackKey
import me.doubao.oscillochord.ui.theme.OscilloWhiteKey
import kotlinx.coroutines.CancellationException

private const val TAG = "PianoKeyboard"

private val WHITE_KEY_OFFSETS = listOf(0, 2, 4, 5, 7, 9, 11)
private val BLACK_KEY_DATA = listOf(
    0 to 1, 1 to 3, 3 to 6, 4 to 8, 5 to 10
)

@Composable
fun PianoKeyboard(
    state: KeyboardState,
    onNoteOn: (Int) -> Unit,
    onNoteOff: (Int) -> Unit,
    onNoteSlide: (Int, Int) -> Unit,
    onOctaveShift: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pointerToNote = remember { mutableStateMapOf<Int, Int>() }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(state.octaveStart, state.octaveCount, state.blackKeyLayout) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        try {
                            for (pointer in event.changes) {
                                val pid = pointer.id.value.toInt()
                                when {
                                    pointer.pressed && !pointer.previousPressed -> {
                                        Log.d(TAG, "DOWN pid=$pid x=${pointer.position.x} y=${pointer.position.y}")
                                        hitTest(
                                            pointer.position.x, pointer.position.y,
                                            size.width.toFloat(), size.height.toFloat(), state
                                        )?.let { note ->
                                            Log.d(TAG, "DOWN→noteOn midi=$note")
                                            pointerToNote[pid] = note
                                            onNoteOn(note)
                                        }
                                    }
                                    pointer.pressed && pointer.previousPressed -> {
                                        val prevNote = pointerToNote[pid]
                                        if (prevNote == null) { Log.w(TAG, "MOVE pid=$pid no prevNote"); continue }
                                        val currentNote = hitTest(
                                            pointer.position.x, pointer.position.y,
                                            size.width.toFloat(), size.height.toFloat(), state
                                        )
                                        when (state.slideMode) {
                                            SlideMode.FOLLOW_KEYS -> {
                                                if (currentNote != null && currentNote != prevNote) {
                                                    Log.d(TAG, "SLIDE pid=$pid $prevNote→$currentNote")
                                                    onNoteSlide(prevNote, currentNote)
                                                    pointerToNote[pid] = currentNote
                                                }
                                            }
                                            SlideMode.SHIFT_OCTAVE -> {
                                                dragAccumulator += pointer.position.x - pointer.previousPosition.x
                                                val thresh = size.width / (state.octaveCount * 7)
                                                if (dragAccumulator > thresh) {
                                                    onOctaveShift(1); dragAccumulator = 0f
                                                } else if (dragAccumulator < -thresh) {
                                                    onOctaveShift(-1); dragAccumulator = 0f
                                                }
                                            }
                                        }
                                    }
                                    !pointer.pressed && pointer.previousPressed -> {
                                        Log.d(TAG, "UP pid=$pid note=${pointerToNote[pid]}")
                                        pointerToNote.remove(pid)?.let { onNoteOff(it) }
                                    }
                                }
                            }
                        } catch (e: CancellationException) {
                            Log.d(TAG, "pointerInput cancelled")
                            throw e
                        } catch (e: Exception) {
                            Log.e(TAG, "pointerInput error", e)
                        }
                    }
                }
            }
    ) {
        if (state.blackKeyLayout == BlackKeyLayout.EQUAL_WIDTH) {
            drawEqualWidthKeys(state, primaryColor)
        } else {
            drawPianoKeys(state, primaryColor)
        }
    }
}

private fun DrawScope.drawPianoKeys(state: KeyboardState, primaryColor: androidx.compose.ui.graphics.Color) {
    val totalWhiteKeys = state.octaveCount * 7
    val whiteKeyWidth = size.width / totalWhiteKeys
    val blackKeyWidth = whiteKeyWidth * 0.6f
    val blackKeyHeight = size.height * 0.62f
    val whiteCorner = CornerRadius(10f, 10f)
    val blackCorner = CornerRadius(8f, 8f)
    val gap = 3f

    // White keys
    for (octave in 0 until state.octaveCount) {
        for ((wi, semitone) in WHITE_KEY_OFFSETS.withIndex()) {
            val midiNote = state.octaveStart + octave * 12 + semitone
            val x = (octave * 7 + wi) * whiteKeyWidth
            val isActive = state.activeNotes.contains(midiNote)
            drawRoundRect(
                color = if (isActive) primaryColor else OscilloWhiteKey,
                topLeft = Offset(x + gap / 2, gap),
                size = Size(whiteKeyWidth - gap, size.height - gap * 2),
                cornerRadius = whiteCorner
            )
            if (state.showNoteLabels) {
                drawNoteLabel(PitchUtils.midiNoteToName(midiNote),
                    x + whiteKeyWidth / 2, size.height * 0.9f,
                    whiteKeyWidth * 0.28f,
                    if (isActive) 0xFFFFFFFF.toInt() else 0xFF666666.toInt())
            }
        }
    }

    // Black keys on top
    for (octave in 0 until state.octaveCount) {
        for ((whiteIndex, semitone) in BLACK_KEY_DATA) {
            val midiNote = state.octaveStart + octave * 12 + semitone
            val x = (octave * 7 + whiteIndex) * whiteKeyWidth + whiteKeyWidth * 0.7f
            val isActive = state.activeNotes.contains(midiNote)
            drawRoundRect(
                color = if (isActive) primaryColor else OscilloBlackKey,
                topLeft = Offset(x, 0f),
                size = Size(blackKeyWidth, blackKeyHeight),
                cornerRadius = blackCorner
            )
            if (state.showNoteLabels) {
                drawNoteLabel(PitchUtils.midiNoteToName(midiNote),
                    x + blackKeyWidth / 2, blackKeyHeight * 0.88f,
                    blackKeyWidth * 0.32f,
                    if (isActive) 0xFFFFFFFF.toInt() else 0xFFAAAAAA.toInt())
            }
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
            val isBlack = semitone in setOf(1, 3, 6, 8, 10)
            val x = (octave * 12 + semitone) * keyWidth
            val isActive = state.activeNotes.contains(midiNote)
            val keyHeight = size.height - gap * 2

            drawRoundRect(
                color = when {
                    isActive -> primaryColor
                    isBlack -> OscilloBlackKey
                    else -> OscilloWhiteKey
                },
                topLeft = Offset(x + gap / 2, gap),
                size = Size(keyWidth - gap, keyHeight),
                cornerRadius = cornerRadius
            )

            if (state.showNoteLabels) {
                val textColor = when {
                    isActive -> 0xFFFFFFFF.toInt()
                    isBlack -> 0xFFAAAAAA.toInt()
                    else -> 0xFF666666.toInt()
                }
                drawNoteLabel(PitchUtils.midiNoteToName(midiNote),
                    x + keyWidth / 2, size.height * 0.9f, keyWidth * 0.38f, textColor)
            }
        }
    }
}

private fun DrawScope.drawNoteLabel(text: String, x: Float, y: Float, textSize: Float, color: Int) {
    drawContext.canvas.nativeCanvas.drawText(text, x, y, Paint().apply {
        this.color = color; this.textSize = textSize
        textAlign = Paint.Align.CENTER; isAntiAlias = true
    })
}

private fun hitTest(
    x: Float, y: Float, totalWidth: Float, totalHeight: Float, state: KeyboardState
): Int? {
    if (x < 0f || x > totalWidth) return null
    return if (state.blackKeyLayout == BlackKeyLayout.EQUAL_WIDTH) {
        val keyCount = state.octaveCount * 12
        val keyWidth = totalWidth / keyCount
        val index = (x / keyWidth).toInt().coerceIn(0, keyCount - 1)
        state.octaveStart + index
    } else {
        val totalWhiteKeys = state.octaveCount * 7
        val whiteKeyWidth = totalWidth / totalWhiteKeys
        val blackKeyWidth = whiteKeyWidth * 0.6f
        val blackKeyHeight = totalHeight * 0.62f
        if (y < blackKeyHeight) {
            for (octave in 0 until state.octaveCount) {
                for ((whiteIndex, semitone) in BLACK_KEY_DATA) {
                    val effectiveWhiteIndex = octave * 7 + whiteIndex
                    val keyLeft = effectiveWhiteIndex * whiteKeyWidth + whiteKeyWidth * 0.7f
                    if (x in keyLeft..(keyLeft + blackKeyWidth))
                        return state.octaveStart + octave * 12 + semitone
                }
            }
        }
        val whiteIndex = (x / whiteKeyWidth).toInt().coerceIn(0, totalWhiteKeys - 1)
        state.octaveStart + (whiteIndex / 7) * 12 + WHITE_KEY_OFFSETS[whiteIndex % 7]
    }
}
