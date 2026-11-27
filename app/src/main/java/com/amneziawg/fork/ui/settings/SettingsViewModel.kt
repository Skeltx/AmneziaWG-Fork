package com.amneziawg.fork.ui.settings

import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amneziawg.fork.db.AmneziaDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val database: AmneziaDatabase,
    private val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
) : ViewModel() {

    private val AUTO_RECONNECT_KEY = booleanPreferencesKey("auto_reconnect")
    private val BLOCK_NON_VPN_KEY = booleanPreferencesKey("block_non_vpn")
    private val ALWAYS_ON_VPN_KEY = booleanPreferencesKey("always_on_vpn")
    private val BIOMETRIC_LOCK_KEY = booleanPreferencesKey("biometric_lock")
    private val LOCK_ON_BACKGROUND_KEY = booleanPreferencesKey("lock_on_background")
    private val LOG_LEVEL_KEY = stringPreferencesKey("log_level")

    private val _autoReconnect = MutableStateFlow(true)
    private val _blockNonVpnTraffic = MutableStateFlow(false)
    private val _alwaysOnVpn = MutableStateFlow(false)
    private val _biometricLock = MutableStateFlow(false)
    private val _lockOnBackground = MutableStateFlow(true)
    private val _logLevel = MutableStateFlow("INFO")

    val uiState = combine(
        _autoReconnect, _blockNonVpnTraffic, _alwaysOnVpn,
        _biometricLock, _lockOnBackground, _logLevel
    ) { autoReconnect, blockNonVpn, alwaysOnVpn, biometricLock, lockOnBackground, logLevel ->
        SettingsUiState(
            autoReconnect = autoReconnect,
            blockNonVpnTraffic = blockNonVpn,
            alwaysOnVpn = alwaysOnVpn,
            biometricLock = biometricLock,
            lockOnBackground = lockOnBackground,
            logLevel = logLevel
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(
            autoReconnect = true,
            blockNonVpnTraffic = false,
            alwaysOnVpn = false,
            biometricLock = false,
            lockOnBackground = true,
            logLevel = "INFO"
        )
    )

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            _autoReconnect.value = prefs[AUTO_RECONNECT_KEY] ?: true
            _blockNonVpnTraffic.value = prefs[BLOCK_NON_VPN_KEY] ?: false
            _alwaysOnVpn.value = prefs[ALWAYS_ON_VPN_KEY] ?: false
            _biometricLock.value = prefs[BIOMETRIC_LOCK_KEY] ?: false
            _lockOnBackground.value = prefs[LOCK_ON_BACKGROUND_KEY] ?: true
            _logLevel.value = prefs[LOG_LEVEL_KEY] ?: "INFO"
        }
    }

    fun onAutoReconnectChange(value: Boolean) {
        _autoReconnect.value = value
        saveSetting(AUTO_RECONNECT_KEY, value)
    }

    fun onBlockNonVpnTrafficChange(value: Boolean) {
        _blockNonVpnTraffic.value = value
        saveSetting(BLOCK_NON_VPN_KEY, value)
    }

    fun onAlwaysOnVpnChange(value: Boolean) {
        _alwaysOnVpn.value = value
        saveSetting(ALWAYS_ON_VPN_KEY, value)
        // Apply always-on VPN setting
        applyAlwaysOnVpn(value)
    }

    fun onBiometricLockChange(value: Boolean) {
        _biometricLock.value = value
        saveSetting(BIOMETRIC_LOCK_KEY, value)
    }

    fun onLockOnBackgroundChange(value: Boolean) {
        _lockOnBackground.value = value
        saveSetting(LOCK_ON_BACKGROUND_KEY, value)
    }

    private fun saveSetting(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, value: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[key] = value }
        }
    }

    private fun saveSetting(key: androidx.datastore.preferences.core.Preferences.Key<String>, value: String) {
        viewModelScope.launch {
            dataStore.edit { it[key] = value }
        }
    }

    private fun applyAlwaysOnVpn(enabled: Boolean) {
        // Implementation would use VpnService.prepare and setAlwaysOnVpnPackage
    }

    fun navigateToAppRouting() {
        // Navigate to app routing screen
    }

    fun navigateToDomainRouting() {
        // Navigate to domain routing screen
    }

    fun navigateToDnsSettings() {
        // Navigate to DNS settings
    }

    fun showLogLevelPicker() {
        // Show dialog to pick log level
    }

    fun exportConfigs() {
        // Export all configs to file
    }

    fun importConfigs() {
        // Import configs from file
    }

    fun clearLogs() {
        viewModelScope.launch {
            database.vpnConnectionLogDao().deleteOldLogs(0)
        }
    }

    fun showLicenses() {
        // Show open source licenses
    }

    fun showPrivacyPolicy() {
        // Show privacy policy
    }

    fun onBack() {
        // Navigation back
    }
}

data class SettingsUiState(
    val autoReconnect: Boolean,
    val blockNonVpnTraffic: Boolean,
    val alwaysOnVpn: Boolean,
    val biometricLock: Boolean,
    val lockOnBackground: Boolean,
    val logLevel: String
)