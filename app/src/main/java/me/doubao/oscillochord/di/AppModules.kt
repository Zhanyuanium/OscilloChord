package me.doubao.oscillochord.di

import me.doubao.oscillochord.data.SettingsRepository
import me.doubao.oscillochord.domain.audio.AudioEngine
import me.doubao.oscillochord.domain.chord.ChordDetector
import me.doubao.oscillochord.domain.lissajous.LissajousProjector
import me.doubao.oscillochord.ui.info.InfoViewModel
import me.doubao.oscillochord.ui.keyboard.KeyboardViewModel
import me.doubao.oscillochord.ui.oscilloscope.OscilloscopeViewModel
import me.doubao.oscillochord.ui.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val domainModule = module {
    single { AudioEngine() }
    factory { ChordDetector() }
    factory { LissajousProjector() }
}

val dataModule = module {
    single { SettingsRepository(androidContext()) }
}

val viewModelModule = module {
    viewModel { KeyboardViewModel(get()) }
    viewModel { OscilloscopeViewModel(get()) }
    viewModel { InfoViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
}
