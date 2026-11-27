package com.amneziawg.fork.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amneziawg.fork.R
import com.amneziawg.fork.ui.theme.AmneziaTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AmneziaTheme {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBack() }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = androidx.compose.foundation.layout.Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // VPN Settings
            SettingsSection(title = stringResource(R.string.vpn_settings)) {
                SettingItem(
                    title = stringResource(R.string.auto_reconnect),
                    subtitle = stringResource(R.string.auto_reconnect_desc),
                    icon = androidx.compose.material.icons.Icons.Default.Sync,
                    trailing = {
                        Switch(
                            checked = uiState.autoReconnect,
                            onCheckedChange = { viewModel.onAutoReconnectChange(it) }
                        )
                    }
                )

                SettingItem(
                    title = stringResource(R.string.block_non_vpn_traffic),
                    subtitle = stringResource(R.string.block_non_vpn_traffic_desc),
                    icon = androidx.compose.material.icons.Icons.Default.Block,
                    trailing = {
                        Switch(
                            checked = uiState.blockNonVpnTraffic,
                            onCheckedChange = { viewModel.onBlockNonVpnTrafficChange(it) }
                        )
                    }
                )

                SettingItem(
                    title = stringResource(R.string.always_on_vpn),
                    subtitle = stringResource(R.string.always_on_vpn_desc),
                    icon = androidx.compose.material.icons.Icons.Default.VpnLock,
                    trailing = {
                        Switch(
                            checked = uiState.alwaysOnVpn,
                            onCheckedChange = { viewModel.onAlwaysOnVpnChange(it) }
                        )
                    }
                )
            }

            // Routing Settings
            SettingsSection(title = stringResource(R.string.routing_settings)) {
                SettingItem(
                    title = stringResource(R.string.per_app_routing),
                    subtitle = stringResource(R.string.per_app_routing_desc),
                    icon = androidx.compose.material.icons.Icons.Default.Apps,
                    trailing = {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
                            contentDescription = null
                        )
                    },
                    onClick = { viewModel.navigateToAppRouting() }
                )

                SettingItem(
                    title = stringResource(R.string.per_domain_routing),
                    subtitle = stringResource(R.string.per_domain_routing_desc),
                    icon = androidx.compose.material.icons.Icons.Default.Language,
                    trailing = {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
                            contentDescription = null
                        )
                    },
                    onClick = { viewModel.navigateToDomainRouting() }
                )

                SettingItem(
                    title = stringResource(R.string.dns_settings),
                    subtitle = stringResource(R.string.dns_settings_desc),
                    icon = androidx.compose.material.icons.Icons.Default.Dns,
                    trailing = {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
                            contentDescription = null
                        )
                    },
                    onClick = { viewModel.navigateToDnsSettings() }
                )
            }

            // Security Settings
            SettingsSection(title = stringResource(R.string.security_settings)) {
                SettingItem(
                    title = stringResource(R.string.biometric_lock),
                    subtitle = stringResource(R.string.biometric_lock_desc),
                    icon = androidx.compose.material.icons.Icons.Default.Fingerprint,
                    trailing = {
                        Switch(
                            checked = uiState.biometricLock,
                            onCheckedChange = { viewModel.onBiometricLockChange(it) }
                        )
                    }
                )

                SettingItem(
                    title = stringResource(R.string.lock_on_background),
                    subtitle = stringResource(R.string.lock_on_background_desc),
                    icon = androidx.compose.material.icons.Icons.Default.Lock,
                    trailing = {
                        Switch(
                            checked = uiState.lockOnBackground,
                            onCheckedChange = { viewModel.onLockOnBackgroundChange(it) }
                        )
                    }
                )
            }

            // Advanced Settings
            SettingsSection(title = stringResource(R.string.advanced_settings)) {
                SettingItem(
                    title = stringResource(R.string.logging_level),
                    subtitle = stringResource(R.string.logging_level_desc),
                    icon = androidx.compose.material.icons.Icons.Default.BugReport,
                    trailing = {
                        Text(text = uiState.logLevel, fontSize = 14.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
                            contentDescription = null
                        )
                    },
                    onClick = { viewModel.showLogLevelPicker() }
                )

                SettingItem(
                    title = stringResource(R.string.export_configs),
                    subtitle = stringResource(R.string.export_configs_desc),
                    icon = androidx.compose.material.icons.Icons.Default.FileDownload,
                    trailing = {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
                            contentDescription = null
                        )
                    },
                    onClick = { viewModel.exportConfigs() }
                )

                SettingItem(
                    title = stringResource(R.string.import_configs),
                    subtitle = stringResource(R.string.import_configs_desc),
                    icon = androidx.compose.material.icons.Icons.Default.FileUpload,
                    trailing = {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
                            contentDescription = null
                        )
                    },
                    onClick = { viewModel.importConfigs() }
                )

                SettingItem(
                    title = stringResource(R.string.clear_logs),
                    subtitle = stringResource(R.string.clear_logs_desc),
                    icon = androidx.compose.material.icons.Icons.Default.Delete,
                    trailing = {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
                            contentDescription = null
                        )
                    },
                    onClick = { viewModel.clearLogs() }
                )
            }

            // About
            SettingsSection(title = stringResource(R.string.about)) {
                SettingItem(
                    title = stringResource(R.string.version),
                    subtitle = "1.0.0",
                    icon = androidx.compose.material.icons.Icons.Default.Info,
                    trailing = {}
                )

                SettingItem(
                    title = stringResource(R.string.open_source_licenses),
                    subtitle = stringResource(R.string.open_source_licenses_desc),
                    icon = androidx.compose.material.icons.Icons.Default.Gavel,
                    trailing = {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
                            contentDescription = null
                        )
                    },
                    onClick = { viewModel.showLicenses() }
                )

                SettingItem(
                    title = stringResource(R.string.privacy_policy),
                    subtitle = stringResource(R.string.privacy_policy_desc),
                    icon = androidx.compose.material.icons.Icons.Default.PrivacyTip,
                    trailing = {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
                            contentDescription = null
                        )
                    },
                    onClick = { viewModel.showPrivacyPolicy() }
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = androidx.compose.foundation.layout.Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.foundation.layout.Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
fun SettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    trailing: @Composable () -> Unit,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null) {
        androidx.compose.foundation.layout.Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onClick)
            .background(androidx.compose.material3.MaterialTheme.colorScheme.surface)
    } else {
        androidx.compose.foundation.layout.Modifier
            .fillMaxWidth()
            .padding(16.dp)
    }

    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = androidx.compose.foundation.layout.Modifier.size(24.dp),
                tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
            )
            androidx.compose.foundation.layout.Column {
                Text(text = title, fontSize = 16.sp)
                Text(text = subtitle, fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}