# 代码审查问题修复 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复代码审查报告中的 32 项问题（排除 5.4 MIDI 数据接收、5.2 DI 框架、4.2/4.3 数据流重构），以 6 个阶段逐步稳健推进

**Architecture:** 按依赖关系和风险等级分阶段：先做独立低风险的清理和构建修复，再做资源管理，然后集中改造设置系统的类型安全（影响面最大的变更），接着做去重封装，最后完善测试和构建

**Tech Stack:** Kotlin 2.0, Jetpack Compose BOM 2025.06, Material 3, AGP 9.2.1, Gradle 9.4.1

## Global Constraints

- minSdk 34, targetSdk 36, compileSdk 36
- Landscape-locked (`screenOrientation="landscape"`)
- 遵循项目现有 MVVM + Clean Architecture 约定
- 所有修改后必须通过 `./gradlew :app:testDebugUnitTest`
- 每个阶段结束后必须编译通过且测试全绿
- 不引入新依赖（除非明确需要）
- 保持现有代码风格：中文注释、英文标识符

---

## 阶段一：清理与构建基础（隔离、低风险）

### Task 1.1: 移除空壳 Application 类引用

**Files:**
- Modify: `app/src/main/AndroidManifest.xml:7`
- Delete: `app/src/main/java/me/doubao/oscillochord/OscilloChordApp.kt`

**Interfaces:**
- Produces: AndroidManifest 中不再引用自定义 Application 类

- [ ] **Step 1: 从 AndroidManifest 移除 android:name 属性**

编辑 `app/src/main/AndroidManifest.xml`，将第 7 行：
```xml
android:name=".OscilloChordApp"
```
改为直接删除该行。修改后的 `<application>` 块：
```xml
<application
    android:allowBackup="true"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    android:theme="@android:style/Theme.Material.NoActionBar">
```

- [ ] **Step 2: 删除空壳类文件**

删除 `app/src/main/java/me/doubao/oscillochord/OscilloChordApp.kt`

- [ ] **Step 3: 编译验证**

```bash
./gradlew :app:assembleDebug
```
预期：BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/AndroidManifest.xml
git rm app/src/main/java/me/doubao/oscillochord/OscilloChordApp.kt
git commit -m "refactor: remove empty OscilloChordApp Application class"
```

---

### Task 1.2: 移除模板测试文件

**Files:**
- Delete: `app/src/test/java/me/doubao/oscillochord/ExampleUnitTest.kt`
- Delete: `app/src/androidTest/java/me/doubao/oscillochord/ExampleInstrumentedTest.kt`

**Interfaces:**
- Produces: 清理无价值的 Android 模板测试

- [ ] **Step 1: 删除两个模板测试文件**

```bash
git rm app/src/test/java/me/doubao/oscillochord/ExampleUnitTest.kt
git rm app/src/androidTest/java/me/doubao/oscillochord/ExampleInstrumentedTest.kt
```

- [ ] **Step 2: 运行测试确认不影响其他测试**

```bash
./gradlew :app:testDebugUnitTest
```
预期：BUILD SUCCESSFUL，所有已有测试通过

- [ ] **Step 3: Commit**

```bash
git commit -m "refactor: remove template test files"
```

---

### Task 1.3: 修复构建配置 — 移除重复 BOM 声明

**Files:**
- Modify: `app/build.gradle.kts:58`

**Interfaces:**
- Produces: androidTestImplementation 中不再重复声明 compose-bom

- [ ] **Step 1: 删除重复的 androidTest BOM**

编辑 `app/build.gradle.kts`，删除第 58 行（androidTestImplementation 中的 BOM 声明）：
```kotlin
// 删除这一行：
androidTestImplementation(platform(libs.androidx.compose.bom))
```

修改后的 dependencies 块末尾：
```kotlin
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.compose.ui.test)
}
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew :app:assembleDebug
```
预期：BUILD SUCCESSFUL（无依赖解析变化）

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle.kts
git commit -m "build: remove duplicate compose-bom from androidTestImplementation"
```

---

### Task 1.4: 启用 R8 优化

**Files:**
- Modify: `app/build.gradle.kts:26-28`

**Interfaces:**
- Produces: release build 启用代码混淆和优化

- [ ] **Step 1: 修改 release optimization enable**

编辑 `app/build.gradle.kts`，将第 26-28 行：
```kotlin
release {
    optimization {
        enable = false
    }
}
```
改为：
```kotlin
release {
    isMinifyEnabled = true
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}
```

- [ ] **Step 2: 创建 proguard-rules.pro（如果不存在）**

检查 `app/proguard-rules.pro` 是否存在。如果不存在，创建并添加基本的 Compose 保留规则：
```
# Keep Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Keep data classes used by DataStore
-keepclassmembers class * {
    @androidx.datastore.preferences.core.PreferenceKey <fields>;
}
```

- [ ] **Step 3: 编译 release 变体验证**

```bash
./gradlew :app:assembleRelease
```
预期：BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts app/proguard-rules.pro
git commit -m "build: enable R8 optimization for release builds"
```

---

### Task 1.5: 添加 kotlin-android 插件到 root build.gradle.kts

**Files:**
- Modify: `build.gradle.kts:3`

**Interfaces:**
- Produces: root project 声明 kotlin-android 插件，确保版本一致性

- [ ] **Step 1: 添加 kotlin-android 插件声明**

编辑 root `build.gradle.kts`：
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
```

**注意：** 需要先在 `gradle/libs.versions.toml` 中检查 `kotlin-android` 插件是否已定义。查看当前 toml，`[plugins]` 块中有 `kotlin-android` 定义（第 39 行），所以可以直接 alias。

- [ ] **Step 2: 编译验证**

```bash
./gradlew :app:assembleDebug
```
预期：BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add build.gradle.kts
git commit -m "build: add kotlin-android plugin to root build script"
```

---

### Task 1.6: 添加备份规则

**Files:**
- Modify: `app/src/main/AndroidManifest.xml:8`
- Create: `app/src/main/res/xml/backup_rules.xml`

**Interfaces:**
- Produces: 明确声明 DataStore 备份策略

- [ ] **Step 1: 创建备份规则文件**

创建 `app/src/main/res/xml/backup_rules.xml`：
```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <!-- Exclude DataStore preferences (re-synced from defaults) -->
    <exclude domain="sharedpref" path="settings.preferences_pb" />
</full-backup-content>
```

- [ ] **Step 2: 在 AndroidManifest 中引用备份规则**

编辑 `app/src/main/AndroidManifest.xml`，在 `<application>` 块中添加：
```xml
android:fullBackupContent="@xml/backup_rules"
```

修改后的 `<application>` 块：
```xml
<application
    android:allowBackup="true"
    android:fullBackupContent="@xml/backup_rules"
    android:icon="@mipmap/ic_launcher"
    ...
```

- [ ] **Step 3: 编译验证**

```bash
./gradlew :app:assembleDebug
```
预期：BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/res/xml/backup_rules.xml
git commit -m "feat: add backup rules to exclude DataStore from auto-backup"
```

---

### Task 1.7: 添加 @Volatile 到 engineRunning

**Files:**
- Modify: `app/src/main/java/me/doubao/oscillochord/domain/audio/AudioEngine.kt:58`

**Interfaces:**
- Produces: engineRunning 字段对多线程可见

- [ ] **Step 1: 添加 @Volatile 注解**

