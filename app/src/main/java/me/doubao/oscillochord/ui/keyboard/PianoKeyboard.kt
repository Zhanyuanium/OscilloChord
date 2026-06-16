package me.doubao.oscillochord.ui.keyboard

import android.graphics.Paint
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import me.doubao.oscillochord.domain.chord.PitchUtils
import me.doubao.oscillochord.ui.theme.OscilloBlackKey
import me.doubao.oscillochord.ui.theme.OscilloWhiteKey

// White key semitone offsets within one octave
private val WHITE_KEY_OFFSETS = listOf(0, 2, 4, 5, 7, 9, 11)
// Black key: (whiteIndex before the black key, semitone offset from octave start)
private val BLACK_KEY_DATA = listOf(
    0 to 1,   // C# between C(0) and D(1)
    1 to 3,   // D# between D(1) and E(2)
    3 to 6,   // F# between F(3) and G(4)
    4 to 8,   // G# between G(4) and A(5)
    5 to 10   // A# between A(5) and B(6)
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

    // Animated octave offset for smooth transitions
    val animOctaveStart by animateFloatAsState(
        targetValue = state.octaveStart.toFloat(),
        animationSpec = tween(durationMillis = 250)
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val activeKeyColor = primaryColor.copy(alpha = 0.5f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(state.octaveStart, state.octaveCount, state.blackKeyLayout) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        for (pointer in event.changes) {
                            val pid = pointer.id.value.toInt()

                            when {
                                pointer.pressed && !pointer.previousPressed -> {
                                    val note = hitTest(
                                        pointer.position.x, pointer.position.y,
                                        size.width.toFloat(), size.height.toFloat(), state
                                    )
                                    if (note != null) { pointerToNote[pid] = note; onNoteOn(note) }
                                }
                                pointer.pressed && pointer.previousPressed -> {
                                    val prevNote = pointerToNote[pid] ?: continue
                                    val currentNote = hitTest(
                                        pointer.position.x, pointer.position.y,
                                        size.width.toFloat(), size.height.toFloat(), state
                                    )
                                    when (state.slideMode) {
                                        SlideMode.FOLLOW_KEYS -> {
                                            if (currentNote != null && currentNote != prevNote) {
                                                onNoteSlide(prevNote, currentNote)
                                                pointerToNote[pid] = currentNote
                                            }
                                        }
                                        SlideMode.SHIFT_OCTAVE -> {
                                            dragAccumulator += pointer.position.x - pointer.previousPosition.x
                                            val threshold = size.width / (state.octaveCount * 7)
                                            if (dragAccumulator > threshold) {
                                                onOctaveShift(1); dragAccumulator = 0f
                                            } else if (dragAccumulator < -threshold) {
                                                onOctaveShift(-1); dragAccumulator = 0f
                                            }
                                        }
                                    }
                                }
                                !pointer.pressed && pointer.previousPressed -> {
                                    pointerToNote.remove(pid)?.let { onNoteOff(it) }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        if (state.blackKeyLayout == BlackKeyLayout.EQUAL_WIDTH) {
            drawEqualWidthKeys(state, activeKeyColor, primaryColor)
        } else {
            drawPianoKeys(state, activeKeyColor, primaryColor)
        }
    }
}

private fun DrawScope.drawPianoKeys(
    state: KeyboardState, activeColor: Color, primaryColor: Color
) {
    val totalWhiteKeys = state.octaveCount * 7
    val whiteKeyWidth = size.width / totalWhiteKeys
    val blackKeyWidth = whiteKeyWidth * 0.6f
    val blackKeyHeight = size.height * 0.62f
    val cornerRadius = CornerRadius(6f, 6f)
    val gap = 2f

    // White keys
    for (octave in 0 until state.octaveCount) {
        for ((wi, semitone) in WHITE_KEY_OFFSETS.withIndex()) {
            val midiNote = state.octaveStart + octave * 12 + semitone
            val x = (octave * 7 + wi) * whiteKeyWidth
            val isActive = state.activeNotes.contains(midiNote)
            val bgColor = if (isActive) activeColor else OscilloWhiteKey

            drawRoundRect(
                color = bgColor,
                topLeft = Offset(x + gap / 2, gap),
                size = Size(whiteKeyWidth - gap, size.height - gap * 2),
                cornerRadius = cornerRadius
            )

            if (state.showNoteLabels) {
                drawNoteLabel(
                    PitchUtils.midiNoteToName(midiNote),
                    x + whiteKeyWidth / 2, size.height * 0.9f,
                    whiteKeyWidth * 0.28f,
                    if (isActive) 0xFFFFFFFF.toInt() else 0xFF666666.toInt()
                )
            }
        }
    }

    // Black keys
    for (octave in 0 until state.octaveCount) {
        for ((whiteIndex, semitone) in BLACK_KEY_DATA) {
            val effectiveWhiteIndex = octave * 7 + whiteIndex
            val midiNote = state.octaveStart + octave * 12 + semitone
            val x = effectiveWhiteIndex * whiteKeyWidth + whiteKeyWidth * 0.7f
            val isActive = state.activeNotes.contains(midiNote)
            val bgColor = if (isActive) primaryColor else OscilloBlackKey

            drawRoundRect(
                color = bgColor,
                topLeft = Offset(x, 0f),
                size = Size(blackKeyWidth, blackKeyHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )

            if (state.showNoteLabels) {
                drawNoteLabel(
                    PitchUtils.midiNoteToName(midiNote),
                    x + blackKeyWidth / 2, blackKeyHeight * 0.88f,
                    blackKeyWidth * 0.32f,
                    if (isActive) 0xFFFFFFFF.toInt() else 0xFFAAAAAA.toInt()
                )
            }
        }
    }
}

private fun DrawScope.drawEqualWidthKeys(
    state: KeyboardState, activeColor: Color, primaryColor: Color
) {
    val totalKeys = state.octaveCount * 12
    val keyWidth = size.width / totalKeys
    val cornerRadius = CornerRadius(5f, 5f)
    val gap = 2f

    for (octave in 0 until state.octaveCount) {
        for (semitone in 0 until 12) {
            val midiNote = state.octaveStart + octave * 12 + semitone
            val isBlack = semitone in setOf(1, 3, 6, 8, 10)
            val x = (octave * 12 + semitone) * keyWidth
            val isActive = state.activeNotes.contains(midiNote)
            val keyHeight = size.height - gap * 2

            val bgColor = when {
                isActive -> if (isBlack) primaryColor else activeColor
                isBlack -> OscilloBlackKey
                else -> OscilloWhiteKey
            }

            drawRoundRect(
                color = bgColor,
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
                drawNoteLabel(
                    PitchUtils.midiNoteToName(midiNote),
                    x + keyWidth / 2, size.height * 0.9f,
                    keyWidth * 0.38f, textColor
                )
            }
        }
    }
}

private fun DrawScope.drawNoteLabel(text: String, x: Float, y: Float, textSize: Float, color: Int) {
    val paint = Paint().apply {
        this.color = color; this.textSize = textSize
        textAlign = Paint.Align.CENTER; isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
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

        // Check black keys first (upper zone)
        if (y < blackKeyHeight) {
            for (octave in 0 until state.octaveCount) {
                for ((whiteIndex, semitone) in BLACK_KEY_DATA) {
                    val effectiveWhiteIndex = octave * 7 + whiteIndex
                    val keyLeft = effectiveWhiteIndex * whiteKeyWidth + whiteKeyWidth * 0.7f
                    val keyRight = keyLeft + blackKeyWidth
                    if (x in keyLeft..keyRight) {
                        return state.octaveStart + octave * 12 + semitone
                    }
                }
            }
        }

        // Fall through to white key
        val whiteIndex = (x / whiteKeyWidth).toInt().coerceIn(0, totalWhiteKeys - 1)
        val octave = whiteIndex / 7
        val wi = whiteIndex % 7
        state.octaveStart + octave * 12 + WHITE_KEY_OFFSETS[wi]
    }
}
