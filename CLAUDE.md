# CLAUDE.md — OscilloChord

## Build

```bash
export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

## Architecture

MVVM + Clean Architecture. Package: `me.doubao.oscillochord`

- **UI layer** (`ui/`): Compose + Material 3, ViewModels expose `StateFlow`, collected with `collectAsStateWithLifecycle`
- **Domain layer** (`domain/`): AudioEngine, Oscillator, Waveform, ChordDetector, LissajousProjector, MidiInputManager
- **Data layer** (`data/`): SettingsRepository (DataStore Preferences)

## Key Constraints

- minSdk 34, targetSdk 36, compileSdk 36
- Landscape-locked (`AndroidManifest.xml`: `screenOrientation="landscape"`)
- Compose BOM 2025.06, Kotlin 2.0, Gradle 9.4.1, AGP 9.2.1
- No NDK — audio uses `android.media.AudioTrack`

## Critical Patterns

### Audio Engine
- Single long-lived coroutine, AudioTrack created once and kept alive
- Writes silence when no oscillators active (eliminates startup latency)
- Smooth normalization via `lerp` to prevent clicks when oscillator count changes
- `bufferSize` from `AudioTrack.getMinBufferSize()`, buffered in `ShortArray(bufferSize/2)`

### Oscilloscope
- Independent visual oscillators (not synced to audio buffer rate)
- Driven by `LaunchedEffect` + `withFrameNanos` at display refresh rate
- 256 samples per frame per oscillator, projected via `LissajousProjector`
- `OscilloscopeViewModel.maxTrail` controls trail length (settable from settings)
- `OscilloscopeView.trailFadeEnabled` toggles chunked alpha-fade rendering

### Pointer Input
- `pointerInput` keys MUST include all state properties the handler reads. If a property is read inside `when { }` but not in the key list, the gesture detector won't restart when that property changes — behavior gets stuck.
- Use live state reads inside `pointerInput` (read `dragOffset`, `scrollAnim.value` directly), not snapshot `val`s captured at compose time.
- `Animatable.snapTo()` is a suspend function — cannot be called inside `awaitPointerEventScope`. Use `LaunchedEffect` with a counter pattern (not mutableStateOf the request — see below).
- Use `mutableStateOf` counter key for `LaunchedEffect`, NOT the request data itself (setting request to null inside the effect cancels the effect).

### State Management
- `KeyboardState` is a data class; all mutations go through `KeyboardViewModel` via `_state.update { it.copy(...) }`
- Settings flow: SettingsPanel → SettingsViewModel → SettingsRepository (DataStore) → re-collected → LaunchedEffect in MainScreen → KeyboardViewModel methods
- Always wire new settings through the full chain: repository key → SettingsState field → SettingsViewModel setter → MainScreen LaunchedEffect → target VM method

### Fonts (Type.kt)
- `headlineMedium` + `titleMedium` + `bodySmall`: Monospace (for chord/note data)
- `bodyMedium` + `labelSmall`: System default (for UI labels)

## Known Pitfalls

1. **Do not reformat unrelated code.** Keep logic changes minimal. Use ktlint for formatting (separate commits).
2. **PianoKeyboard.kt** has complex pointer handling. Multi-finger drag tracking uses a single `dragPointerId`. The drag-tracking pointer's lift resets `dragPointerId = -1`.
3. **Extra octaves** during scroll/animation use the original `octaveCount` for key sizing, only extending the iteration range. Both drawing and hitTest formulas must match exactly.
4. **`PitchUtils`** uses `Math.floorMod` for pitch class (Java `%` returns negative for negative inputs, causing `ArrayIndexOutOfBounds`).
5. **`hitTest`** receives `displayOffset`-adjusted coordinates. During drag/animation, the `liveOffset` in pointerInput must be recomputed inline (not captured from composable body).
6. **`LaunchedEffect` self-cancellation**: If its key is set to `null` inside the effect body, the effect restarts and the coroutine is cancelled. Use a counter as key, with a separate slot for data.

## Testing

```bash
./gradlew :app:testDebugUnitTest  # unit tests (PitchUtils, Oscillator, AudioEngine, LissajousProjector, ChordDetector, TuningSystem)
```

UI changes must be manually verified on device. No Compose UI tests configured.