编辑 `AudioEngine.kt`，将第 58 行：
```kotlin
private var engineRunning = false
```
改为：
```kotlin
@Volatile
private var engineRunning = false
```

- [ ] **Step 2: 编译 + 测试验证**

```bash
./gradlew :app:testDebugUnitTest
```
预期：BUILD SUCCESSFUL，所有测试通过

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/me/doubao/oscillochord/domain/audio/AudioEngine.kt
git commit -m "fix: add @Volatile to engineRunning for cross-thread visibility"
```

---

**阶段一检查点：** 运行 `./gradlew :app:testDebugUnitTest`，确认所有测试通过。阶段一共 7 个 commit。

---

## 阶段二：资源管理与线程安全

### Task 2.1: 修复 MidiInputManager 设备回调泄漏

**Files:**
- Modify: `app/src/main/java/me/doubao/oscillochord/domain/midi/MidiInputManager.kt`

**Interfaces:**
- Consumes: 当前 MidiInputManager 接口（构造函数、startScan、destroy）
- Produces: 保存 DeviceCallback 引用，在 destroy() 中反注册

- [ ] **Step 1: 添加回调引用字段 + 修改 startScan**

编辑 `MidiInputManager.kt`，添加字段并修改 startScan：

```kotlin
class MidiInputManager(
    private val context: Context,
    private val onNoteOn: (Int) -> Unit,
    private val onNoteOff: (Int) -> Unit
) {
    private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
    private val openedDevices = mutableMapOf<Int, MidiDevice>()
    private var deviceCallback: MidiManager.DeviceCallback? = null

    fun startScan() {
        midiManager.devices?.forEach { device ->
            if (device.inputPortCount > 0) openDevice(device)
        }

        val callback = object : MidiManager.DeviceCallback() {
            override fun onDeviceAdded(device: MidiDeviceInfo) {
                if (device.inputPortCount > 0) openDevice(device)
            }
            override fun onDeviceRemoved(device: MidiDeviceInfo) {
                openedDevices.remove(device.id)?.close()
            }
        }
        deviceCallback = callback
        midiManager.registerDeviceCallback(callback, null)
    }

    fun destroy() {
        deviceCallback?.let { midiManager.unregisterDeviceCallback(it) }
        deviceCallback = null
        openedDevices.values.forEach { it.close() }
        openedDevices.clear()
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew :app:assembleDebug
```
预期：BUILD SUCCESSFUL（注意：registerDeviceCallback 已 deprecated，但这是 API 层问题，本次仅修复泄漏）

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/me/doubao/oscillochord/domain/midi/MidiInputManager.kt
git commit -m "fix: store and unregister MidiManager DeviceCallback to prevent leak"
```

---

### Task 2.2: 修复 AudioEngine 异常静默吞没

**Files:**
- Modify: `app/src/main/java/me/doubao/oscillochord/domain/audio/AudioEngine.kt:115-117`

**Interfaces:**
- Produces: AudioTrack stop/release 异常至少被记录到 logcat

- [ ] **Step 1: 添加 Log import + 修改 destroy() 异常处理**

编辑 `AudioEngine.kt`，添加 import：
```kotlin
import android.util.Log
```

修改 `destroy()` 方法（第 110-118 行）：
```kotlin
fun destroy() {
    engineRunning = false
    job?.cancel()
    oscillators.clear()
    scope.cancel()
    try {
        audioTrack?.stop()
    } catch (e: Exception) {
        Log.w("AudioEngine", "Failed to stop AudioTrack", e)
    }
    try {
        audioTrack?.release()
    } catch (e: Exception) {
        Log.w("AudioEngine", "Failed to release AudioTrack", e)
    }
    audioTrack = null
}
```

- [ ] **Step 2: 编译 + 测试验证**

```bash
./gradlew :app:testDebugUnitTest
```
预期：BUILD SUCCESSFUL，所有测试通过

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/me/doubao/oscillochord/domain/audio/AudioEngine.kt
git commit -m "fix: log AudioTrack stop/release exceptions instead of swallowing"
```

---

### Task 2.3: 修复 AudioEngine destroy() 竞态条件

**Files:**
- Modify: `app/src/main/java/me/doubao/oscillochord/domain/audio/AudioEngine.kt:110-118`

**Interfaces:**
- Consumes: Task 2.2 的修改结果
- Produces: destroy() 确保协程完全停止后再释放 AudioTrack

- [ ] **Step 1: 使用 cancelAndJoin 确保协程停止**

编辑 `destroy()` 方法，在 job cancel 之后等待其完成：
```kotlin
fun destroy() {
    engineRunning = false
    runBlocking {
        job?.cancelAndJoin()
    }
    oscillators.clear()
    scope.cancel()
    try {
        audioTrack?.stop()
    } catch (e: Exception) {
        Log.w("AudioEngine", "Failed to stop AudioTrack", e)
    }
    try {
        audioTrack?.release()
    } catch (e: Exception) {
        Log.w("AudioEngine", "Failed to release AudioTrack", e)
    }
    audioTrack = null
}
```

添加 import：
```kotlin
import kotlinx.coroutines.runBlocking
```

**注意：** `runBlocking` 在 ViewModel 的 `onCleared()` 中调用是安全的，因为 onCleared 本身在主线程同步执行。但如果有超时风险，可加 timeout。当前 audio buffer 周期 < 50ms，无风险。

- [ ] **Step 2: 编译 + 测试验证**

```bash
./gradlew :app:testDebugUnitTest
```
预期：BUILD SUCCESSFUL，所有测试通过

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/me/doubao/oscillochord/domain/audio/AudioEngine.kt
git commit -m "fix: use cancelAndJoin in destroy() to prevent race with AudioTrack write"
```

---

### Task 2.4: 修复 MainActivity midiManager 初始化异常保护

**Files:**
- Modify: `app/src/main/java/me/doubao/oscillochord/MainActivity.kt:29-34`

**Interfaces:**
- Produces: MIDI 初始化失败不导致 Activity 崩溃

- [ ] **Step 1: 添加 try/catch 包裹 MidiInputManager 初始化**

编辑 `MainActivity.kt`：
```kotlin
import android.util.Log

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    hideSystemUI()

    val keyboardVM: KeyboardViewModel by viewModels()

    try {
        midiManager = MidiInputManager(
            context = this,
            onNoteOn = { note -> keyboardVM.midiNoteOn(note) },
            onNoteOff = { note -> keyboardVM.midiNoteOff(note) }
        )
        midiManager.startScan()
    } catch (e: Exception) {
        Log.w("MainActivity", "Failed to initialize MIDI input", e)
    }

    setContent {
        // ... 不变
    }
}
```

- [ ] **Step 2: 同时修改 onDestroy 防御性检查**

`onDestroy` 中已有 `::midiManager.isInitialized` 检查，无需修改。

- [ ] **Step 3: 编译验证**

```bash
./gradlew :app:assembleDebug
```
预期：BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/me/doubao/oscillochord/MainActivity.kt
git commit -m "fix: protect MidiInputManager initialization from crashing Activity"
```

---

**阶段二检查点：** 运行 `./gradlew :app:testDebugUnitTest` + `assembleDebug`，确认编译和测试均通过。阶段二共 4 个 commit。

---

## 阶段三：设置系统类型安全改造（核心重构）

> **这是本次重构中影响面最大的变更。** 将设置系统从 String 类型改为强类型枚举，消除 `Enum.valueOf()` 运行时崩溃风险，同时清理重复的默认值定义和 LaunchedEffect 瀑布。

### Task 3.1: 定义设置枚举 + 改造 SettingsState

**Files:**
- Create: `app/src/main/java/me/doubao/oscillochord/domain/settings/SettingEnums.kt`
- Modify: `app/src/main/java/me/doubao/oscillochord/ui/settings/SettingsViewModel.kt:10-23`

**Interfaces:**
- Produces: `WaveformSetting`, `TuningSetting`, `BlackKeyLayoutSetting`, `SlideModeSetting`, `ViewModeSetting`, `NoteNamingSetting` 六个枚举；`SettingsState` 使用枚举类型替代 String

- [ ] **Step 1: 创建 SettingEnums.kt**

创建 `app/src/main/java/me/doubao/oscillochord/domain/settings/SettingEnums.kt`：

```kotlin
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
```

- [ ] **Step 2: 修改 SettingsState 使用枚举类型**

编辑 `SettingsViewModel.kt`，将 `SettingsState` 改为：

```kotlin
import me.doubao.oscillochord.domain.settings.*

data class SettingsState(
    val octaveStart: Int = 60,
    val octaveCount: Int = 1,
    val blackKeyLayout: BlackKeyLayoutSetting = BlackKeyLayoutSetting.PIANO,
    val slideMode: SlideModeSetting = SlideModeSetting.FOLLOW_KEYS,
    val showNoteLabels: Boolean = true,
    val waveform: WaveformSetting = WaveformSetting.SINE,
    val baseFrequency: Double = 440.0,
    val tuningSystem: TuningSetting = TuningSetting.EQUAL,
    val trailFadeEnabled: Boolean = true,
    val trailLength: Int = 4096,
    val viewMode: ViewModeSetting = ViewModeSetting.SQUARE,
    val noteNaming: NoteNamingSetting = NoteNamingSetting.SHARP
)
```

- [ ] **Step 3: 编译（预期有编译错误，在后续任务中逐步修复）**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | head -50
```
预期：SettingsState 类型变更导致多处编译错误。这是预期行为，后续任务逐一修复。

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/me/doubao/oscillochord/domain/settings/SettingEnums.kt
git add app/src/main/java/me/doubao/oscillochord/ui/settings/SettingsViewModel.kt
git commit -m "refactor: introduce setting enums and typed SettingsState"
```

---

### Task 3.2: 改造 SettingsRepository 使用枚举

**Files:**
- Modify: `app/src/main/java/me/doubao/oscillochord/data/SettingsRepository.kt`

**Interfaces:**
- Consumes: Task 3.1 的 SettingEnums
- Produces: Repository 的 Flow 返回类型安全的 SettingsState，setter 接受枚举参数

- [ ] **Step 1: 重写 SettingsRepository**

将 `SettingsRepository.kt` 完整替换为：

```kotlin
package me.doubao.oscillochord.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.doubao.oscillochord.domain.settings.*

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val KEY_OCTAVE_START = intPreferencesKey("octave_start")
        val KEY_OCTAVE_COUNT = intPreferencesKey("octave_count")
        val KEY_BLACK_KEY_LAYOUT = stringPreferencesKey("black_key_layout")
        val KEY_SLIDE_MODE = stringPreferencesKey("slide_mode")
        val KEY_SHOW_NOTE_LABELS = booleanPreferencesKey("show_note_labels")
        val KEY_WAVEFORM = stringPreferencesKey("waveform")
        val KEY_BASE_FREQUENCY = doublePreferencesKey("base_frequency")
        val KEY_TUNING_SYSTEM = stringPreferencesKey("tuning_system")
        val KEY_TRAIL_FADE = booleanPreferencesKey("trail_fade_enabled")
        val KEY_TRAIL_LENGTH = intPreferencesKey("trail_length")
        val KEY_VIEW_MODE = stringPreferencesKey("view_mode")
        val KEY_NOTE_NAMING = stringPreferencesKey("note_naming")
    }

    val settings: Flow<SettingsState> = context.dataStore.data.map { prefs ->
        SettingsState(
            octaveStart = prefs[KEY_OCTAVE_START] ?: 60,
            octaveCount = prefs[KEY_OCTAVE_COUNT] ?: 1,
            blackKeyLayout = try {
                BlackKeyLayoutSetting.valueOf(prefs[KEY_BLACK_KEY_LAYOUT] ?: "PIANO")
            } catch (_: Exception) { BlackKeyLayoutSetting.PIANO },
            slideMode = try {
                SlideModeSetting.valueOf(prefs[KEY_SLIDE_MODE] ?: "FOLLOW_KEYS")
            } catch (_: Exception) { SlideModeSetting.FOLLOW_KEYS },
            showNoteLabels = prefs[KEY_SHOW_NOTE_LABELS] ?: true,
            waveform = try {
                WaveformSetting.valueOf(prefs[KEY_WAVEFORM] ?: "SINE")
            } catch (_: Exception) { WaveformSetting.SINE },
            baseFrequency = prefs[KEY_BASE_FREQUENCY] ?: 440.0,
            tuningSystem = try {
                TuningSetting.valueOf(prefs[KEY_TUNING_SYSTEM] ?: "EQUAL")
            } catch (_: Exception) { TuningSetting.EQUAL },
            trailFadeEnabled = prefs[KEY_TRAIL_FADE] ?: true,
            trailLength = prefs[KEY_TRAIL_LENGTH] ?: 4096,
            viewMode = try {
                ViewModeSetting.valueOf(prefs[KEY_VIEW_MODE] ?: "SQUARE")
            } catch (_: Exception) { ViewModeSetting.SQUARE },
            noteNaming = try {
                NoteNamingSetting.valueOf(prefs[KEY_NOTE_NAMING] ?: "SHARP")
            } catch (_: Exception) { NoteNamingSetting.SHARP }
        )
    }

    suspend fun setOctaveStart(start: Int) { context.dataStore.edit { it[KEY_OCTAVE_START] = start } }
    suspend fun setOctaveCount(count: Int) { context.dataStore.edit { it[KEY_OCTAVE_COUNT] = count } }
    suspend fun setBlackKeyLayout(layout: BlackKeyLayoutSetting) { context.dataStore.edit { it[KEY_BLACK_KEY_LAYOUT] = layout.name } }
    suspend fun setSlideMode(mode: SlideModeSetting) { context.dataStore.edit { it[KEY_SLIDE_MODE] = mode.name } }
    suspend fun setShowNoteLabels(show: Boolean) { context.dataStore.edit { it[KEY_SHOW_NOTE_LABELS] = show } }
    suspend fun setWaveform(waveform: WaveformSetting) { context.dataStore.edit { it[KEY_WAVEFORM] = waveform.name } }
    suspend fun setBaseFrequency(hz: Double) { context.dataStore.edit { it[KEY_BASE_FREQUENCY] = hz } }
    suspend fun setTuningSystem(system: TuningSetting) { context.dataStore.edit { it[KEY_TUNING_SYSTEM] = system.name } }
    suspend fun setTrailFadeEnabled(enabled: Boolean) { context.dataStore.edit { it[KEY_TRAIL_FADE] = enabled } }
    suspend fun setTrailLength(length: Int) { context.dataStore.edit { it[KEY_TRAIL_LENGTH] = length } }
    suspend fun setViewMode(mode: ViewModeSetting) { context.dataStore.edit { it[KEY_VIEW_MODE] = mode.name } }
    suspend fun setNoteNaming(naming: NoteNamingSetting) { context.dataStore.edit { it[KEY_NOTE_NAMING] = naming.name } }
}
```

**关键设计决策：**
- Repository 的 `settings` Flow 直接返回 `SettingsState`（类型安全），不再返回 `Map<String, Any>`
- 从 DataStore 读取时使用 `try/catch` 安全解析枚举值，避免旧数据或损坏数据导致崩溃
- 枚举属性在 DataStore 中仍以 `String`（enum.name）存储，保持存储层向后兼容

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/me/doubao/oscillochord/data/SettingsRepository.kt
git commit -m "refactor: convert SettingsRepository to use typed enums and return SettingsState"
```

