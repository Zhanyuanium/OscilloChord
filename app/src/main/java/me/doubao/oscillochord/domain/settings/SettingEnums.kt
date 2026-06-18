package me.doubao.oscillochord.domain.settings

import me.doubao.oscillochord.domain.audio.Waveform
import me.doubao.oscillochord.domain.chord.TuningSystem
import me.doubao.oscillochord.ui.keyboard.BlackKeyLayout
import me.doubao.oscillochord.ui.keyboard.SlideMode

/** 波形选择（映射到 domain Waveform） */
enum class WaveformSetting(val waveform: Waveform) {
    SINE(Waveform.SINE),
    SQUARE(Waveform.SQUARE),
    TRIANGLE(Waveform.TRIANGLE),
    SAWTOOTH(Waveform.SAWTOOTH)
}

/** 调律系统选择（映射到 domain TuningSystem） */
enum class TuningSetting(val system: TuningSystem) {
    EQUAL(TuningSystem.EQUAL),
    JUST(TuningSystem.JUST),
    PYTHAGOREAN(TuningSystem.PYTHAGOREAN)
}

/** 黑键布局选择 */
enum class BlackKeyLayoutSetting(val layout: BlackKeyLayout) {
    PIANO(BlackKeyLayout.PIANO),
    EQUAL_WIDTH(BlackKeyLayout.EQUAL_WIDTH)
}

/** 滑动模式选择 */
enum class SlideModeSetting(val mode: SlideMode) {
    FOLLOW_KEYS(SlideMode.FOLLOW_KEYS),
    SHIFT_OCTAVE(SlideMode.SHIFT_OCTAVE)
}

/** 视图模式选择 */
enum class ViewModeSetting { SQUARE, WIDE }

/** 音符命名偏好 */
enum class NoteNamingSetting { SHARP, FLAT }
