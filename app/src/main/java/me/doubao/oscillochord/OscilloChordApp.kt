package me.doubao.oscillochord

import android.app.Application
import me.doubao.oscillochord.di.dataModule
import me.doubao.oscillochord.di.domainModule
import me.doubao.oscillochord.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class OscilloChordApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@OscilloChordApp)
            modules(domainModule, dataModule, viewModelModule)
        }
    }
}
