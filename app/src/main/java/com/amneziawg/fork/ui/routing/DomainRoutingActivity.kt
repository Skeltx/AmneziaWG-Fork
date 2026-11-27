package com.amneziawg.fork.ui.routing

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
import com.amneziawg.fork.model.RoutingAction
import com.amneziawg.fork.ui.theme.AmneziaTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class DomainRoutingActivity : ComponentActivity() {

    private val viewModel: DomainRoutingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AmneziaTheme {
                DomainRoutingScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DomainRoutingScreen(viewModel: DomainRoutingViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var newDomain by remember { mutableStateOf("") }
    var isRegex by remember { mutableStateOf(false) }
    var selectedAction by remember { mutableStateOf(RoutingAction.ROUTE_VIA_VPN) }
    var selectedConfigId by remember { mutableStateOf<Long?>(null) }

    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.per_domain_routing), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBack() }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer),
                actions = {
                    IconButton(onClick = { viewModel.onRefresh() }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh)
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
            // Add new domain card
            Card(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = stringResource(R.string.add_domain_rule), fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = newDomain,
                        onValueChange = { newDomain = it },
                        label = { Text(text = stringResource(R.string.domain)) },
                        placeholder = { Text(text = "example.com") },
                        singleLine = true,
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri
                        )
                    )

                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = isRegex,
                            onCheckedChange = { isRegex = it }
                        )
                        Text(text = stringResource(R.string.regex_pattern))

                        androidx.compose.material3.Spacer(modifier = androidx.compose.ui.Modifier.weight(1f))

                        // Action dropdown
                        androidx.compose.material3.Text(text = stringResource(R.string.action))
                        androidx.compose.material3.Menu(
                            expanded = androidx.compose.runtime.mutableStateOf(false)
                        ) {
                            // Action selector
                        }
                    }

                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Config selector if action is ROUTE_VIA_CONFIG
                        if (selectedAction == RoutingAction.ROUTE_VIA_CONFIG) {
                            Text(text = stringResource(R.string.select_config))
                            // Config dropdown
                        }
                    }

                    Button(
                        onClick = {
                            if (newDomain.isNotBlank()) {
                                viewModel.addDomainRule(newDomain, isRegex, selectedAction, selectedConfigId)
                                newDomain = ""
                            }
                        },
                        enabled = newDomain.isNotBlank()
                    ) {
                        Text(text = stringResource(R.string.add))
                    }
                }
            }

            // Domain list
            if (uiState.isLoading) {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                    Spacer(modifier = androidx.compose.ui.Modifier.padding(16.dp))
                    Text(text = stringResource(R.string.loading))
                }
            } else {
                LazyColumn(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.domains) { domain ->
                        DomainRoutingItem(
                            domain = domain,
                            onDelete = { viewModel.deleteDomainRule(domain.domain) },
                            onEdit = { viewModel.editDomainRule(domain) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DomainRoutingItem(
    domain: DomainInfo,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Column {
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = domain.domain, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    if (domain.isRegex) {
                        androidx.compose.material3.Badge(
                            badgeContent = { Text(text = "REGEX", fontSize = 10.sp) },
                            colors = androidx.compose.material3.BadgeDefaults.badgeColors(
                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary
                            )
                        ) {}
                    }
                }

                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Action badge
                    val (actionText, actionColor) = when (domain.action) {
                        RoutingAction.ROUTE_VIA_VPN -> "VPN" to androidx.compose.material3.MaterialTheme.colorScheme.primary
                        RoutingAction.ROUTE_DIRECT -> "Direct" to androidx.compose.material3.MaterialTheme.colorScheme.tertiary
                        RoutingAction.BLOCK -> "Block" to androidx.compose.material3.MaterialTheme.colorScheme.error
                        RoutingAction.ROUTE_VIA_CONFIG -> "Config #${domain.configId}" to androidx.compose.material3.MaterialTheme.colorScheme.secondary
                    }

                    androidx.compose.material3.Badge(
                        badgeContent = { Text(text = actionText, fontSize = 10.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary) },
                        colors = androidx.compose.material3.BadgeDefaults.badgeColors(containerColor = actionColor)
                    ) {}

                    if (domain.configId != null) {
                        Text(text = "Config #${domain.configId}", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                        contentDescription = "Edit"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

data class DomainInfo(
    val domain: String,
    val isRegex: Boolean,
    val action: RoutingAction,
    val configId: Long?
)