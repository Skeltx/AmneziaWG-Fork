package com.amneziawg.fork.ui.main

import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.app.Application
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amneziawg.fork.db.AmneziaDatabase
import com.amneziawg.fork.model.RoutingConfig
import com.amneziawg.fork.vpn.AmneziaVPNService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application,
    private val database: AmneziaDatabase
) : ViewModel() {

    private val _connectionState = MutableStateFlow(AmneziaVPNService.ConnectionState.DISCONNECTED)
    private val _selectedConfigId = MutableStateFlow<Long?>(null)
    private val _activeConfig = MutableStateFlow<RoutingConfig?>(null)
    private val _stats = MutableStateFlow<com.amneziawg.fork.vpn.RoutingEngine.RoutingStats?>(null)

    // UI State
    val uiState = combine(
        _connectionState,
        database.routingConfigDao().getAll().map { entities ->
            entities.map { it.toModel() }
        }.distinctUntilChanged(),
        _activeConfig,
        _stats
    ) { connectionState, configs, activeConfig, stats ->
        MainUiState(
            connectionState = connectionState,
            configs = configs,
            activeConfig = activeConfig,
            stats = stats
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(
            connectionState = AmneziaVPNService.ConnectionState.DISCONNECTED,
            configs = emptyList(),
            activeConfig = null,
            stats = null
        )
    )

    init {
        loadActiveConfig()
        observeVpnState()
    }

    private fun loadActiveConfig() {
        viewModelScope.launch {
            val active = database.routingConfigDao().getActive().firstOrNull()?.toModel()
            _activeConfig.value = active
            active?.id?.let { _selectedConfigId.value = it }
        }
    }

    private fun observeVpnState() {
        viewModelScope.launch {
            AmneziaVPNService.connectionStateFlow
                .distinctUntilChanged()
                .collect { state ->
                    _connectionState.value = state
                    if (state == AmneziaVPNService.ConnectionState.CONNECTED) {
                        // Update active config
                        _selectedConfigId.value?.let { loadActiveConfig() }
                    } else if (state == AmneziaVPNService.ConnectionState.DISCONNECTED) {
                        _activeConfig.value = null
                        _selectedConfigId.value = null
                    }
                }
        }
    }

    fun onToggleConnection() {
        when (_connectionState.value) {
            AmneziaVPNService.ConnectionState.DISCONNECTED -> {
                _selectedConfigId.value?.let { connect(it) }
            }
            AmneziaVPNService.ConnectionState.CONNECTED -> disconnect()
        }
    }

    fun selectConfig(configId: Long) {
        _selectedConfigId.value = configId
    }

    fun connect(configId: Long) {
        AmneziaVPNService.startConnection(application, configId)
    }

    fun disconnect() {
        AmneziaVPNService.stopConnection(application)
    }

    fun reconnect() {
        AmneziaVPNService.reconnect(application)
    }

    fun navigateToSettings() {
        // Navigation handled by Compose Navigation
    }

    fun navigateToAddConfig() {
        // Navigation handled by Compose Navigation
    }

    fun showConfigOptions(config: RoutingConfig) {
        // Show bottom sheet with options: edit, duplicate, delete, export
    }

    fun navigateToRoutingRules() {
        // Navigation handled by Compose Navigation
    }

    fun prepareVpn(): Intent? {
        return AmneziaVPNService.prepareVpn(application)
    }
}

data class MainUiState(
    val connectionState: AmneziaVPNService.ConnectionState,
    val configs: List<RoutingConfig>,
    val activeConfig: RoutingConfig?,
    val stats: com.amneziawg.fork.vpn.RoutingEngine.RoutingStats?
)