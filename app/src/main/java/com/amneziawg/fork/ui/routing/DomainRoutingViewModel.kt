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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amneziawg.fork.db.AmneziaDatabase
import com.amneziawg.fork.model.DomainRoutingProfile
import com.amneziawg.fork.model.RoutingAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DomainRoutingViewModel @Inject constructor(
    private val application: Application,
    private val database: AmneziaDatabase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _domains = MutableStateFlow<List<DomainInfo>>(emptyList())

    val uiState = combine(_isLoading, _domains) { isLoading, domains ->
        DomainRoutingUiState(isLoading = isLoading, domains = domains)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DomainRoutingUiState(isLoading = true, domains = emptyList())
    )

    init {
        loadDomains()
    }

    private fun loadDomains() {
        viewModelScope.launch {
            _isLoading.value = true
            val profiles = database.domainRoutingProfileDao().getAll().firstOrNull() ?: emptyList()
            val domains = profiles.map { profile ->
                DomainInfo(
                    domain = profile.domain,
                    isRegex = profile.isRegex,
                    action = profile.action,
                    configId = profile.assignedConfigId
                )
            }
            _domains.value = domains
            _isLoading.value = false
        }
    }

    fun onRefresh() {
        loadDomains()
    }

    fun addDomainRule(domain: String, isRegex: Boolean, action: RoutingAction, configId: Long?) {
        viewModelScope.launch {
            val profile = DomainRoutingProfile(
                domain = domain,
                assignedConfigId = configId,
                action = action,
                isRegex = isRegex
            )
            database.domainRoutingProfileDao().upsertProfile(profile)
            loadDomains()
        }
    }

    fun deleteDomainRule(domain: String) {
        viewModelScope.launch {
            database.domainRoutingProfileDao().deleteByDomain(domain)
            loadDomains()
        }
    }

    fun editDomainRule(domain: DomainInfo) {
        // Show edit dialog
    }

    fun onBack() {
        // Navigation back
    }
}

data class DomainRoutingUiState(
    val isLoading: Boolean,
    val domains: List<DomainInfo>
)

data class DomainInfo(
    val domain: String,
    val isRegex: Boolean,
    val action: RoutingAction,
    val configId: Long?
)