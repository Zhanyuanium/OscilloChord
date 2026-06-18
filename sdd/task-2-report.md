# Phase 2 Report: Resource Management Fixes

## Summary

Four resource-management fixes were applied to the OscilloChord codebase, each addressing a distinct defect identified during the code review.

---

## Task 2.1: MidiInputManager DeviceCallback Leak

**Commit:** `add5670`  
**Files:** `app/src/main/java/me/doubao/oscillochord/domain/midi/MidiInputManager.kt`

**Problem:** `MidiManager.registerDeviceCallback()` was called with an anonymous object every time `startScan()` ran, but the callback was never stored or unregistered in `destroy()`. Each call leaked the callback registration.

**Fix:** Added a `deviceCallback: MidiManager.DeviceCallback?` field. `startScan()` now stores the callback object before registering it. `destroy()` calls `unregisterDeviceCallback()` and nulls the reference before cleaning up opened devices.

---

## Task 2.2: AudioEngine Exception Logging

**Commit:** `d41b633`  
**Files:** `app/src/main/java/me/doubao/oscillochord/domain/audio/AudioEngine.kt`

**Problem:** The `try { audioTrack?.stop() } catch (_: Exception) {}` blocks silently swallowed exceptions during AudioTrack stop/release, making it impossible to diagnose failures during teardown.

**Fix:** Added `import android.util.Log` and replaced the empty catch blocks with `Log.w("AudioEngine", ...)` calls that log the exception with stack trace.

---

## Task 2.3: AudioEngine destroy() Race Condition

**Commit:** `d7d789f`  
**Files:** `app/src/main/java/me/doubao/oscillochord/domain/audio/AudioEngine.kt`

**Problem:** `job?.cancel()` is non-blocking -- it sets the cancellation flag but does not wait for the coroutine to finish. The coroutine's audio buffer write loop could still be accessing `audioTrack` after `destroy()` stopped and released it, causing a race condition.

**Fix:** Replaced `job?.cancel()` with `runBlocking { job?.cancelAndJoin() }`, ensuring the audio coroutine completes before proceeding to `scope.cancel()` and AudioTrack stop/release. Safe because `destroy()` is called from `onCleared()` which runs synchronously on the main thread, and the audio buffer cycle is < 50ms.

---

## Task 2.4: MainActivity MIDI Init Protection

**Commit:** `ea696c0`  
**Files:** `app/src/main/java/me/doubao/oscillochord/MainActivity.kt`

**Problem:** `MidiInputManager` construction and `startScan()` were called without exception handling. If the MIDI service was unavailable (no `MidiManager`), `context.getSystemService` would throw a `ClassCastException`, crashing the Activity on launch.

**Fix:** Wrapped the `MidiInputManager` initialization and `startScan()` in a `try/catch` block that logs the failure via `Log.w("MainActivity", ...)`. The existing `::midiManager.isInitialized` guard in `onDestroy()` already handles the case where initialization failed.

---

## Verification Results

| Check | Result |
|---|---|
| `./gradlew :app:testDebugUnitTest` | BUILD SUCCESSFUL, all tests passing |
| `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL |

## Self-Review

- All changes are minimal and focused -- no reformatting of unrelated code.
- Each commit covers exactly the files and changes described in the task brief.
- The `MidiInputManager` anonymous callback is now retained as a field, preventing the GC from collecting it (the framework holds a strong reference, but storing it explicitly matches Android best practices).
- `runBlocking` in `destroy()` is justified: `ViewModel.onCleared()` runs on the main thread, the audio buffer write completes in under 50ms, and there is no risk of ANR at this scale.
- `lateinit var` in `MainActivity` with the try/catch means the variable may stay uninitialized after a failed MIDI setup. The `::midiManager.isInitialized` guard in `onDestroy()` correctly handles this case.
