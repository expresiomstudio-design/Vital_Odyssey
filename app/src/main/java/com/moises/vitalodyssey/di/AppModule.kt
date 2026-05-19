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
import com.moises.vitalodyssey.domain.repository.HealthRepository
import com.moises.vitalodyssey.domain.repository.UserRepository
import com.moises.vitalodyssey.domain.usecase.*
import com.moises.vitalodyssey.domain.usecase.apprules.CalculateFocoArcanoUseCase
import com.moises.vitalodyssey.domain.usecase.apprules.DeleteAppRuleUseCase
import com.moises.vitalodyssey.domain.usecase.apprules.GetAppRuleByIdUseCase
import com.moises.vitalodyssey.domain.usecase.apprules.GetAppRulesUseCase
import com.moises.vitalodyssey.domain.usecase.apprules.GetTrackedAppsUsageUseCase
import com.moises.vitalodyssey.domain.usecase.apprules.SaveAppRuleUseCase
import com.moises.vitalodyssey.domain.usecase.health.CalculateDefenseMultiplierUseCase
import com.moises.vitalodyssey.domain.usecase.health.GetYesterdayHealthStatsUseCase
import com.moises.vitalodyssey.presentation.viewmodels.*
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // 0. Firebase & Remote
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get(), get(), androidContext(), get()) }
    single<AppUsageRepository> { AppUsageRepositoryImpl(androidContext()) }
    single<HealthRepository> { com.moises.vitalodyssey.data.device.HealthRepositoryImpl(androidContext()) }

    // 1. DataStore
    single { UserPreferencesManager(androidContext()) }

    // 2. Base de Datos Local
    single { com.moises.vitalodyssey.data.local.DatabaseManager(androidContext(), get()) }

    single { get<com.moises.vitalodyssey.data.local.DatabaseManager>().getDatabaseSync() }

    // 3. DAOs
    factory { get<com.moises.vitalodyssey.data.local.DatabaseManager>().getDatabaseSync().habitDao() }
    factory { get<com.moises.vitalodyssey.data.local.DatabaseManager>().getDatabaseSync().userDao() }
    factory { get<com.moises.vitalodyssey.data.local.DatabaseManager>().getDatabaseSync().appRuleDao() }
    factory { get<com.moises.vitalodyssey.data.local.DatabaseManager>().getDatabaseSync().bossDao() }

    // 4. Casos de Uso
    factory { CalculatePlayerStatsUseCase() }
    factory { CalculateBattleTurnUseCase(get<CalculateDefenseMultiplierUseCase>()) }
    factory { CalculateBossStatsUseCase() }
    factory { CheckAndSeedInitialBossUseCase(get(), get()) }
    factory { ProcessBattleResultUseCase(get(), get(), get()) }
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

    // Casos de Uso - Salud
    factory { GetYesterdayHealthStatsUseCase(get(), get()) }
    factory { CalculateDefenseMultiplierUseCase(get(), get()) }

    // 5. ViewModels
    viewModel {
        DashboardViewModel(
            userRepository = get(),
            calculateStats = get(),
            calculateBossStats = get(),
            calculateBattleTurn = get(),
            processBattleResult = get(),
            calculateFocoArcano = get(),
            calculateDefenseMultiplierUseCase = get(),
            checkAndSeedInitialBossUseCase = get(),
            bossDao = get(),
            habitDao = get(),
            userPrefsManager = get()
        )
    }
    
    viewModel {
        ProfileViewModel(
            userRepository = get(),
            calculateStats = get(),
            authRepository = get(),
            userPrefsManager = get()
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
            userRepository = get(),
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

    viewModel {
        HealthViewModel(
            healthRepository = get(),
            userPreferencesManager = get(),
            userRepository = get(),
            getYesterdayHealthStats = get(),
            calculateDefenseMultiplier = get()
        )
    }
}
