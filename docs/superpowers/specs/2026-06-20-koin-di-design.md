# Koin 依赖注入引入 — 设计文档

## 目标

为 OscilloChord 引入 Koin 依赖注入框架，解除 ViewModel 对具体实现的内部 new 耦合，使依赖可注入、可替换，为后续设置数据流重构奠定基础。

## 技术选型：Koin 4.1.0

理由：小型项目（35 KT 文件 / 4 ViewModel），无需编译时注解处理。Koin 构建零开销、纯 Kotlin DSL、概念简单。

## 模块划分

### domainModule — 领域依赖

| 类 | 作用域 | 原因 |
|---|---|---|
| `AudioEngine` | `single` | 全局唯一，所有键盘/MIDI 输入共享 |
| `ChordDetector` | `factory` | 无状态，每次调用创建新实例 |
| `LissajousProjector` | `factory` | 无状态，每次调用创建新实例 |

### dataModule — 数据依赖

| 类 | 作用域 | 原因 |
|---|---|---|
| `SettingsRepository` | `single` | 全局唯一 DataStore 访问，需要 `androidContext()` |

### viewModelModule — ViewModel 注册

```kotlin
viewModel { KeyboardViewModel(get()) }
viewModel { OscilloscopeViewModel(get()) }
viewModel { InfoViewModel(get()) }
viewModel { SettingsViewModel(get()) }
```

## 文件变更清单

| 操作 | 文件 | 说明 |
|------|------|------|
| 新建 | `app/src/main/java/me/doubao/oscillochord/di/AppModules.kt` | Koin 模块定义 |
| 修改 | `app/src/main/java/me/doubao/oscillochord/OscilloChordApp.kt` | 重建 Application 类，启动 Koin |
| 修改 | `app/src/main/AndroidManifest.xml` | 加回 `android:name=".OscilloChordApp"` |
| 修改 | `app/src/main/java/me/doubao/oscillochord/MainActivity.kt` | ViewModel 获取改用 `koinViewModel()`（via MainScreen） |
| 修改 | `app/src/main/java/me/doubao/oscillochord/ui/screen/MainScreen.kt` | `viewModel()` → `koinViewModel()` |
| 修改 | `app/src/main/java/me/doubao/oscillochord/ui/keyboard/KeyboardViewModel.kt` | 构造函数注入 AudioEngine |
| 修改 | `app/src/main/java/me/doubao/oscillochord/ui/oscilloscope/OscilloscopeViewModel.kt` | 构造函数注入 LissajousProjector |
| 修改 | `app/src/main/java/me/doubao/oscillochord/ui/info/InfoViewModel.kt` | 构造函数注入 ChordDetector |
| 修改 | `app/src/main/java/me/doubao/oscillochord/ui/settings/SettingsViewModel.kt` | 构造函数注入 SettingsRepository，降为 ViewModel |
| 修改 | `gradle/libs.versions.toml` | 添加 koin 版本和库声明 |
| 修改 | `app/build.gradle.kts` | 添加 koin-android、koin-androidx-compose 依赖 |

## ViewModel 构造函数改造

```kotlin
// KeyboardViewModel: 注入 AudioEngine
class KeyboardViewModel(
    private val audioEngine: AudioEngine
) : ViewModel()

// OscilloscopeViewModel: 注入 LissajousProjector
class OscilloscopeViewModel(
    private val projector: LissajousProjector
) : ViewModel()

// InfoViewModel: 注入 ChordDetector
class InfoViewModel(
    private val detector: ChordDetector
) : ViewModel()

// SettingsViewModel: 注入 SettingsRepository，由 AndroidViewModel 降为 ViewModel
class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel()
```

## Koin 初始化

```kotlin
// OscilloChordApp.kt
class OscilloChordApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@OscilloChordApp)
            modules(domainModule, dataModule, viewModelModule)
        }
    }
}
```

## Compose 集成

`MainScreen.kt` 中 ViewModel 获取方式变更：
```kotlin
// 改造前
import androidx.lifecycle.viewmodel.compose.viewModel
fun MainScreen(keyboardVM: KeyboardViewModel = viewModel(), ...)

// 改造后
import org.koin.androidx.compose.koinViewModel
fun MainScreen(keyboardVM: KeyboardViewModel = koinViewModel(), ...)
```

## 测试影响

- 单元测试可以传入 mock 依赖构造 ViewModel，无需真实 AudioTrack 或 DataStore
- `AudioEngineTest` 目前在 JVM 上受限（AudioTrack 需要 Android Framework），引入 DI 后可以 mock AudioEngine 而非创建真实实例
- ViewModel 测试可以使用 fake 依赖（如 `ChordDetector` 已是纯逻辑，无需 mock）

## 不变项

- 所有 ViewModel 公共 API 不变
- Compose UI 代码（PianoKeyboard、OscilloscopeView、InfoPanel、SettingsPanel）不变
- Domain 层代码（AudioEngine、Oscillator、ChordDetector 等）本身不变，仅注册方式改变
- SettingsRepository 接口不变
- 现有测试全部保持通过

## 设计决策记录

1. **AudioEngine 用 single 而非 factory**：全局唯一的音频输出通道，多实例会导致 AudioTrack 竞争
2. **SettingsRepository 用 single**：DataStore 全局唯一，singleton 避免文件锁冲突
3. **ChordDetector / LissajousProjector 用 factory**：无状态纯计算类，每次新建零成本且线程安全
4. **SettingsViewModel 从 AndroidViewModel 降为 ViewModel**：依赖由 Koin 注入后不再需要 Application 参数
5. **不引入 ViewModelFactory**：Koin 的 `viewModel { }` DSL 内部处理了 `ViewModelProvider.Factory`，无需手写
