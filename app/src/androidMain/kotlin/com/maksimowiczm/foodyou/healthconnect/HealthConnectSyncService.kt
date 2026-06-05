package com.maksimowiczm.foodyou.healthconnect

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.maksimowiczm.foodyou.common.domain.date.DateProvider
import com.maksimowiczm.foodyou.fooddiary.domain.usecase.ObserveDiaryMealsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HealthConnectSyncService(
    private val manager: HealthConnectManager,
    private val observeDiaryMealsUseCase: ObserveDiaryMealsUseCase,
    private val dateProvider: DateProvider,
    private val dataStore: DataStore<Preferences>,
    coroutineScope: CoroutineScope,
) {
    companion object {
        val SYNC_ENABLED_KEY = booleanPreferencesKey("health_connect:sync_enabled")
    }

    val syncEnabled = dataStore.data.map { it[SYNC_ENABLED_KEY] ?: false }

    init {
        coroutineScope.launch { observeAndSync() }
    }

    private suspend fun observeAndSync() {
        val todayFlow = dateProvider.observeDate()

        combine(todayFlow, syncEnabled) { date, enabled -> date to enabled }
            .collectLatest { (date, enabled) ->
                if (!enabled || !manager.isAvailable) return@collectLatest

                observeDiaryMealsUseCase.observe(date).collectLatest { diaryMeals ->
                    if (manager.hasPermissions()) {
                        manager.syncDiaryForDate(date, diaryMeals)
                    }
                }
            }
    }

    suspend fun syncNow() {
        if (!manager.isAvailable || !manager.hasPermissions()) return
        val date = dateProvider.observeDate().first()
        val diaryMeals = observeDiaryMealsUseCase.observe(date).first()
        manager.syncDiaryForDate(date, diaryMeals)
    }
}