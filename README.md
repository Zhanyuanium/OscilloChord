# OscilloChord

A real-time oscilloscope for musical chords on Android. Press keys on the virtual piano (or connect a MIDI keyboard) and watch the Lissajous patterns form as the waveforms interact, while the app identifies the chord and displays note details.

[中文文档](README_zh.md)

![main1](docs/screenshots/main1.png)

![main2](docs/screenshots/main2.png)

## Features

- **Virtual piano keyboard** — piano-style or equal-width layout, multi-touch with per-finger tracking, slide-to-follow or slide-to-shift-octave modes, optional note labels, configurable octave range
- **Real-time oscilloscope** — rotation-projection Lissajous figures for any number of simultaneously played notes, N=1 shows waveform, N=2 shows classic Lissajous, N>=3 uses Scheme A equally-spaced angular projection, optional trail fade with configurable length
- **Chord detection** — recognizes triads, sevenths, ninths, suspended, and added-note chords, displays note names, intervals, and frequencies with inversion-aware root detection
- **Three tuning systems** — equal temperament (12-TET), just intonation (5-limit), and Pythagorean tuning, with configurable base frequency (415--466 Hz)
- **Four waveforms** — sine, square, triangle, sawtooth
- **MIDI input** — USB and Bluetooth MIDI device support
- **Material You design** — dynamic color on Android 12+, dark theme, two layout modes (square for phones, wide for tablets and landscape)
- **Chinese and English** — full i18n support

## Requirements

- Android 14 (API 34) or later
- MIDI input requires a USB-OTG or Bluetooth MIDI device

## Build

```bash
git clone https://github.com/doubao/oscillochord.git
cd oscillochord
./gradlew :app:assembleDebug
```

The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Architecture

MVVM + Clean Architecture, built with Kotlin and Jetpack Compose.

```
app/src/main/java/me/doubao/oscillochord/
├── MainActivity.kt               # Single activity, landscape-locked
├── ui/
│   ├── theme/                     # Material 3 dark theme + dynamic color
│   ├── screen/                    # MainScreen (two layout modes)
│   ├── keyboard/                  # PianoKeyboard + KeyboardViewModel
│   ├── oscilloscope/              # OscilloscopeView + OscilloscopeViewModel
│   ├── info/                      # InfoPanel + InfoViewModel (chord detection)
│   └── settings/                  # SettingsPanel + SettingsViewModel
├── domain/
│   ├── audio/                     # AudioEngine (AudioTrack), Oscillator, Waveform
│   ├── chord/                     # ChordDetector, ChordDatabase, PitchUtils, TuningSystem
│   ├── lissajous/                 # LissajousProjector (rotation projection)
│   └── midi/                      # MidiInputManager
└── data/
    └── SettingsRepository.kt      # DataStore preferences
```

### Tech Stack

| Component | Library |
|-----------|---------|
| UI | Jetpack Compose + Material 3 (BOM 2025.06) |
| Audio | `android.media.AudioTrack` (no NDK) |
| State | Kotlin StateFlow + Compose `collectAsStateWithLifecycle` |
| Persistence | DataStore Preferences |
| Concurrency | Kotlin Coroutines |
| MIDI | `android.media.midi` (API 23+) |

### Key Design Decisions

- **Audio engine runs on a single long-lived coroutine** writing to a permanently-open `AudioTrack` instance. Silence is written when no notes are active, eliminating startup latency. A smooth normalization factor prevents volume-jump clicks when oscillators are added or removed.
- **Oscilloscope uses independent visual oscillators** driven by Compose's `withFrameNanos`, not by the audio buffer rate. Each frame generates 256 sample points per active oscillator, projected to 2D, and drawn as a continuous path.
- **Octave scrolling tracks a pixel-precise offset** during drag, then animates to the nearest octave boundary with a `tween` animation after release, projecting the landing point from the release velocity for natural inertia.
- **Chord detection is inversion-aware** — it tests each played note as a candidate root and matches the resulting pitch-class set against a template database.

## License

MIT
