package com.maksimowiczm.foodyou.app.infrastructure.android

import android.os.Bundle
import com.maksimowiczm.foodyou.app.ui.healthconnect.HealthConnectSettingsScreen
import com.maksimowiczm.foodyou.app.ui.theme.FoodYouTheme

class HealthConnectActivity : FoodYouAbstractActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FoodYouTheme {
                HealthConnectSettingsScreen(onBack = { finish() })
            }
        }
    }
}
