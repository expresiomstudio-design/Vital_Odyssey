package com.moises.vitalodyssey.di

import androidx.room.Room
import com.moises.vitalodyssey.data.local.AppDatabase
import com.moises.vitalodyssey.data.local.UserPreferencesManager
import com.moises.vitalodyssey.domain.usecase.CalculateBattleTurnUseCase
import com.moises.vitalodyssey.domain.usecase.CalculatePlayerStatsUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {

    // 1. DataStore (Preferencias del Usuario) - Usamos 'single' para que solo exista uno en toda la app
    single { UserPreferencesManager(androidContext()) }

    // 2. Base de Datos Local (Room) - También debe ser 'single'
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "vital_odyssey_db"
        ).build()
    }

    // 3. DAOs (Para acceder a las tablas)
    single { get<AppDatabase>().habitDao() }

    // 4. Casos de Uso (Lógica de negocio) - Usamos 'factory' para crear una instancia nueva cada vez que se necesiten
    factory { CalculatePlayerStatsUseCase() }
    factory { CalculateBattleTurnUseCase() }
    factory { CalculateBossStatsUseCase() }
    factory { ProcessBattleResultUseCase(get()) }
}