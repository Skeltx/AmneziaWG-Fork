package com.amneziawg.fork.ui.config

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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
class ConfigEditorActivity : ComponentActivity() {

    private val viewModel: ConfigEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AmneziaTheme {
                ConfigEditorScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ConfigEditorScreen(viewModel: ConfigEditorViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) stringResource(R.string.edit_config) else stringResource(R.string.add_config), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onCancel() }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer),
                actions = {
                    IconButton(onClick = { viewModel.onSave() }, enabled = uiState.isValid) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Check,
                            contentDescription = stringResource(R.string.save)
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
            // Basic Config Section
            Card(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = stringResource(R.string.basic_settings), fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    ConfigTextField(
                        label = stringResource(R.string.config_name),
                        value = uiState.name,
                        onValueChange = { viewModel.onNameChange(it) },
                        isRequired = true
                    )

                    ConfigTextField(
                        label = stringResource(R.string.private_key),
                        value = uiState.privateKey,
                        onValueChange = { viewModel.onPrivateKeyChange(it) },
                        isRequired = true,
                        helperText = stringResource(R.string.private_key_hint)
                    )

                    ConfigTextField(
                        label = stringResource(R.string.public_key),
                        value = uiState.publicKey,
                        onValueChange = { viewModel.onPublicKeyChange(it) },
                        isRequired = false,
                        helperText = stringResource(R.string.public_key_hint)
                    )

                    ConfigTextField(
                        label = stringResource(R.string.endpoint),
                        value = uiState.endpoint,
                        onValueChange = { viewModel.onEndpointChange(it) },
                        isRequired = true,
                        helperText = stringResource(R.string.endpoint_hint)
                    )

                    ConfigTextField(
                        label = stringResource(R.string.allowed_ips),
                        value = uiState.allowedIps.joinToString(", "),
                        onValueChange = { viewModel.onAllowedIpsChange(it.split(",").map { it.trim() }.filter { it.isNotEmpty() }) },
                        isRequired = true,
                        helperText = stringResource(R.string.allowed_ips_hint)
                    )

                    ConfigTextField(
                        label = stringResource(R.string.dns_servers),
                        value = uiState.dns.joinToString(", "),
                        onValueChange = { viewModel.onDnsChange(it.split(",").map { it.trim() }.filter { it.isNotEmpty() }) },
                        isRequired = false,
                        helperText = stringResource(R.string.dns_hint)
                    )

                    androidx.compose.foundation.layout.Row(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ConfigNumberField(
                            label = stringResource(R.string.mtu),
                            value = uiState.mtu,
                            onValueChange = { viewModel.onMtuChange(it) },
                            defaultValue = 1420
                        )
                        ConfigNumberField(
                            label = stringResource(R.string.keepalive),
                            value = uiState.keepalive,
                            onValueChange = { viewModel.onKeepaliveChange(it) },
                            defaultValue = 25
                        )
                    }
                }
            }

            // Routing Rules Section
            Card(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        Text(text = stringResource(R.string.routing_rules), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.onAddRule() }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Add,
                                contentDescription = stringResource(R.string.add_rule)
                            )
                        }
                    }

                    if (uiState.rules.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_rules_yet),
                            fontSize = 14.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = androidx.compose.ui.Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.rules) { rule ->
                                RuleEditorItem(
                                    rule = rule,
                                    index = uiState.rules.indexOf(rule),
                                    onUpdate = { updatedRule -> viewModel.onRuleUpdate(uiState.rules.indexOf(rule), updatedRule) },
                                    onDelete = { viewModel.onRuleDelete(uiState.rules.indexOf(rule)) }
                                )
                            }
                        }
                    }
                }
            }

            // Default Action
            Card(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = stringResource(R.string.default_action), fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    androidx.compose.material3.RadioButton(
                        selected = uiState.defaultAction == com.amneziawg.fork.model.RoutingAction.ROUTE_VIA_VPN,
                        onClick = { viewModel.onDefaultActionChange(com.amneziawg.fork.model.RoutingAction.ROUTE_VIA_VPN) },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.route_via_vpn))
                    }

                    androidx.compose.material3.RadioButton(
                        selected = uiState.defaultAction == com.amneziawg.fork.model.RoutingAction.ROUTE_DIRECT,
                        onClick = { viewModel.onDefaultActionChange(com.amneziawg.fork.model.RoutingAction.ROUTE_DIRECT) },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.route_direct))
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isRequired: Boolean = false,
    helperText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        singleLine = true,
        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
        isError = isRequired && value.isBlank(),
        supportingText = if (helperText != null) { androidx.compose.material3.Text(text = helperText) } else { null }
    )
}

@Composable
fun ConfigNumberField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    defaultValue: Int
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { text ->
            text.toIntOrNull()?.let { onValueChange(it) } ?: onValueChange(defaultValue)
        },
        label = { Text(text = label) },
        singleLine = true,
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .weight(1f),
        keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
    )
}

@Composable
fun RuleEditorItem(
    rule: com.amneziawg.fork.model.RoutingRuleDto,
    index: Int,
    onUpdate: (com.amneziawg.fork.model.RoutingRuleDto) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Text(text = "Rule #${index + 1}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                        contentDescription = "Delete rule",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.error
                    )
                }
            }

            // Rule Type Dropdown
            androidx.compose.material3.Menu(
                expanded = androidx.compose.runtime.mutableStateOf(false)
            ) {
                // Would need proper dropdown implementation
            }

            // Simplified: Show rule type as text
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = rule.name,
                    onValueChange = { onUpdate(rule.copy(name = it)) },
                    label = { Text(text = "Name") },
                    modifier = androidx.compose.ui.Modifier.weight(1f)
                )
            }

            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Type selector would go here
                Text(text = "Type: ${rule.type.displayName}", fontSize = 14.sp)
            }

            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = rule.target,
                    onValueChange = { onUpdate(rule.copy(target = it)) },
                    label = { Text(text = "Target (package/domain/IP)") },
                    modifier = androidx.compose.ui.Modifier.weight(1f)
                )
            }

            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Action selector
                Text(text = "Action: ${rule.action.displayName}", fontSize = 14.sp)
            }

            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ConfigNumberField(
                    label = "Priority",
                    value = rule.priority,
                    onValueChange = { onUpdate(rule.copy(priority = it)) },
                    defaultValue = 0
                )
            }
        }
    }
}