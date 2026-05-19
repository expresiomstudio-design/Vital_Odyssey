package com.moises.vitalodyssey

import android.app.Application
import com.moises.vitalodyssey.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VitalOdysseyApp : Application(), KoinComponent {

    private val databaseManager: com.moises.vitalodyssey.data.local.DatabaseManager by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@VitalOdysseyApp)
            modules(appModule)
        }
        
        setupDailyCombatWorker()
    }

    private fun setupDailyCombatWorker() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = databaseManager.getDatabaseSync()
                val user = db.userDao().getFirstUserProfileOnce()
                val cutoff = user?.cutoffTime ?: "00:00"
                com.moises.vitalodyssey.data.remote.UserRepositoryImpl.scheduleDailyCombatWorker(this@VitalOdysseyApp, cutoff)
            } catch (e: Exception) {
                android.util.Log.e("VitalOdysseyApp", "Error setting up DailyCombatWorker: ${e.message}", e)
            }
        }
    }
}
