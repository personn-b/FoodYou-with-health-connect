package com.maksimowiczm.foodyou.app.ui.healthconnect

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maksimowiczm.foodyou.healthconnect.HealthConnectManager
import com.maksimowiczm.foodyou.healthconnect.HealthConnectSyncService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HealthConnectViewModel(
    private val manager: HealthConnectManager,
    private val syncService: HealthConnectSyncService,
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    val isAvailable = manager.isAvailable

    val syncEnabled = syncService.syncEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    private val _hasPermissions = MutableStateFlow(false)
    val hasPermissions = _hasPermissions.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    init {
        viewModelScope.launch { _hasPermissions.value = manager.hasPermissions() }
    }

    fun refreshPermissions(grantedPermissions: Set<String>? = null) {
        if (grantedPermissions != null) {
            _hasPermissions.value = grantedPermissions.containsAll(HealthConnectManager.PERMISSIONS)
        } else {
            viewModelScope.launch { _hasPermissions.value = manager.hasPermissions() }
        }
    }

    fun setSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[HealthConnectSyncService.SYNC_ENABLED_KEY] = enabled }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            syncService.syncNow()
            _isSyncing.value = false
        }
    }
}