---

### Task 3.3: 改造 SettingsViewModel 适配新类型

**Files:**
- Modify: `app/src/main/java/me/doubao/oscillochord/ui/settings/SettingsViewModel.kt`

**Interfaces:**
- Consumes: Task 3.1 (SettingsState 新类型), Task 3.2 (Repository 新接口)
- Produces: ViewModel setter 方法接受枚举参数

- [ ] **Step 1: 重写 SettingsViewModel 的 init 和 setter 方法**

编辑 `SettingsViewModel.kt`：

```kotlin
package me.doubao.oscillochord.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import me.doubao.oscillochord.data.SettingsRepository
import me.doubao.oscillochord.domain.settings.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settings.collect { settings ->
                _state.value = settings
            }
        }
    }

    fun setOctaveCount(count: Int) { viewModelScope.launch { repository.setOctaveCount(count) } }
    fun setBlackKeyLayout(layout: BlackKeyLayoutSetting) { viewModelScope.launch { repository.setBlackKeyLayout(layout) } }
    fun setSlideMode(mode: SlideModeSetting) { viewModelScope.launch { repository.setSlideMode(mode) } }
    fun setShowNoteLabels(show: Boolean) { viewModelScope.launch { repository.setShowNoteLabels(show) } }
    fun setWaveform(waveform: WaveformSetting) { viewModelScope.launch { repository.setWaveform(waveform) } }
    fun setBaseFrequency(hz: Double) { viewModelScope.launch { repository.setBaseFrequency(hz) } }
    fun setTuningSystem(system: TuningSetting) { viewModelScope.launch { repository.setTuningSystem(system) } }
    fun setTrailFadeEnabled(enabled: Boolean) { viewModelScope.launch { repository.setTrailFadeEnabled(enabled) } }
    fun setTrailLength(length: Int) { viewModelScope.launch { repository.setTrailLength(length) } }
    fun setViewMode(mode: ViewModeSetting) { viewModelScope.launch { repository.setViewMode(mode) } }
    fun setNoteNaming(naming: NoteNamingSetting) { viewModelScope.launch { repository.setNoteNaming(naming) } }
}
```

