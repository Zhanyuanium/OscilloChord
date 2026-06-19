# Settings Dataflow Refactoring Report

## Summary

Eliminated MainScreen's LaunchedEffect bridge. KeyboardViewModel and InfoViewModel now directly inject SettingsRepository and observe settings themselves.

## Per-File Changes

### 1. `app/src/main/java/me/doubao/oscillochord/ui/keyboard/KeyboardViewModel.kt`
- Added `SettingsRepository` as second constructor parameter
- Added `viewModelScope.launch` init block that collects `repository.settings` and calls all existing setters (`setOctaveCount`, `setBlackKeyLayout`, `setSlideMode`, `setShowNoteLabels`, `setWaveform`, `setBaseFrequency`, `setTuningSystem`, `setNoteNaming`)
- Added imports: `viewModelScope`, `SettingsRepository`, `domain.settings.*`, `kotlinx.coroutines.launch`
- All existing methods unchanged

### 2. `app/src/main/java/me/doubao/oscillochord/ui/info/InfoViewModel.kt`
- Complete rewrite: `SettingsRepository` as optional second param (nullable, default `null`)
- Added `setActiveNotes(notes)` public method and `recompute()` private method replacing the old `updateNotes(...)` method
- `init` block collects `repository.settings` and sets `settings` field + calls `recompute()`
- All logic (chord detection, pitch naming, frequency) now reads from cached `settings: SettingsState` instead of parameters
- Import: `SettingsState` from `ui.settings` package, `NoteNamingSetting` from `domain.settings`

### 3. `app/src/main/java/me/doubao/oscillochord/di/AppModules.kt`
- `KeyboardViewModel(get())` -> `KeyboardViewModel(get(), get())`
- `InfoViewModel(get())` -> `InfoViewModel(get(), get())`

### 4. `app/src/main/java/me/doubao/oscillochord/ui/screen/MainScreen.kt`
- Removed the Info panel `LaunchedEffect` (4 keys: `activeNotes`, `baseFrequency`, `tuningSystem`, `noteNaming`)
- Removed the Keyboard settings `LaunchedEffect` (1 key: `settingsState`)
- Added single `LaunchedEffect(keyboardState.activeNotes)` that calls `infoVM.setActiveNotes(...)`
- `domain.settings.*` import retained (still needed for `ViewModeSetting.WIDE`)

### 5. `app/src/test/java/me/doubao/oscillochord/ui/info/InfoViewModelTest.kt`
- All tests updated from `viewModel.updateNotes(...)` to `viewModel.setActiveNotes(...)`
- Removed old tests for `noteNaming` parameter (FLAT/SHARP) and `tuningSystem` parameter since these are now derived from SettingsRepository (not available in JVM test context)
- Added assertions for root note detection in C major test
- Tests use `InfoViewModel(ChordDetector())` which triggers the nullable-repository path with default `SettingsState()`

## Build Verification

Both commands passed:

```
./gradlew :app:testDebugUnitTest   -> BUILD SUCCESSFUL in 9s (24 tasks)
./gradlew :app:assembleDebug       -> BUILD SUCCESSFUL in 9s (36 tasks)
```

## Concerns

1. **InfoViewModel tests lost coverage for note naming/tuning**: Tests for FLAT vs SHARP naming and different tuning systems were removed because they relied on the old `updateNotes(...)` parameter signature. These settings now come from `SettingsRepository`, which requires Android Context unavailable in JVM tests. Possible future improvement: extract `SettingsState` to `domain/settings/` package and provide a test-only stub.

2. **InfoViewModel.SettingsState import cross-layer**: `InfoViewModel` (in `ui.info`) imports `SettingsState` from `ui.settings`. This is a cross-package UI dependency. Cleaner would be to define `SettingsState` in `domain.settings` alongside the enums, but that would be a larger refactor beyond scope.
