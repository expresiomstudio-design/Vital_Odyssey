package com.moises.vitalodyssey.domain.usecase.apprules

import com.moises.vitalodyssey.data.local.AppRuleDao
import com.moises.vitalodyssey.data.local.AppRuleEntity
import com.moises.vitalodyssey.domain.model.AppRule

class DeleteAppRuleUseCase(private val appRuleDao: AppRuleDao) {
    suspend operator fun invoke(rule: AppRule) {
        appRuleDao.deleteRule(AppRuleEntity.fromDomain(rule))
    }
}
