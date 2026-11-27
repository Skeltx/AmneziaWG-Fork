package com.amneziawg.fork.ui.routing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
class DnsSettingsActivity : ComponentActivity() {

    private val viewModel: DnsSettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AmneziaTheme {
                DnsSettingsScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DnsSettingsScreen(viewModel: DnsSettingsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var customDns1 by remember { mutableStateOf("") }
    var customDns2 by remember { mutableStateOf("") }
    var customDns3 by remember { mutableStateOf("") }

    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dns_settings), fontWeight = FontWeight.Bold) },
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
            // DNS Provider Selection
            Card(
                modifier = androidx.compose.foundation.layout.Modifier.fillMaxWidth()
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.foundation.layout.Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = stringResource(R.string.dns_provider), fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    DnsProviderOption(
                        name = stringResource(R.string.dns_system),
                        description = stringResource(R.string.dns_system_desc),
                        icon = androidx.compose.material.icons.Icons.Default.PhoneAndroid,
                        isSelected = uiState.selectedProvider == DnsProvider.SYSTEM,
                        onClick = { viewModel.onProviderChange(DnsProvider.SYSTEM) }
                    )

                    DnsProviderOption(
                        name = stringResource(R.string.dns_google),
                        description = stringResource(R.string.dns_google_desc),
                        icon = androidx.compose.material.icons.Icons.Default.Cloud,
                        isSelected = uiState.selectedProvider == DnsProvider.GOOGLE,
                        onClick = { viewModel.onProviderChange(DnsProvider.GOOGLE) }
                    )

                    DnsProviderOption(
                        name = stringResource(R.string.dns_cloudflare),
                        description = stringResource(R.string.dns_cloudflare_desc),
                        icon = androidx.compose.material.icons.Icons.Default.Shield,
                        isSelected = uiState.selectedProvider == DnsProvider.CLOUDFLARE,
                        onClick = { viewModel.onProviderChange(DnsProvider.CLOUDFLARE) }
                    )

                    DnsProviderOption(
                        name = stringResource(R.string.dns_quad9),
                        description = stringResource(R.string.dns_quad9_desc),
                        icon = androidx.compose.material.icons.Icons.Default.Security,
                        isSelected = uiState.selectedProvider == DnsProvider.QUAD9,
                        onClick = { viewModel.onProviderChange(DnsProvider.QUAD9) }
                    )

                    DnsProviderOption(
                        name = stringResource(R.string.dns_adguard),
                        description = stringResource(R.string.dns_adguard_desc),
                        icon = androidx.compose.material.icons.Icons.Default.Block,
                        isSelected = uiState.selectedProvider == DnsProvider.ADGUARD,
                        onClick = { viewModel.onProviderChange(DnsProvider.ADGUARD) }
                    )

                    DnsProviderOption(
                        name = stringResource(R.string.dns_custom),
                        description = stringResource(R.string.dns_custom_desc),
                        icon = androidx.compose.material.icons.Icons.Default.Edit,
                        isSelected = uiState.selectedProvider == DnsProvider.CUSTOM,
                        onClick = { viewModel.onProviderChange(DnsProvider.CUSTOM) }
                    )
                }
            }

            // Custom DNS Input
            if (uiState.selectedProvider == DnsProvider.CUSTOM) {
                Card(
                    modifier = androidx.compose.foundation.layout.Modifier.fillMaxWidth()
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = androidx.compose.foundation.layout.Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = stringResource(R.string.custom_dns_servers), fontSize = 18.sp, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = customDns1,
                            onValueChange = { customDns1 = it },
                            label = { Text(text = stringResource(R.string.primary_dns)) },
                            placeholder = { Text(text = "1.1.1.1") },
                            singleLine = true,
                            modifier = androidx.compose.foundation.layout.Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )

                        OutlinedTextField(
                            value = customDns2,
                            onValueChange = { customDns2 = it },
                            label = { Text(text = stringResource(R.string.secondary_dns)) },
                            placeholder = { Text(text = "8.8.8.8") },
                            singleLine = true,
                            modifier = androidx.compose.foundation.layout.Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = customDns3,
                            onValueChange = { customDns3 = it },
                            label = { Text(text = stringResource(R.string.tertiary_dns)) },
                            placeholder = { Text(text = "9.9.9.9") },
                            singleLine = true,
                            modifier = androidx.compose.foundation.layout.Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = { viewModel.saveCustomDns(customDns1, customDns2, customDns3) }
                        ) {
                            Text(text = stringResource(R.string.save))
                        }
                    }
                }
            }

            // Additional Options
            Card(
                modifier = androidx.compose.foundation.layout.Modifier.fillMaxWidth()
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.foundation.layout.Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = stringResource(R.string.additional_options), fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    SettingRow(
                        title = stringResource(R.string.block_ads_trackers),
                        subtitle = stringResource(R.string.block_ads_trackers_desc),
                        icon = androidx.compose.material.icons.Icons.Default.Block,
                        trailing = {
                            Switch(
                                checked = uiState.blockAdsTrackers,
                                onCheckedChange = { viewModel.onBlockAdsTrackersChange(it) }
                            )
                        }
                    )

                    SettingRow(
                        title = stringResource(R.string.block_malware),
                        subtitle = stringResource(R.string.block_malware_desc),
                        icon = androidx.compose.material.icons.Icons.Default.BugReport,
                        trailing = {
                            Switch(
                                checked = uiState.blockMalware,
                                onCheckedChange = { viewModel.onBlockMalwareChange(it) }
                            )
                        }
                    )

                    SettingRow(
                        title = stringResource(R.string.dns_over_https),
                        subtitle = stringResource(R.string.dns_over_https_desc),
                        icon = androidx.compose.material.icons.Icons.Default.Lock,
                        trailing = {
                            Switch(
                                checked = uiState.dnsOverHttps,
                                onCheckedChange = { viewModel.onDnsOverHttpsChange(it) }
                            )
                        }
                    )

                    SettingRow(
                        title = stringResource(R.string.dns_over_tls),
                        subtitle = stringResource(R.string.dns_over_tls_desc),
                        icon = androidx.compose.material.icons.Icons.Default.VpnKey,
                        trailing = {
                            Switch(
                                checked = uiState.dnsOverTls,
                                onCheckedChange = { viewModel.onDnsOverTlsChange(it) }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DnsProviderOption(
    name: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = androidx.compose.foundation.layout.Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer else androidx.compose.material3.MaterialTheme.colorScheme.surface
        )
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = androidx.compose.foundation.layout.Modifier.padding(16.dp),
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
                    tint = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.foundation.layout.Column {
                    Text(text = name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(text = description, fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    trailing: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = androidx.compose.foundation.layout.Modifier
            .fillMaxWidth()
            .padding(16.dp),
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

enum class DnsProvider {
    SYSTEM, GOOGLE, CLOUDFLARE, QUAD9, ADGUARD, CUSTOM
}

data class DnsSettingsUiState(
    val selectedProvider: DnsProvider = DnsProvider.SYSTEM,
    val customDns1: String = "",
    val customDns2: String = "",
    val customDns3: String = "",
    val blockAdsTrackers: Boolean = false,
    val blockMalware: Boolean = false,
    val dnsOverHttps: Boolean = true,
    val dnsOverTls: Boolean = false
)