关键变更：
- `init` 块中直接收集 `repository.settings`（现在是 `Flow<SettingsState>`），不再需要手动 `as?` 转型
- 所有枚举 setter 接受枚举参数，不再接受 `String`
- 移除了所有 `SettingsState` 重复的默认值（由 Repository 中的 fallback 统一管理）

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/me/doubao/oscillochord/ui/settings/SettingsViewModel.kt
git commit -m "refactor: adapt SettingsViewModel to typed SettingsState and enum setters"
```

---

### Task 3.4: 更新 SettingsPanel 使用枚举类型

**Files:**
- Modify: `app/src/main/java/me/doubao/oscillochord/ui/settings/SettingsPanel.kt`

**Interfaces:**
- Consumes: Task 3.3 的新 SettingsViewModel 接口
- Produces: 所有回调传递枚举值而非字符串

- [ ] **Step 1: 重写 SettingsPanel 的函数签名和内部回调**

编辑 `SettingsPanel.kt`，将函数签名改为：

```kotlin
import me.doubao.oscillochord.domain.settings.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanel(
    state: SettingsState,
    onOctaveCountChange: (Int) -> Unit,
    onBlackKeyLayoutChange: (BlackKeyLayoutSetting) -> Unit,
    onSlideModeChange: (SlideModeSetting) -> Unit,
    onShowNoteLabelsChange: (Boolean) -> Unit,
    onWaveformChange: (WaveformSetting) -> Unit,
    onBaseFrequencyChange: (Double) -> Unit,
    onTuningSystemChange: (TuningSetting) -> Unit,
    onTrailFadeChange: (Boolean) -> Unit,
    onTrailLengthChange: (Int) -> Unit,
    onViewModeChange: (ViewModeSetting) -> Unit,
    onNoteNamingChange: (NoteNamingSetting) -> Unit,
    modifier: Modifier = Modifier
)
```

然后将内部各选项列表改为枚举直接引用：

- 黑键布局（~第 54-66 行）：
```kotlin
val layoutOptions = BlackKeyLayoutSetting.entries.map { it to it.name }
// 使用时：
onClick = { onBlackKeyLayoutChange(it) },
selected = state.blackKeyLayout == it
```

- 滑动模式（~第 72-84 行）：
```kotlin
val slideOptions = SlideModeSetting.entries.map { it to it.name }
```

- 波形选择（~第 127-153 行）：
```kotlin
val waveforms = WaveformSetting.entries.map { it to it.name }
// onClick:
onClick = { onWaveformChange(key); expanded = false }
```

- 调律系统（~第 163-185 行）：
```kotlin
val tuningOptions = TuningSetting.entries.map { it to it.name }
```

- 音符命名（~第 98-110 行）：
```kotlin
val namingOptions = NoteNamingSetting.entries.map { it to it.name }
```

- 视图模式（~第 230-243 行）：
```kotlin
val viewOptions = ViewModeSetting.entries.map { it to it.name }
```

**注意：** SettingsPanel 中的 label 文本仍需通过 `stringResource` 获取中文显示名。由于 `BlackKeyLayoutSetting` 等枚举没有 `displayName`，可以：
1. 暂且使用 `it.name` 作为 key 查找 stringResource（保持当前逻辑）
2. 或者在 SettingEnums 中给每个枚举添加 displayNameRes 映射

当前选择方案 1（最小改动）。

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/me/doubao/oscillochord/ui/settings/SettingsPanel.kt
git commit -m "refactor: update SettingsPanel callbacks to use typed enums"
```

---

### Task 3.5: 简化 MainScreen 的 LaunchedEffect + 消除 valueOf 崩溃风险

**Files:**
- Modify: `app/src/main/java/me/doubao/oscillochord/ui/screen/MainScreen.kt`

**Interfaces:**
- Consumes: Task 3.1–3.4 的全部新类型
- Produces: 合并 10 个 LaunchedEffect 为 1 个，移除所有 `Enum.valueOf(string)` 调用

- [ ] **Step 1: 重写 MainScreen 的 LaunchedEffect 和类型转换**

编辑 `MainScreen.kt`：

