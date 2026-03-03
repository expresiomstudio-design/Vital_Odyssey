package com.moises.vitalodyssey.di

import androidx.room.Room
import com.moises.vitalodyssey.data.local.AppDatabase
import com.moises.vitalodyssey.data.local.UserPreferencesManager
import com.moises.vitalodyssey.domain.usecase.CalculateBattleTurnUseCase
import com.moises.vitalodyssey.domain.usecase.CalculateBossStatsUseCase
import com.moises.vitalodyssey.domain.usecase.CalculatePlayerStatsUseCase
import com.moises.vitalodyssey.domain.usecase.ProcessBattleResultUseCase
import com.moises.vitalodyssey.presentation.viewmodels.DashboardViewModel // Importación necesaria
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel // Importación necesaria
import org.koin.dsl.module

val appModule = module {

    // 1. DataStore
    single { UserPreferencesManager(androidContext()) }

    // 2. Base de Datos Local
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "vital_odyssey_db"
        ).build()
    }

    // 3. DAOs
    single { get<AppDatabase>().habitDao() }

    // 4. Casos de Uso
    factory { CalculatePlayerStatsUseCase() }
    factory { CalculateBattleTurnUseCase() }
    factory { CalculateBossStatsUseCase() }
    factory { ProcessBattleResultUseCase(get()) }

    // 5. ViewModels - ¡Aquí está la corrección clave!
    viewModel {
        DashboardViewModel(
            userPrefs = get(),
            calculateStats = get(),
            calculateBossStats = get(),
            calculateBattleTurn = get(),
            processBattleResult = get()
        )
    }
}