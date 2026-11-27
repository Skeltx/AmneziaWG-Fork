package com.amneziawg.fork.ui.routing

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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class DnsSettingsViewModel @Inject constructor(
    private val application: Application,
    private val database: AmneziaDatabase,
    private val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
) : ViewModel() {

    private val DNS_PROVIDER_KEY = stringPreferencesKey("dns_provider")
    private val CUSTOM_DNS_1_KEY = stringPreferencesKey("custom_dns_1")
    private val CUSTOM_DNS_2_KEY = stringPreferencesKey("custom_dns_2")
    private val CUSTOM_DNS_3_KEY = stringPreferencesKey("custom_dns_3")
    private val BLOCK_ADS_KEY = booleanPreferencesKey("block_ads")
    private val BLOCK_MALWARE_KEY = booleanPreferencesKey("block_malware")
    private val DNS_OVER_HTTPS_KEY = booleanPreferencesKey("dns_over_https")
    private val DNS_OVER_TLS_KEY = booleanPreferencesKey("dns_over_tls")

    private val _selectedProvider = MutableStateFlow(DnsProvider.SYSTEM)
    private val _customDns1 = MutableStateFlow("")
    private val _customDns2 = MutableStateFlow("")
    private val _customDns3 = MutableStateFlow("")
    private val _blockAdsTrackers = MutableStateFlow(false)
    private val _blockMalware = MutableStateFlow(false)
    private val _dnsOverHttps = MutableStateFlow(true)
    private val _dnsOverTls = MutableStateFlow(false)

    val uiState = combine(
        _selectedProvider, _customDns1, _customDns2, _customDns3,
        _blockAdsTrackers, _blockMalware, _dnsOverHttps, _dnsOverTls
    ) { provider, dns1, dns2, dns3, blockAds, blockMalware, doh, dot ->
        DnsSettingsUiState(
            selectedProvider = provider,
            customDns1 = dns1,
            customDns2 = dns2,
            customDns3 = dns3,
            blockAdsTrackers = blockAds,
            blockMalware = blockMalware,
            dnsOverHttps = doh,
            dnsOverTls = dot
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DnsSettingsUiState()
    )

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            _selectedProvider.value = DnsProvider.valueOf(prefs[DNS_PROVIDER_KEY] ?: "SYSTEM")
            _customDns1.value = prefs[CUSTOM_DNS_1_KEY] ?: ""
            _customDns2.value = prefs[CUSTOM_DNS_2_KEY] ?: ""
            _customDns3.value = prefs[CUSTOM_DNS_3_KEY] ?: ""
            _blockAdsTrackers.value = prefs[BLOCK_ADS_KEY] ?: false
            _blockMalware.value = prefs[BLOCK_MALWARE_KEY] ?: false
            _dnsOverHttps.value = prefs[DNS_OVER_HTTPS_KEY] ?: true
            _dnsOverTls.value = prefs[DNS_OVER_TLS_KEY] ?: false
        }
    }

    fun onProviderChange(provider: DnsProvider) {
        _selectedProvider.value = provider
        saveSetting(DNS_PROVIDER_KEY, provider.name)
    }

    fun saveCustomDns(dns1: String, dns2: String, dns3: String) {
        _customDns1.value = dns1
        _customDns2.value = dns2
        _customDns3.value = dns3
        saveSetting(CUSTOM_DNS_1_KEY, dns1)
        saveSetting(CUSTOM_DNS_2_KEY, dns2)
        saveSetting(CUSTOM_DNS_3_KEY, dns3)
    }

    fun onBlockAdsTrackersChange(value: Boolean) {
        _blockAdsTrackers.value = value
        saveSetting(BLOCK_ADS_KEY, value)
    }

    fun onBlockMalwareChange(value: Boolean) {
        _blockMalware.value = value
        saveSetting(BLOCK_MALWARE_KEY, value)
    }

    fun onDnsOverHttpsChange(value: Boolean) {
        _dnsOverHttps.value = value
        saveSetting(DNS_OVER_HTTPS_KEY, value)
    }

    fun onDnsOverTlsChange(value: Boolean) {
        _dnsOverTls.value = value
        saveSetting(DNS_OVER_TLS_KEY, value)
    }

    private fun saveSetting(key: androidx.datastore.preferences.core.Preferences.Key<String>, value: String) {
        viewModelScope.launch {
            dataStore.edit { it[key] = value }
        }
    }

    private fun saveSetting(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, value: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[key] = value }
        }
    }

    fun onBack() {
        // Navigation back
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