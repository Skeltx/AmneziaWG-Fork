package com.amneziawg.fork.ui.main

import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amneziawg.fork.R
import com.amneziawg.fork.ui.theme.AmneziaTheme
import com.amneziawg.fork.vpn.AmneziaVPNService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.connectSelectedConfig()
        }
    }

    private val vpnPrepareLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.connectSelectedConfig()
        }
    }

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AmneziaTheme {
                MainScreen(
                    viewModel = viewModel,
                    onRequestVpnPermission = { vpnPermissionLauncher.launch(Manifest.permission.INTERNET) },
                    onPrepareVpn = { intent -> vpnPrepareLauncher.launch(intent) }
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestVpnPermission: () -> Unit,
    onPrepareVpn: (Intent) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer),
                actions = {
                    IconButton(onClick = { viewModel.navigateToSettings() }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                    IconButton(onClick = { viewModel.navigateToAddConfig() }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_config)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection Status Card
            ConnectionStatusCard(
                state = uiState.connectionState,
                activeConfig = uiState.activeConfig,
                onToggle = { viewModel.onToggleConnection() },
                onPrepareVpn = onPrepareVpn,
                onRequestPermission = onRequestVpnPermission
            )

            // Active Config Info
            uiState.activeConfig?.let { config ->
                ActiveConfigCard(config = config, onDisconnect = { viewModel.disconnect() })
            }

            // Config List
            ConfigListSection(
                configs = uiState.configs,
                activeConfigId = uiState.activeConfig?.id,
                onConfigClick = { config -> viewModel.selectConfig(config.id) },
                onConfigLongClick = { config -> viewModel.showConfigOptions(config) },
                onAddConfig = { viewModel.navigateToAddConfig() }
            )

            // Routing Rules Summary
            if (uiState.activeConfig != null) {
                RoutingRulesSummaryCard(
                    config = uiState.activeConfig!!,
                    onManageRules = { viewModel.navigateToRoutingRules() }
                )
            }

            // Stats
            uiState.stats?.let { stats ->
                StatsCard(stats = stats)
            }
        }
    }
}

