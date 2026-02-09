package com.moises.vitalodyssey

import android.app.Application
import com.moises.vitalodyssey.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class VitalOdysseyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // ¡Encendemos Koin!
        startKoin {
            androidContext(this@VitalOdysseyApp)
            modules(appModule)
        }
    }
}