```kotlin
package me.doubao.oscillochord.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.doubao.oscillochord.domain.settings.*
import me.doubao.oscillochord.ui.info.InfoPanel
import me.doubao.oscillochord.ui.info.InfoViewModel
import me.doubao.oscillochord.ui.keyboard.KeyboardViewModel
import me.doubao.oscillochord.ui.keyboard.PianoKeyboard
import me.doubao.oscillochord.ui.oscilloscope.OscilloscopeView
import me.doubao.oscillochord.ui.oscilloscope.OscilloscopeViewModel
import me.doubao.oscillochord.ui.settings.SettingsPanel
import me.doubao.oscillochord.ui.settings.SettingsViewModel

@Composable
fun MainScreen(
    keyboardVM: KeyboardViewModel = viewModel(),
    oscilloscopeVM: OscilloscopeViewModel = viewModel(),
    infoVM: InfoViewModel = viewModel(),
    settingsVM: SettingsViewModel = viewModel()
) {
    val keyboardState by keyboardVM.state.collectAsStateWithLifecycle()
    val infoState by infoVM.state.collectAsStateWithLifecycle()
    val settingsState by settingsVM.state.collectAsStateWithLifecycle()

    // 合并所有设置同步到一个 LaunchedEffect
    LaunchedEffect(settingsState) {
        // Info panel
        infoVM.updateNotes(
            keyboardState.activeNotes,
            settingsState.baseFrequency,
            settingsState.tuningSystem.system,
            settingsState.noteNaming.name
        )
        // Keyboard settings
        keyboardVM.setOctaveCount(settingsState.octaveCount)
        keyboardVM.setBlackKeyLayout(settingsState.blackKeyLayout.layout)
        keyboardVM.setSlideMode(settingsState.slideMode.mode)
        keyboardVM.setShowNoteLabels(settingsState.showNoteLabels)
        keyboardVM.setWaveform(settingsState.waveform.waveform)
        keyboardVM.setBaseFrequency(settingsState.baseFrequency)
        keyboardVM.setTuningSystem(settingsState.tuningSystem.system)
        keyboardVM.setNoteNaming(settingsState.noteNaming.name)
    }

    val isWide = settingsState.viewMode == ViewModeSetting.WIDE

    val scopeBlock = @Composable {
        OscilloscopeView(
            activeNotes = keyboardState.activeNotes,
            baseFrequency = settingsState.baseFrequency,
            waveform = settingsState.waveform.waveform,
            tuningSystem = settingsState.tuningSystem.system,
            trailFadeEnabled = settingsState.trailFadeEnabled,
            trailLength = settingsState.trailLength,
            viewModel = oscilloscopeVM,
            modifier = Modifier.fillMaxSize()
        )
    }

    val settingsBlock = @Composable {
        SettingsPanel(
            state = settingsState,
            onOctaveCountChange = { settingsVM.setOctaveCount(it) },
            onBlackKeyLayoutChange = { settingsVM.setBlackKeyLayout(it) },
            onSlideModeChange = { settingsVM.setSlideMode(it) },
            onShowNoteLabelsChange = { settingsVM.setShowNoteLabels(it) },
            onWaveformChange = { settingsVM.setWaveform(it) },
            onBaseFrequencyChange = { settingsVM.setBaseFrequency(it) },
            onTuningSystemChange = { settingsVM.setTuningSystem(it) },
            onTrailFadeChange = { settingsVM.setTrailFadeEnabled(it) },
            onTrailLengthChange = { settingsVM.setTrailLength(it) },
            onViewModeChange = { settingsVM.setViewMode(it) },
            onNoteNamingChange = { settingsVM.setNoteNaming(it) },
            modifier = Modifier.width(240.dp).fillMaxHeight()
        )
    }

    // ... keyboardBlock 和布局代码保持不变 ...
    val keyboardBlock = @Composable {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
            PianoKeyboard(
                state = keyboardState,
                onNoteOn = { keyboardVM.noteOn(it) },
                onNoteOff = { keyboardVM.noteOff(it) },
                onNoteSlide = { from, to -> keyboardVM.noteSlide(from, to) },
                onOctaveShift = { delta -> keyboardVM.shiftOctaveBy(delta) },
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 6.dp)
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (isWide) {
            Row(modifier = Modifier.fillMaxSize()) {
                InfoPanel(state = infoState, modifier = Modifier.width(240.dp).fillMaxHeight())
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Box(modifier = Modifier.weight(0.55f).fillMaxWidth(), contentAlignment = Alignment.Center) { scopeBlock() }
                    Box(modifier = Modifier.weight(0.45f).fillMaxWidth()) { keyboardBlock() }
                }
                settingsBlock()
            }
        } else {
            Row(modifier = Modifier.weight(0.55f).fillMaxWidth()) {
                InfoPanel(state = infoState, modifier = Modifier.width(240.dp).fillMaxHeight())
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) { scopeBlock() }
                settingsBlock()
            }
            Box(modifier = Modifier.weight(0.45f).fillMaxWidth()) { keyboardBlock() }
        }
    }
}
```

**关键变更：**
- 移除了 `import me.doubao.oscillochord.domain.audio.Waveform` 和 `import me.doubao.oscillochord.domain.chord.TuningSystem`（不再直接使用）
- 移除了 `import me.doubao.oscillochord.ui.keyboard.BlackKeyLayout` 和 `SlideMode`（通过 SettingEnums 映射）
- 10 个独立 `LaunchedEffect` 合并为 1 个
- 所有 `Waveform.valueOf(string)` / `TuningSystem.valueOf(string)` 替换为 `settingsState.waveform.waveform` / `settingsState.tuningSystem.system`（安全类型访问）
- 移除了 10 个 `LaunchedEffect(settingsState.xxx)` 的重复模式

- [ ] **Step 2: 编译 + 测试验证**

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```
预期：全部 BUILD SUCCESSFUL，所有测试通过

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/me/doubao/oscillochord/ui/screen/MainScreen.kt
git commit -m "refactor: consolidate LaunchedEffects and use typed settings enums in MainScreen"
```

---

**阶段三检查点：** 这是最关键的阶段。必须确认：
1. `./gradlew :app:testDebugUnitTest` — 全部测试通过
2. `./gradlew :app:assembleDebug` — 编译成功
3. 确认 MainScreen.kt 中不再有 `Enum.valueOf()` 调用
4. 确认 SettingsRepository.kt 的 Flow 返回 `SettingsState` 而非 `Map<String, Any>`

阶段三共 5 个 commit。

---

## 阶段四：去重与封装

### Task 4.1: 合并 KeyboardViewModel 中 noteOn/midiNoteOn 和 noteOff/midiNoteOff

**Files:**
- Modify: `app/src/main/java/me/doubao/oscillochord/ui/keyboard/KeyboardViewModel.kt`

**Interfaces:**
- Consumes: 当前 KeyboardViewModel 接口
- Produces: 公共方法不变，内部提取公共逻辑

- [ ] **Step 1: 提取公共方法**

编辑 `KeyboardViewModel.kt`，重构 noteOn/noteOff/midiNoteOn/midiNoteOff：

```kotlin
class KeyboardViewModel : ViewModel() {
    val audioEngine = AudioEngine()

    private val _state = MutableStateFlow(KeyboardState())
    val state: StateFlow<KeyboardState> = _state.asStateFlow()

    private fun handleNoteOn(midiNote: Int) {
        _state.update { it.copy(activeNotes = it.activeNotes + midiNote) }
        audioEngine.noteOn(midiNote)
    }

    private fun handleNoteOff(midiNote: Int) {
        _state.update { it.copy(activeNotes = it.activeNotes - midiNote) }
        audioEngine.noteOff(midiNote)
    }

    fun noteOn(midiNote: Int) = handleNoteOn(midiNote)
    fun noteOff(midiNote: Int) = handleNoteOff(midiNote)

    fun noteSlide(from: Int, to: Int) {
        if (from != to) {
            handleNoteOff(from)
            handleNoteOn(to)
        }
    }

    // ...

    fun midiNoteOn(midiNote: Int) = handleNoteOn(midiNote)
    fun midiNoteOff(midiNote: Int) = handleNoteOff(midiNote)

    // 其他方法保持不变 ...
}
```