@Composable
fun ConnectionStatusCard(
    state: AmneziaVPNService.ConnectionState,
    activeConfig: com.amneziawg.fork.model.RoutingConfig?,
    onToggle: () -> Unit,
    onPrepareVpn: (Intent) -> Unit,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
        elevation = androidx.compose.foundation.layout.CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Icon
            val (icon, color) = when (state) {
                AmneziaVPNService.ConnectionState.CONNECTED -> androidx.compose.material.icons.Icons.Default.CheckCircle to androidx.compose.material3.MaterialTheme.colorScheme.primary
                AmneziaVPNService.ConnectionState.CONNECTING -> androidx.compose.material.icons.Icons.Default.Sync to androidx.compose.material3.MaterialTheme.colorScheme.tertiary
                AmneziaVPNService.ConnectionState.DISCONNECTING -> androidx.compose.material.icons.Icons.Default.HourglassEmpty to androidx.compose.material3.MaterialTheme.colorScheme.tertiary
                AmneziaVPNService.ConnectionState.ERROR -> androidx.compose.material.icons.Icons.Default.Error to androidx.compose.material3.MaterialTheme.colorScheme.error
                else -> androidx.compose.material.icons.Icons.Default.PowerSettingsNew to androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = androidx.compose.ui.Modifier.size(64.dp),
                tint = color
            )

            Spacer(modifier = androidx.compose.ui.Modifier.padding(16.dp))

            Text(
                text = when (state) {
                    AmneziaVPNService.ConnectionState.CONNECTED -> context.getString(R.string.status_connected)
                    AmneziaVPNService.ConnectionState.CONNECTING -> context.getString(R.string.status_connecting)
                    AmneziaVPNService.ConnectionState.DISCONNECTING -> context.getString(R.string.status_disconnecting)
                    AmneziaVPNService.ConnectionState.ERROR -> context.getString(R.string.status_error)
                    else -> context.getString(R.string.status_disconnected)
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Spacer(modifier = androidx.compose.ui.Modifier.padding(8.dp))

            activeConfig?.let {
                Text(
                    text = context.getString(R.string.active_config, it.name),
                    fontSize = 14.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = androidx.compose.ui.Modifier.padding(24.dp))

            // Main Toggle Button
            Button(
                onClick = {
                    when (state) {
                        AmneziaVPNService.ConnectionState.DISCONNECTED -> {
                            val intent = AmneziaVPNService.prepareVpn(context)
                            if (intent != null) {
                                onPrepareVpn(intent)
                            } else {
                                onToggle()
                            }
                        }
                        AmneziaVPNService.ConnectionState.CONNECTED -> onToggle()
                        else -> {}
                    }
                },
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = state != AmneziaVPNService.ConnectionState.CONNECTING && state != AmneziaVPNService.ConnectionState.DISCONNECTING,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = when (state) {
                        AmneziaVPNService.ConnectionState.CONNECTED -> androidx.compose.material3.MaterialTheme.colorScheme.error
                        else -> androidx.compose.material3.MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                Text(
                    text = when (state) {
                        AmneziaVPNService.ConnectionState.CONNECTED -> context.getString(R.string.disconnect)
                        AmneziaVPNService.ConnectionState.CONNECTING -> context.getString(R.string.connecting)
                        AmneziaVPNService.ConnectionState.DISCONNECTING -> context.getString(R.string.disconnecting)
                        else -> context.getString(R.string.connect)
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (state == AmneziaVPNService.ConnectionState.ERROR) {
                Spacer(modifier = androidx.compose.ui.Modifier.padding(16.dp))
                Text(
                    text = context.getString(R.string.connection_error),
                    fontSize = 14.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun ActiveConfigCard(
    config: com.amneziawg.fork.model.RoutingConfig,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier.padding(16.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Text(text = config.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = config.wireguardConfig.endpoint, fontSize = 14.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = androidx.compose.ui.Modifier.padding(8.dp))

            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "Rules: ${config.rules.size}", fontSize = 14.sp)
                Text(text = "Default: ${config.defaultAction.displayName}", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ConfigListSection(
    configs: List<com.amneziawg.fork.model.RoutingConfig>,
    activeConfigId: Long?,
    onConfigClick: (com.amneziawg.fork.model.RoutingConfig) -> Unit,
    onConfigLongClick: (com.amneziawg.fork.model.RoutingConfig) -> Unit,
    onAddConfig: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Text(text = context.getString(R.string.configs), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            androidx.compose.material3.TextButton(onClick = onAddConfig) {
                Text(text = context.getString(R.string.add_config))
            }
        }

        if (configs.isEmpty()) {
            Card(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.VpnLock,
                        contentDescription = null,
                        modifier = androidx.compose.ui.Modifier.size(48.dp),
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = androidx.compose.ui.Modifier.padding(16.dp))
                    Text(
                        text = context.getString(R.string.no_configs),
                        fontSize = 16.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = androidx.compose.ui.Modifier.padding(8.dp))
                    Text(
                        text = context.getString(R.string.no_configs_hint),
                        fontSize = 14.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
            ) {
                items(configs) { config ->
                    ConfigItem(
                        config = config,
                        isActive = config.id == activeConfigId,
                        onClick = { onConfigClick(config) },
                        onLongClick = { onConfigLongClick(config) }
                    )
                }
            }
        }
    }
}

@Composable
fun ConfigItem(
    config: com.amneziawg.fork.model.RoutingConfig,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .fillMaxSize(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isActive) androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable(onClick = onClick)
                .combinedClickable(
                    onLongClick = onLongClick,
                    onClick = onClick
                ),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.foundation.layout.Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Column {
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.foundation.layout.Alignment.CenterVertically
                ) {
                    Text(
                        text = config.name,
                        fontSize = 16.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                    )
                    if (isActive) {
                        androidx.compose.material3.Badge(
                            badgeContent = { Text(text = "ACTIVE", fontSize = 10.sp) },
                            colors = androidx.compose.material3.BadgeDefaults.badgeColors(
                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                            )
                        ) {}
                    }
                }
                Spacer(modifier = androidx.compose.ui.Modifier.padding(4.dp))
                Text(
                    text = "${config.wireguardConfig.endpoint} | Rules: ${config.rules.size}",
                    fontSize = 12.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
                contentDescription = null,
                tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RoutingRulesSummaryCard(
    config: com.amneziawg.fork.model.RoutingConfig,
    onManageRules: () -> Unit
) {
    val context = LocalContext.context

    Card(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier.padding(16.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Text(text = context.getString(R.string.routing_rules), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                androidx.compose.material3.TextButton(onClick = onManageRules) {
                    Text(text = context.getString(R.string.manage))
                }
            }

            if (config.rules.isEmpty()) {
                Text(
                    text = context.getString(R.string.no_routing_rules),
                    fontSize = 14.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = androidx.compose.ui.Modifier.padding(top = 8.dp)
                )
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(config.rules.take(5)) { rule ->
                        RuleSummaryItem(rule = rule)
                    }
                    if (config.rules.size > 5) {
                        Text(
                            text = context.getString(R.string.and_more_rules, config.rules.size - 5),
                            fontSize = 12.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = androidx.compose.ui.Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RuleSummaryItem(rule: com.amneziawg.fork.model.RoutingRule) {
    androidx.compose.foundation.layout.Row(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.foundation.layout.Alignment.CenterVertically
        ) {
            // Rule type icon
            val icon = when (rule.type) {
                com.amneziawg.fork.model.RoutingRuleType.APP_PACKAGE -> androidx.compose.material.icons.Icons.Default.Apps
                com.amneziawg.fork.model.RoutingRuleType.DOMAIN, com.amneziawg.fork.model.RoutingRuleType.DNS_NAME -> androidx.compose.material.icons.Icons.Default.Language
                com.amneziawg.fork.model.RoutingRuleType.IP_CIDR -> androidx.compose.material.icons.Icons.Default.Router
                com.amneziawg.fork.model.RoutingRuleType.PROCESS_UID -> androidx.compose.material.icons.Icons.Default.Memory
                com.amneziawg.fork.model.RoutingRuleType.GEO_IP -> androidx.compose.material.icons.Icons.Default.Public
                else -> androidx.compose.material.icons.Icons.Default.Help
            }

            Icon(imageVector = icon, contentDescription = null, modifier = androidx.compose.ui.Modifier.size(20.dp), tint = androidx.compose.material3.MaterialTheme.colorScheme.primary)

            androidx.compose.foundation.layout.Column {
                Text(text = rule.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(text = "${rule.type.displayName}: ${rule.target}", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Action badge
        val (actionText, actionColor) = when (rule.action) {
            com.amneziawg.fork.model.RoutingAction.ROUTE_VIA_VPN -> "VPN" to androidx.compose.material3.MaterialTheme.colorScheme.primary
            com.amneziawg.fork.model.RoutingAction.ROUTE_DIRECT -> "Direct" to androidx.compose.material3.MaterialTheme.colorScheme.tertiary
            com.amneziawg.fork.model.RoutingAction.BLOCK -> "Block" to androidx.compose.material3.MaterialTheme.colorScheme.error
            com.amneziawg.fork.model.RoutingAction.ROUTE_VIA_CONFIG -> "Other Config" to androidx.compose.material3.MaterialTheme.colorScheme.secondary
        }

        androidx.compose.material3.Badge(
            badgeContent = { Text(text = actionText, fontSize = 10.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary) },
            colors = androidx.compose.material3.BadgeDefaults.badgeColors(containerColor = actionColor)
        ) {}
    }
}

@Composable
fun StatsCard(stats: com.amneziawg.fork.vpn.RoutingEngine.RoutingStats) {
    val context = LocalContext.current

    Card(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier.padding(16.dp)
        ) {
            Text(text = context.getString(R.string.stats), fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = androidx.compose.ui.Modifier.padding(12.dp))

            androidx.compose.foundation.layout.Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatItem(
                    label = context.getString(R.string.vpn_traffic),
                    value = formatBytes(stats.totalBytesVpn),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
                StatItem(
                    label = context.getString(R.string.direct_traffic),
                    value = formatBytes(stats.totalBytesDirect),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
                )
                StatItem(
                    label = context.getString(R.string.blocked),
                    value = stats.blockedPackets.toString(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier
            .weight(1f)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes} B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
    }
}