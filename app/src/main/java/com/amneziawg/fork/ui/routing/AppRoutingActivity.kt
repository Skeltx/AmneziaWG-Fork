package com.amneziawg.fork.ui.routing

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
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
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PorterDuff
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
class AppRoutingActivity : ComponentActivity() {

    private val viewModel: AppRoutingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AmneziaTheme {
                AppRoutingScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AppRoutingScreen(viewModel: AppRoutingViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.per_app_routing), fontWeight = FontWeight.Bold) },
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
                    IconButton(onClick = { viewModel.onRefreshApps() }) {
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
            modifier = androidx.compose.foundation.layout.Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info card
            Card(
                modifier = androidx.compose.foundation.layout.Modifier.fillMaxWidth()
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.foundation.layout.Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_routing_info),
                        fontSize = 14.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.app_routing_info_desc),
                        fontSize = 12.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // App list
            if (uiState.isLoading) {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.foundation.layout.Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                    Spacer(modifier = androidx.compose.foundation.layout.Modifier.padding(16.dp))
                    Text(text = stringResource(R.string.loading_apps))
                }
            } else {
                LazyColumn(
                    modifier = androidx.compose.foundation.layout.Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.apps) { app ->
                        AppRoutingItem(
                            app = app,
                            onConfigChange = { configId ->
                                viewModel.onAppConfigChange(app.packageName, configId)
                            },
                            onRuleClick = { viewModel.onAppRuleClick(app.packageName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppRoutingItem(
    app: AppInfo,
    onConfigChange: (Long?) -> Unit,
    onRuleClick: () -> Unit
) {
    Card(
        modifier = androidx.compose.foundation.layout.Modifier.fillMaxWidth()
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
                // App icon placeholder
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.foundation.layout.Modifier.size(40.dp)
                        .background(
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Android,
                        contentDescription = null,
                        modifier = androidx.compose.foundation.layout.Modifier
                            .size(24.dp)
                            .align(Alignment.Center),
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    )
                }

                androidx.compose.foundation.layout.Column {
                    Text(text = app.appName, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(text = app.packageName, fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.End
            ) {
                // Config selector would go here
                TextButton(onClick = onRuleClick) {
                    Text(text = stringResource(R.string.rules))
                }

                Text(
                    text = app.assignedConfigId?.let { "Config #$it" } ?: stringResource(R.string.use_default),
                    fontSize = 12.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val assignedConfigId: Long? = null,
    val isSystem: Boolean = false
)