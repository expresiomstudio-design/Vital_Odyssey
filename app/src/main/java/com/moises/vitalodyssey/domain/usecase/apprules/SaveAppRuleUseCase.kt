package com.moises.vitalodyssey.domain.usecase.apprules

import com.moises.vitalodyssey.data.local.AppRuleDao
import com.moises.vitalodyssey.data.local.AppRuleEntity
import com.moises.vitalodyssey.domain.model.AppRule

class SaveAppRuleUseCase(private val appRuleDao: AppRuleDao) {
    suspend operator fun invoke(rule: AppRule) {
        val entity = AppRuleEntity.fromDomain(rule)
        if (rule.id == 0) {
            appRuleDao.insertRule(entity)
        } else {
            appRuleDao.updateRule(entity)
        }
    }
}
