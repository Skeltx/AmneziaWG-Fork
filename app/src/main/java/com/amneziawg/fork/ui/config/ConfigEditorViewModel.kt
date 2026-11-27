package com.amneziawg.fork.ui.config

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amneziawg.fork.db.AmneziaDatabase
import com.amneziawg.fork.model.RoutingAction
import com.amneziawg.fork.model.RoutingConfig
import com.amneziawg.fork.model.RoutingRule
import com.amneziawg.fork.model.RoutingRuleDto
import com.amneziawg.fork.model.RoutingRuleType
import com.amneziawg.fork.model.WireGuardConfig
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
class ConfigEditorViewModel @Inject constructor(
    private val application: Application,
    private val database: AmneziaDatabase
) : ViewModel() {

    private val _name = MutableStateFlow("")
    private val _privateKey = MutableStateFlow("")
    private val _publicKey = MutableStateFlow("")
    private val _endpoint = MutableStateFlow("")
    private val _allowedIps = MutableStateFlow<List<String>>(listOf("0.0.0.0/0", "::/0"))
    private val _dns = MutableStateFlow<List<String>>(listOf("1.1.1.1", "8.8.8.8"))
    private val _mtu = MutableStateFlow(1420)
    private val _keepalive = MutableStateFlow(25)
    private val _rules = MutableStateFlow<List<RoutingRuleDto>>(emptyList())
    private val _defaultAction = MutableStateFlow(RoutingAction.ROUTE_VIA_VPN)
    private val _isEditing = MutableStateFlow(false)
    private val _editingConfigId = MutableStateFlow<Long?>(null)

    val uiState = combine(
        _name, _privateKey, _publicKey, _endpoint, _allowedIps, _dns,
        _mtu, _keepalive, _rules, _defaultAction, _isEditing, _editingConfigId
    ) { name, privateKey, publicKey, endpoint, allowedIps, dns, mtu, keepalive, rules, defaultAction, isEditing, editingConfigId ->
        ConfigEditorUiState(
            name = name,
            privateKey = privateKey,
            publicKey = publicKey,
            endpoint = endpoint,
            allowedIps = allowedIps,
            dns = dns,
            mtu = mtu,
            keepalive = keepalive,
            rules = rules,
            defaultAction = defaultAction,
            isEditing = isEditing,
            editingConfigId = editingConfigId,
            isValid = name.isNotBlank() && privateKey.isNotBlank() && endpoint.isNotBlank() && allowedIps.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConfigEditorUiState(
            name = "",
            privateKey = "",
            publicKey = "",
            endpoint = "",
            allowedIps = listOf("0.0.0.0/0", "::/0"),
            dns = listOf("1.1.1.1", "8.8.8.8"),
            mtu = 1420,
            keepalive = 25,
            rules = emptyList(),
            defaultAction = RoutingAction.ROUTE_VIA_VPN,
            isEditing = false,
            editingConfigId = null,
            isValid = false
        )
    )

    fun loadConfig(configId: Long) {
        viewModelScope.launch {
            val config = database.routingConfigDao().getWithRules(configId)?.toModel()
            config?.let {
                _name.value = it.name
                _privateKey.value = it.wireguardConfig.privateKey
                _publicKey.value = it.wireguardConfig.publicKey
                _endpoint.value = it.wireguardConfig.endpoint
                _allowedIps.value = it.wireguardConfig.allowedIPs
                _dns.value = it.wireguardConfig.dns
                _mtu.value = it.wireguardConfig.mtu
                _keepalive.value = it.wireguardConfig.persistentKeepalive
                _rules.value = it.rules.map { rule ->
                    RoutingRuleDto(
                        name = rule.name,
                        type = rule.type,
                        target = rule.target,
                        action = rule.action,
                        priority = rule.priority,
                        isEnabled = rule.isEnabled
                    )
                }
                _defaultAction.value = it.defaultAction
                _isEditing.value = true
                _editingConfigId.value = configId
            }
        }
    }

    fun onNameChange(name: String) { _name.value = name }
    fun onPrivateKeyChange(key: String) { _privateKey.value = key }
    fun onPublicKeyChange(key: String) { _publicKey.value = key }
    fun onEndpointChange(endpoint: String) { _endpoint.value = endpoint }
    fun onAllowedIpsChange(ips: List<String>) { _allowedIps.value = ips }
    fun onDnsChange(dns: List<String>) { _dns.value = dns }
    fun onMtuChange(mtu: Int) { _mtu.value = mtu }
    fun onKeepaliveChange(keepalive: Int) { _keepalive.value = keepalive }
    fun onDefaultActionChange(action: RoutingAction) { _defaultAction.value = action }

    fun onAddRule() {
        val newRule = RoutingRuleDto(
            name = "New Rule",
            type = RoutingRuleType.APP_PACKAGE,
            target = "",
            action = RoutingAction.ROUTE_VIA_VPN,
            priority = _rules.value.size,
            isEnabled = true
        )
        _rules.value = _rules.value + newRule
    }

    fun onRuleUpdate(index: Int, updatedRule: RoutingRuleDto) {
        val currentRules = _rules.value.toMutableList()
        if (index in currentRules.indices) {
            currentRules[index] = updatedRule
            _rules.value = currentRules
        }
    }

    fun onRuleDelete(index: Int) {
        val currentRules = _rules.value.toMutableList()
        if (index in currentRules.indices) {
            currentRules.removeAt(index)
            _rules.value = currentRules
        }
    }

    fun onSave() {
        viewModelScope.launch {
            val wgConfig = WireGuardConfig(
                name = _name.value,
                privateKey = _privateKey.value,
                publicKey = _publicKey.value,
                address = _allowedIps.value, // Using allowedIPs as address for simplicity
                dns = _dns.value,
                endpoint = _endpoint.value,
                allowedIPs = _allowedIps.value,
                persistentKeepalive = _keepalive.value,
                mtu = _mtu.value
            )

            val rules = _rules.value.mapIndexed { index, dto ->
                RoutingRule(
                    configId = _editingConfigId.value ?: 0,
                    name = dto.name,
                    type = dto.type,
                    target = dto.target,
                    action = dto.action,
                    priority = dto.priority,
                    isEnabled = dto.isEnabled
                )
            }

            val config = RoutingConfig(
                id = _editingConfigId.value ?: 0,
                name = _name.value,
                wireguardConfig = wgConfig,
                rules = rules,
                defaultAction = _defaultAction.value
            )

            if (_isEditing.value) {
                database.routingConfigDao().update(com.amneziawg.fork.db.RoutingConfigEntity.fromModel(config))
                database.routingRuleDao().replaceRulesForConfig(
                    config.id,
                    rules.map { com.amneziawg.fork.db.RoutingRuleEntity.fromModel(it) }
                )
            } else {
                val newId = database.routingConfigDao().insert(com.amneziawg.fork.db.RoutingConfigEntity.fromModel(config))
                val rulesWithId = rules.map { it.copy(configId = newId) }
                database.routingRuleDao().insertAll(rulesWithId.map { com.amneziawg.fork.db.RoutingRuleEntity.fromModel(it) })
            }

            // Navigate back
            onCancel()
        }
    }

    fun onCancel() {
        // Navigation back handled by activity
    }

    fun reset() {
        _name.value = ""
        _privateKey.value = ""
        _publicKey.value = ""
        _endpoint.value = ""
        _allowedIps.value = listOf("0.0.0.0/0", "::/0")
        _dns.value = listOf("1.1.1.1", "8.8.8.8")
        _mtu.value = 1420
        _keepalive.value = 25
        _rules.value = emptyList()
        _defaultAction.value = RoutingAction.ROUTE_VIA_VPN
        _isEditing.value = false
        _editingConfigId.value = null
    }
}

data class ConfigEditorUiState(
    val name: String,
    val privateKey: String,
    val publicKey: String,
    val endpoint: String,
    val allowedIps: List<String>,
    val dns: List<String>,
    val mtu: Int,
    val keepalive: Int,
    val rules: List<RoutingRuleDto>,
    val defaultAction: RoutingAction,
    val isEditing: Boolean,
    val editingConfigId: Long?,
    val isValid: Boolean
)