package com.moises.vitalodyssey.di

import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.moises.vitalodyssey.data.device.AppUsageRepositoryImpl
import com.moises.vitalodyssey.data.local.AppDatabase
import com.moises.vitalodyssey.data.local.UserPreferencesManager
import com.moises.vitalodyssey.data.remote.AuthRepositoryImpl
import com.moises.vitalodyssey.data.remote.UserRepositoryImpl
import com.moises.vitalodyssey.domain.repository.AppUsageRepository
import com.moises.vitalodyssey.domain.repository.AuthRepository
import com.moises.vitalodyssey.domain.repository.UserRepository
import com.moises.vitalodyssey.domain.usecase.*
import com.moises.vitalodyssey.domain.usecase.apprules.CalculateFocoArcanoUseCase
import com.moises.vitalodyssey.domain.usecase.apprules.DeleteAppRuleUseCase
import com.moises.vitalodyssey.domain.usecase.apprules.GetAppRuleByIdUseCase
import com.moises.vitalodyssey.domain.usecase.apprules.GetAppRulesUseCase
import com.moises.vitalodyssey.domain.usecase.apprules.GetTrackedAppsUsageUseCase
import com.moises.vitalodyssey.domain.usecase.apprules.SaveAppRuleUseCase
import com.moises.vitalodyssey.presentation.viewmodels.*
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // 0. Firebase & Remote
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get(), get(), get(), get()) }
    single<AppUsageRepository> { AppUsageRepositoryImpl(androidContext()) }

    // 1. DataStore
    single { UserPreferencesManager(androidContext()) }

    // 2. Base de Datos Local
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "vital_odyssey_db"
        )
            .addMigrations(
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
            .fallbackToDestructiveMigration()
            .build()
    }

    // 3. DAOs
    single { get<AppDatabase>().habitDao() }
    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().appRuleDao() }

    // 4. Casos de Uso
    factory { CalculatePlayerStatsUseCase() }
    factory { CalculateBattleTurnUseCase() }
    factory { CalculateBossStatsUseCase() }
    factory { ProcessBattleResultUseCase(get()) }
    factory { CalculateHabitScoreUseCase() }
    factory { EvaluateHabitStateUseCase() }
    factory { EvaluateStrictStateUseCase() }
    factory { RecalculateHabitScoresUseCase(get(), get(), get(), get()) }
    factory { RecordHabitLogUseCase(get(), get(), get()) }
    
    // Casos de Uso - Foco Arcano
    factory { GetAppRulesUseCase(get()) }
    factory { SaveAppRuleUseCase(get()) }
    factory { DeleteAppRuleUseCase(get()) }
    factory { GetTrackedAppsUsageUseCase(get(), get(), get()) }
    factory { GetAppRuleByIdUseCase(get()) }
    factory { CalculateFocoArcanoUseCase(get()) }

    // 5. ViewModels
    viewModel {
        DashboardViewModel(
            userRepository = get(),
            calculateStats = get(),
            calculateBossStats = get(),
            calculateBattleTurn = get(),
            processBattleResult = get(),
            calculateFocoArcano = get()
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
            habitDao = get(),
            evaluateStateUseCase = get(),
            recordHabitLogUseCase = get()
        )
    }

    viewModel {
        HabitFormViewModel(
            habitDao = get(),
            userRepository = get(),
            recalculateHabitScoresUseCase = get()
        )
    }

    viewModel { (habitId: Int) ->
        HabitTrackingViewModel(
            habitId = habitId,
            habitDao = get(),
            userRepository = get(),
            evaluateStrictStateUseCase = get(),
            recordHabitLogUseCase = get()
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

    viewModel {
        AppRuleFormViewModel(
            saveAppRuleUseCase = get(),
            deleteAppRuleUseCase = get(),
            getAppRuleByIdUseCase = get(),
            appUsageRepository = get(),
            userRepository = get(),
            getAppRulesUseCase = get()
        )
    }

    viewModel {
        AppRulesViewModel(
            getTrackedAppsUsageUseCase = get(),
            appRuleDao = get(),
            saveAppRuleUseCase = get(),
            appUsageRepository = get()
        )
    }
}
