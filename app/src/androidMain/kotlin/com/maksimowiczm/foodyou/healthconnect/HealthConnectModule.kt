package com.maksimowiczm.foodyou.healthconnect

import com.maksimowiczm.foodyou.app.ui.healthconnect.HealthConnectViewModel
import com.maksimowiczm.foodyou.common.infrastructure.koin.applicationCoroutineScope
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val healthConnectModule = module {
    single { HealthConnectManager(androidContext()) }

    single(createdAtStart = true) {
        HealthConnectSyncService(
            manager = get(),
            observeDiaryMealsUseCase = get(),
            dateProvider = get(),
            dataStore = get(),
            coroutineScope = applicationCoroutineScope(),
        )
    }

    viewModelOf(::HealthConnectViewModel)
}