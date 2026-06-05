package com.maksimowiczm.foodyou.app.ui.healthconnect

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SyncDisabled
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maksimowiczm.foodyou.app.ui.common.component.ArrowBackIconButton
import com.maksimowiczm.foodyou.healthconnect.HealthConnectManager
import foodyou.app.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HealthConnectSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HealthConnectViewModel = koinViewModel()

    if (!viewModel.isAvailable) {
        UnavailableScreen(onBack = onBack, modifier = modifier)
    } else {
        AvailableScreen(viewModel = viewModel, onBack = onBack, modifier = modifier)
    }
}

@Composable
private fun UnavailableScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.headline_health_connect)) },
                navigationIcon = { ArrowBackIconButton(onBack) },
            )
        },
    ) { paddingValues ->
        ListItem(
            modifier = Modifier.padding(paddingValues),
            headlineContent = {
                Text(stringResource(Res.string.headline_health_connect_unavailable))
            },
            supportingContent = {
                Text(stringResource(Res.string.description_health_connect_unavailable))
            },
            leadingContent = { Icon(Icons.Outlined.FavoriteBorder, contentDescription = null) },
        )
    }
}

@Composable
private fun AvailableScreen(
    viewModel: HealthConnectViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val syncEnabled by viewModel.syncEnabled.collectAsStateWithLifecycle()
    val hasPermissions by viewModel.hasPermissions.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.refreshPermissions()
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.headline_health_connect)) },
                navigationIcon = { ArrowBackIconButton(onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
        ) {
            item {
                ListItem(
                    headlineContent = {
                        Text(stringResource(Res.string.headline_sync_enabled))
                    },
                    supportingContent = {
                        Text(
                            stringResource(
                                if (syncEnabled) Res.string.description_sync_enabled
                                else Res.string.description_sync_disabled
                            )
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = if (syncEnabled) Icons.Outlined.Sync
                            else Icons.Outlined.SyncDisabled,
                            contentDescription = null,
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = syncEnabled,
                            onCheckedChange = { viewModel.setSyncEnabled(it) },
                            enabled = hasPermissions,
                        )
                    },
                )
            }

            if (!hasPermissions) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = stringResource(
                                Res.string.description_health_connect_permissions_required
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = {
                                permissionsLauncher.launch(HealthConnectManager.PERMISSIONS)
                            },
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            Text(stringResource(Res.string.action_grant_permissions))
                        }
                    }
                }
            }

            if (hasPermissions && syncEnabled) {
                item {
                    ListItem(
                        headlineContent = {
                            Text(
                                if (isSyncing) stringResource(Res.string.headline_syncing)
                                else stringResource(Res.string.action_sync_now)
                            )
                        },
                        trailingContent = {
                            if (isSyncing) {
                                CircularProgressIndicator()
                            } else {
                                Button(onClick = viewModel::syncNow) {
                                    Text(stringResource(Res.string.action_sync_now))
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