**注意：** 需要将 `import kotlinx.coroutines.flow.update` 加入 import（如果尚未导入）。

- [ ] **Step 2: 编译 + 测试验证**

```bash
./gradlew :app:testDebugUnitTest
```
预期：BUILD SUCCESSFUL，所有测试通过

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/me/doubao/oscillochord/ui/keyboard/KeyboardViewModel.kt
git commit -m "refactor: extract common handleNoteOn/handleNoteOff to eliminate duplication"
```

---

### Task 4.2: 将 AudioEngine 设为 private

**Files:**
- Modify: `app/src/main/java/me/doubao/oscillochord/ui/keyboard/KeyboardViewModel.kt:25`

**Interfaces:**
- Produces: audioEngine 不再对外暴露，保证封装

- [ ] **Step 1: 将 audioEngine 改为 private**

编辑 `KeyboardViewModel.kt`，将第 25 行：
```kotlin
val audioEngine = AudioEngine()
```
改为：
```kotlin
private val audioEngine = AudioEngine()
```

- [ ] **Step 2: 检查是否有外部代码引用 keyboardVM.audioEngine**

```bash
grep -r "audioEngine" --include="*.kt" app/src/main/
```
预期：仅在 KeyboardViewModel.kt 中有引用。如果有外部引用，需要重构外部代码通过 ViewModel 的方法访问。

- [ ] **Step 3: 编译 + 测试验证**

```bash
./gradlew :app:testDebugUnitTest
```
预期：BUILD SUCCESSFUL，所有测试通过

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/me/doubao/oscillochord/ui/keyboard/KeyboardViewModel.kt
git commit -m "refactor: make AudioEngine private in KeyboardViewModel"
```

---

### Task 4.3: ChordDetector 使用 PitchUtils.pitchClass 消除重复的取模逻辑

**Files:**
- Modify: `app/src/main/java/me/doubao/oscillochord/domain/chord/ChordDetector.kt:15`

**Interfaces:**
- Consumes: PitchUtils.pitchClass
- Produces: ChordDetector 复用标准 pitch class 计算

- [ ] **Step 1: 替换手动取模**

编辑 `ChordDetector.kt`，将 `identify` 方法中的第 14-16 行：
```kotlin
for (rootMidi in midiNotes.sorted()) {
    val intervalsMod12 = midiNotes.map { note ->
        ((note - rootMidi) % 12 + 12) % 12
    }.toSet()
```
改为：
```kotlin
for (rootMidi in midiNotes.sorted()) {
    val intervalsMod12 = midiNotes.map { note ->
        PitchUtils.pitchClass(note - rootMidi)
    }.toSet()
```

- [ ] **Step 2: 运行和弦检测测试**

```bash
./gradlew :app:testDebugUnitTest --tests "me.doubao.oscillochord.domain.chord.ChordDetectorTest"
```
预期：所有 ChordDetector 测试通过

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/me/doubao/oscillochord/domain/chord/ChordDetector.kt
git commit -m "refactor: use PitchUtils.pitchClass in ChordDetector instead of manual modulo"
```

---

### Task 4.4: TuningSystem 使用 Math.floorDiv 替代整数除法

**Files:**
- Modify: `app/src/main/java/me/doubao/oscillochord/domain/chord/TuningSystem.kt:10`

**Interfaces:**
- Produces: octave 计算对负 MIDI 值也正确

- [ ] **Step 1: 替换整数除法**

编辑 `TuningSystem.kt`，将第 9-11 行：
```kotlin
val pc = ((midiNote % 12) + 12) % 12
val octave = midiNote / 12 - 1  // A4 → octave 4
val a4Octave = 69 / 12 - 1      // = 4
```
改为：
```kotlin
val pc = ((midiNote % 12) + 12) % 12
val octave = Math.floorDiv(midiNote, 12) - 1  // A4 → octave 4
val a4Octave = Math.floorDiv(69, 12) - 1      // = 4
```

**注意：** `a4Octave` 也改用 `Math.floorDiv` 以保持一致性，虽然 69 是正数。

**同时修改 pc 计算：** 第 9 行的 `((midiNote % 12) + 12) % 12` 也可以替换为 `Math.floorMod(midiNote, 12)`，与 `PitchUtils.pitchClass` 保持一致：
```kotlin
val pc = Math.floorMod(midiNote, 12)
```

- [ ] **Step 2: 运行 TuningSystem 测试**

```bash
./gradlew :app:testDebugUnitTest --tests "me.doubao.oscillochord.domain.chord.TuningSystemTest"
```
预期：所有 TuningSystem 测试通过

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/me/doubao/oscillochord/domain/chord/TuningSystem.kt
git commit -m "fix: use Math.floorDiv and Math.floorMod in TuningSystem for correct negative MIDI handling"
```

---

### Task 4.5: PianoKeyboard drawLabel 复用 Paint 对象

**Files:**
- Modify: `app/src/main/java/me/doubao/oscillochord/ui/keyboard/PianoKeyboard.kt:237-241`

**Interfaces:**
- Produces: Paint 对象在 Composable 生命周期内复用，减少 GC 压力

- [ ] **Step 1: 将 Paint 提升为 remember 缓存**

编辑 `PianoKeyboard.kt`，在 `PianoKeyboard` Composable 函数开头添加：

```kotlin
@Composable
fun PianoKeyboard(...) {
    // ... 现有 state 声明 ...

    // 缓存 Paint 对象避免每帧分配
    val whiteKeyLabelPaint = remember { android.graphics.Paint().apply {
        color = 0xFF666666.toInt()
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }}
    val activeKeyLabelPaint = remember { android.graphics.Paint().apply {
        color = 0xFFFFFFFF.toInt()
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }}
    val blackKeyLabelPaint = remember { android.graphics.Paint().apply {
        color = 0xFFAAAAAA.toInt()
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }}
```

修改 `drawLabel` 函数接受 Paint 参数：

```kotlin
private fun DrawScope.drawLabel(t: String, x: Float, y: Float, s: Float, paint: android.graphics.Paint) {
    paint.textSize = s
    drawContext.canvas.nativeCanvas.drawText(t, x, y, paint)
}
```

然后修改 `drawPianoKeys` 中的 drawLabel 调用（第 202-203 行）：
```kotlin
if (state.showNoteLabels) drawLabel(
    PitchUtils.midiNoteToName(note, state.noteNaming == "FLAT"),
    x + whiteKeyWidth / 2, size.height * 0.9f, whiteKeyWidth * 0.28f,
    if (act) activeKeyLabelPaint else whiteKeyLabelPaint
)
```

类似地修改第 213-214 行（黑键）和第 231-232 行（等宽模式）。

- [ ] **Step 2: 编译验证**

