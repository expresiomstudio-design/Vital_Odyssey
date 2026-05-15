package com.moises.vitalodyssey

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.moises.vitalodyssey.di.appModule
import com.moises.vitalodyssey.worker.DailyCombatWorker
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.Calendar
import java.util.concurrent.TimeUnit

class VitalOdysseyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@VitalOdysseyApp)
            modules(appModule)
        }
        
        setupDailyCombatWorker()
    }

    private fun setupDailyCombatWorker() {
        // Calcular el tiempo hasta la próxima medianoche
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
        }
        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }
        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyCombatWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyCombatWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }
}
