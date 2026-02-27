package com.moises.vitalodyssey.di

import androidx.room.Room
import com.moises.vitalodyssey.data.local.AppDatabase
import com.moises.vitalodyssey.data.local.UserPreferencesManager
import com.moises.vitalodyssey.domain.usecase.CalculateBattleTurnUseCase
import com.moises.vitalodyssey.domain.usecase.CalculateBossStatsUseCase
import com.moises.vitalodyssey.domain.usecase.CalculatePlayerStatsUseCase
import com.moises.vitalodyssey.domain.usecase.ProcessBattleResultUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {

    // 1. DataStore (Preferencias del Usuario) - 'single' para que solo exista uno
    single { UserPreferencesManager(androidContext()) }

    // 2. Base de Datos Local (Room)
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "vital_odyssey_db"
        ).build()
    }

    // 3. DAOs (Para acceder a las tablas)
    single { get<AppDatabase>().habitDao() }

    // 4. Casos de Uso (Lógica de negocio RPG) - 'factory' crea una instancia nueva cada vez
    factory { CalculatePlayerStatsUseCase() }
    factory { CalculateBattleTurnUseCase() }
    factory { CalculateBossStatsUseCase() }

    // El orquestador necesita el CalculatePlayerStatsUseCase, Koin lo inyecta solo usando 'get()'
    factory { ProcessBattleResultUseCase(get()) }
}