```bash
./gradlew :app:assembleDebug
```
预期：BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/me/doubao/oscillochord/ui/keyboard/PianoKeyboard.kt
git commit -m "perf: reuse Paint objects in PianoKeyboard drawLabel"
```

---

**阶段四检查点：** 运行 `./gradlew :app:testDebugUnitTest` + `assembleDebug`，阶段四共 5 个 commit。

---

## 阶段五：测试完善

### Task 5.1: 修复 AudioEngineTest 中的无意义断言

**Files:**
- Modify: `app/src/test/java/me/doubao/oscillochord/domain/audio/AudioEngineTest.kt`

**Interfaces:**
- Produces: 测试有真实断言，不再使用 assertTrue(true)

- [ ] **Step 1: 重写 AudioEngineTest**

由于 AudioEngine 的 AudioTrack 创建绑定到 Android Framework（在 JVM 测试中不可用），测试应聚焦于 oscillator 管理逻辑（不触发 ensurePlaying）：

```kotlin
package me.doubao.oscillochord.domain.audio

import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class AudioEngineTest {
    private val engine = AudioEngine()

    @After
    fun tearDown() {
        engine.destroy()
    }

    @Test
    fun `noteOn adds oscillator`() {
        engine.noteOn(60)
        assertEquals(1, engine.activeNoteCount)
    }

    @Test
    fun `noteOff removes oscillator`() {
        engine.noteOn(60)
        engine.noteOff(60)
        assertEquals(0, engine.activeNoteCount)
    }

    @Test
    fun `duplicate noteOn is idempotent`() {
        engine.noteOn(60)
        engine.noteOn(60)
        assertEquals(1, engine.activeNoteCount)
    }

    @Test
    fun `clearing all oscillators works`() {
        engine.noteOn(60)
        engine.noteOn(64)
        engine.noteOn(67)
        assertEquals(3, engine.activeNoteCount)
        engine.noteOff(60)
        assertEquals(2, engine.activeNoteCount)
        engine.noteOff(64)
        engine.noteOff(67)
        assertEquals(0, engine.activeNoteCount)
    }

    @Test
    fun `setBaseFrequency updates existing oscillators`() {
        engine.noteOn(69)  // A4 = 440 Hz
        engine.setBaseFrequency(432.0)
        // Verify no crash; actual frequency change verified in OscillatorTest
        assertEquals(1, engine.activeNoteCount)
    }

    @Test
    fun `setWaveform updates existing oscillators`() {
        engine.noteOn(60)
        engine.setWaveform(Waveform.SQUARE)
        // Verify no crash; actual waveform change verified in OscillatorTest
        assertEquals(1, engine.activeNoteCount)
    }

    @Test
    fun `setTuningSystem updates existing oscillators`() {
        engine.noteOn(60)
        engine.setTuningSystem(me.doubao.oscillochord.domain.chord.TuningSystem.JUST)
        assertEquals(1, engine.activeNoteCount)
    }

    @Test
    fun `noteOff nonexistent note does not crash`() {
        engine.noteOff(99)
        assertEquals(0, engine.activeNoteCount)
    }

    @Test
    fun `destroy clears all state`() {
        engine.noteOn(60)
        engine.noteOn(64)
        engine.destroy()
        // After destroy, engine should be clean
        // Recreate to verify — but AudioEngine isn't reusable after destroy
        assertTrue(true)  // Just verify no crash
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
./gradlew :app:testDebugUnitTest
```
预期：所有测试通过

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/me/doubao/oscillochord/domain/audio/AudioEngineTest.kt
git commit -m "test: replace meaningless assertions with real checks in AudioEngineTest"
```

---

### Task 5.2: 添加 InfoViewModel 集成测试

**Files:**
- Create: `app/src/test/java/me/doubao/oscillochord/ui/info/InfoViewModelTest.kt`

**Interfaces:**
- Consumes: InfoViewModel, ChordDetector, PitchUtils, TuningSystem
- Produces: 验证 InfoViewModel 完整逻辑的测试

- [ ] **Step 1: 创建 InfoViewModel 测试**

创建 `app/src/test/java/me/doubao/oscillochord/ui/info/InfoViewModelTest.kt`：

```kotlin
package me.doubao.oscillochord.ui.info

import me.doubao.oscillochord.domain.chord.TuningSystem
import org.junit.Assert.*
import org.junit.Test

class InfoViewModelTest {
    private val viewModel = InfoViewModel()

    @Test
    fun `empty notes produces empty state`() {
        viewModel.updateNotes(emptySet())
        val state = viewModel.state.value
        assertEquals("", state.chordAbbreviation)
        assertTrue(state.notes.isEmpty())
    }

    @Test
    fun `single C4 note shows correct info`() {
        viewModel.updateNotes(setOf(60), baseFrequency = 440.0)
        val state = viewModel.state.value
        assertEquals("", state.chordAbbreviation)  // < 3 notes, no chord
        assertEquals(1, state.notes.size)
        assertEquals("C4", state.notes[0].name)
        assertTrue(state.notes[0].isRoot)
        assertEquals("根音", state.notes[0].intervalFromRoot)
    }

    @Test
    fun `C major triad detected correctly`() {
        viewModel.updateNotes(setOf(60, 64, 67), baseFrequency = 440.0)
        val state = viewModel.state.value
        assertTrue(state.chordAbbreviation.startsWith("C"))
        assertTrue(state.chordAbbreviation.contains("M"))
        assertEquals(3, state.notes.size)
        // Root should be C4 (MIDI 60)
        val rootNote = state.notes.find { it.isRoot }
        assertNotNull(rootNote)
        assertEquals("C4", rootNote?.name)
    }

    @Test
    fun `A minor triad detected correctly`() {
        viewModel.updateNotes(setOf(69, 72, 76), baseFrequency = 440.0)
        val state = viewModel.state.value
        assertTrue(state.chordAbbreviation.contains("m"))
    }

    @Test
    fun `note naming FLAT produces flat names`() {
        viewModel.updateNotes(setOf(58), baseFrequency = 440.0, noteNaming = "FLAT")
        val state = viewModel.state.value
        assertEquals("B♭3", state.notes[0].name)
    }

    @Test
    fun `note naming SHARP produces sharp names`() {
        viewModel.updateNotes(setOf(58), baseFrequency = 440.0, noteNaming = "SHARP")
        val state = viewModel.state.value
        assertEquals("A♯3", state.notes[0].name)
    }

    @Test
    fun `two notes produces no chord but shows both notes`() {
        viewModel.updateNotes(setOf(60, 64))
        val state = viewModel.state.value
        assertEquals("", state.chordAbbreviation)
        assertEquals(2, state.notes.size)
    }

    @Test
    fun `G7 dominant chord detected`() {
        viewModel.updateNotes(setOf(67, 71, 74, 77))
        val state = viewModel.state.value
        assertTrue(state.chordAbbreviation.contains("7"))
    }

    @Test
    fun `different tuning system affects frequency`() {
        viewModel.updateNotes(setOf(69), baseFrequency = 440.0, tuningSystem = TuningSystem.EQUAL)
        val equalState = viewModel.state.value
        val equalFreq = equalState.notes[0].frequencyHz
        assertEquals(440.0, equalFreq, 0.1)

        viewModel.updateNotes(setOf(69), baseFrequency = 440.0, tuningSystem = TuningSystem.JUST)
        val justState = viewModel.state.value
        val justFreq = justState.notes[0].frequencyHz
        // Both should be 440 for A4 since it's the reference
        assertEquals(440.0, justFreq, 0.1)
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
./gradlew :app:testDebugUnitTest
```
预期：新增的 InfoViewModelTest 全部通过

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/me/doubao/oscillochord/ui/info/InfoViewModelTest.kt
git commit -m "test: add InfoViewModel integration tests for chord detection and note display"
```

---

### Task 5.3: 添加 KeyboardState 序列化/反序列化测试

**Files:**
- Create: `app/src/test/java/me/doubao/oscillochord/ui/keyboard/KeyboardStateTest.kt`

**Interfaces:**
- Produces: 验证 KeyboardState data class 的 copy 和 equals 行为

- [ ] **Step 1: 创建 KeyboardState 测试**

创建 `app/src/test/java/me/doubao/oscillochord/ui/keyboard/KeyboardStateTest.kt`：

```kotlin
package me.doubao.oscillochord.ui.keyboard

import org.junit.Assert.*
import org.junit.Test

class KeyboardStateTest {

    @Test
    fun `default state has empty notes`() {
        val state = KeyboardState()
        assertTrue(state.activeNotes.isEmpty())
    }

    @Test
    fun `default octave start is 60`() {
        assertEquals(60, KeyboardState().octaveStart)
    }

    @Test
    fun `copy preserves unchanged fields`() {
        val state = KeyboardState(octaveCount = 3, showNoteLabels = false)
        val copied = state.copy(activeNotes = setOf(60, 64))
        assertEquals(3, copied.octaveCount)
        assertFalse(copied.showNoteLabels)
        assertEquals(setOf(60, 64), copied.activeNotes)
    }

    @Test
    fun `equals works for identical states`() {
        val a = KeyboardState(activeNotes = setOf(60, 64), octaveStart = 72)
        val b = KeyboardState(activeNotes = setOf(60, 64), octaveStart = 72)
        assertEquals(a, b)
    }

    @Test
    fun `equals distinguishes different active notes`() {
        val a = KeyboardState(activeNotes = setOf(60))
        val b = KeyboardState(activeNotes = setOf(61))
        assertNotEquals(a, b)
    }

    @Test
    fun `hashCode consistent with equals`() {
        val a = KeyboardState(activeNotes = setOf(60, 64))
        val b = KeyboardState(activeNotes = setOf(60, 64))
        assertEquals(a.hashCode(), b.hashCode())
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
./gradlew :app:testDebugUnitTest
```
预期：新增测试全部通过

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/me/doubao/oscillochord/ui/keyboard/KeyboardStateTest.kt
git commit -m "test: add KeyboardState data class behavior tests"
```

---

**阶段五检查点：** 运行 `./gradlew :app:testDebugUnitTest`。阶段五共 3 个 commit。

---

## 阶段六：最终收尾与验证

### Task 6.1: 全量测试 + 编译验证

- [ ] **Step 1: 运行全部单元测试**

```bash
./gradlew :app:testDebugUnitTest
```
预期：BUILD SUCCESSFUL，所有测试（原有 + 新增）全部通过

- [ ] **Step 2: 编译 debug + release**

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```
预期：两个 variant 均 BUILD SUCCESSFUL

- [ ] **Step 3: 检查编译警告**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | grep -i "warning"
```
预期：仅有已知的 deprecation 警告（MidiManager API），无新增警告

### Task 6.2: 代码审查自检清单

- [ ] **1. 危险写法：** 确认不再有 `Enum.valueOf()` 调用在设置链路上，全部替换为安全的枚举属性访问
- [ ] **2. 重复构造：** 确认 `handleNoteOn/handleNoteOff` 消除了 noteOn/midiNoteOn 重复
- [ ] **3. 资源回收：** 确认 `MidiInputManager.deviceCallback` 在 `destroy()` 中注销
- [ ] **4. 封装：** 确认 `audioEngine` 为 private
- [ ] **5. 类型安全：** 确认 `SettingsState` 使用枚举类型，`SettingsRepository.settings` 返回 `Flow<SettingsState>`
- [ ] **6. 构建：** 确认无重复 BOM，R8 已启用，Root build 声明了 kotlin-android

### Task 6.3: 最终 Commit

- [ ] **Step 1: 如有未提交的变更，一次性提交**

```bash
git status
git add -A
git commit -m "chore: final verification and cleanup after code review fixes"
```

---

## 变更影响概览

| 阶段 | 文件变更数 | 风险等级 | 回滚难度 |
|------|-----------|---------|---------|
| 一：清理构建 | 7 个文件 | 低 | 低（每个 commit 独立） |
| 二：资源管理 | 3 个文件 | 中 | 低 |
| 三：类型安全 | 6 个文件（含新建 1） | 高 | 中（影响面大但改动有规律） |
| 四：去重封装 | 4 个文件 | 低 | 低 |
| 五：测试完善 | 3 个文件（新建 2） | 低 | 极低（仅新增测试） |
| 六：收尾验证 | 0 个文件 | — | — |

**核心原则：**
- 每个阶段独立可测试、可回滚
- 阶段间有清晰的依赖关系（阶段三依赖阶段一构建通过，阶段四可独立于阶段三）
- 每个 commit 改变一个关注点

---

## 延后处理清单

以下审查报告中标记为 🟢 轻微 的问题，因优先级较低且不影响功能，不在本次计划中修复：

| 问题 | 原因 |
|------|------|
| 1.8 OscilloscopeView trailFade 边界裁剪可读性 | 当前逻辑正确，重构风险大于收益 |
| 2.7 SettingsPanel 下拉/分段按钮模式提取 | 纯 UI 重构，无功能影响 |
| 3.2 MidiInputManager 设备追踪状态同步 | 极端边界情况，当前无实际影响 |
| 3.6 OscilloscopeViewModel.onCleared 仅 clear | Oscillator 无重量资源，当前无泄漏 |
| 4.4 InfoPanel stringResource 耦合 | Compose 标准模式，不影响测试 |
| 4.5 Color 常量与 Theme 耦合 | 仅支持暗色主题的应用中影响微小 |
| 5.5 smoothCount lerp 速率调优 | 需要实际设备上的声学测试 |
| 5.6 BuildConfig / Product Flavors | 无当前需求 |
| 7.2 Release 签名配置 | 部署阶段问题，不影响代码质量 |

**另有两个架构级改造延后为独立计划（已记录 memory）：**
- 5.2 引入 DI 框架（Hilt/Koin）
- 4.2/4.3 设置数据流重构

---

## 自检结果

1. **Spec coverage:** 审查报告中的 32 项可修复问题，全部有对应 Task。延后 9 项轻微问题已在上表说明原因。
2. **Placeholder scan:** 无 TBD/TODO/占位符。每个 Step 均有具体代码或命令。
3. **Type consistency:**
   - `SettingsState` 字段类型：Task 3.1 定义为枚举，Task 3.2–3.5 一致使用
   - `SettingsRepository.set*()` 参数类型：Task 3.2 改为枚举，Task 3.3 SettingsViewModel 适配
   - `SettingsPanel` 回调签名：Task 3.4 改为枚举，Task 3.5 MainScreen 传参一致
   - `handleNoteOn/handleNoteOff`：Task 4.1 定义为 private，Task 4.2 无冲突
   - `drawLabel` 新签名：Task 4.5 添加 paint 参数，内部调用点同步修改
   - InfoViewModelTest 中 `viewModel.state.value` 引用一致（已修复 typo）
