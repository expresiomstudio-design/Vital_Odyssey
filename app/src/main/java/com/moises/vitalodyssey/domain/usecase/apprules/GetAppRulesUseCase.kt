package com.moises.vitalodyssey.domain.usecase.apprules

import com.moises.vitalodyssey.data.local.AppRuleDao
import com.moises.vitalodyssey.domain.model.AppRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetAppRulesUseCase(private val appRuleDao: AppRuleDao) {
    operator fun invoke(): Flow<List<AppRule>> {
        return appRuleDao.getAllRules().map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
