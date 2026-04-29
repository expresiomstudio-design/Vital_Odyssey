package com.moises.vitalodyssey.domain.usecase.apprules

import com.moises.vitalodyssey.data.local.AppRuleDao
import com.moises.vitalodyssey.domain.model.AppRule

class GetAppRuleByIdUseCase(private val appRuleDao: AppRuleDao) {
    suspend operator fun invoke(id: Int): AppRule? {
        return appRuleDao.getRuleById(id)?.toDomain()
    }
}
