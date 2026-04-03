package com.moises.vitalodyssey.di

import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.moises.vitalodyssey.data.local.AppDatabase
import com.moises.vitalodyssey.data.local.UserPreferencesManager
import com.moises.vitalodyssey.data.remote.AuthRepositoryImpl
import com.moises.vitalodyssey.data.remote.UserRepositoryImpl
import com.moises.vitalodyssey.domain.repository.AuthRepository
import com.moises.vitalodyssey.domain.repository.UserRepository
import com.moises.vitalodyssey.domain.usecase.CalculateBattleTurnUseCase
import com.moises.vitalodyssey.domain.usecase.CalculateBossStatsUseCase
import com.moises.vitalodyssey.domain.usecase.CalculatePlayerStatsUseCase
import com.moises.vitalodyssey.domain.usecase.ProcessBattleResultUseCase
import com.moises.vitalodyssey.presentation.viewmodels.DashboardViewModel
import com.moises.vitalodyssey.presentation.viewmodels.ProfileViewModel
import com.moises.vitalodyssey.presentation.viewmodels.HabitsViewModel
import com.moises.vitalodyssey.presentation.viewmodels.AuthViewModel
import com.moises.vitalodyssey.presentation.viewmodels.OnboardingViewModel
import com.moises.vitalodyssey.presentation.viewmodels.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // 0. Firebase & Remote
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get(), get(), get(), get()) }

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
    single { get<AppDatabase>().userDao() }

    // 4. Casos de Uso
    factory { CalculatePlayerStatsUseCase() }
    factory { CalculateBattleTurnUseCase() }
    factory { CalculateBossStatsUseCase() }
    factory { ProcessBattleResultUseCase(get()) }

    // 5. ViewModels
    viewModel {
        DashboardViewModel(
            userRepository = get(),
            calculateStats = get(),
            calculateBossStats = get(),
            calculateBattleTurn = get(),
            processBattleResult = get()
        )
    }
    
    viewModel {
        ProfileViewModel(
            userRepository = get(),
            calculateStats = get(),
            authRepository = get()
        )
    }

    viewModel {
        HabitsViewModel(
            habitDao = get()
        )
    }

    viewModel {
        AuthViewModel(
            authRepository = get(),
            userPrefs = get()
        )
    }

    viewModel {
        OnboardingViewModel(
            userRepository = get()
        )
    }

    viewModel {
        MainViewModel(
            auth = get(),
            userRepository = get()
        )
    }
}
