package com.amneziawg.fork.ui.routing

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amneziawg.fork.db.AmneziaDatabase
import com.amneziawg.fork.model.AppRoutingProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AppRoutingViewModel @Inject constructor(
    private val application: Application,
    private val database: AmneziaDatabase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())

    val uiState = combine(_isLoading, _apps) { isLoading, apps ->
        AppRoutingUiState(isLoading = isLoading, apps = apps)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppRoutingUiState(isLoading = true, apps = emptyList())
    )

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val apps = withContext(Dispatchers.IO) { getInstalledApps() }
            _apps.value = apps
            _isLoading.value = false
        }
    }

    private fun getInstalledApps(): List<AppInfo> {
        val pm = application.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val profiles = database.appRoutingProfileDao().getAll().firstOrNull() ?: emptyList()
        val profileMap = profiles.associateBy { it.packageName }

        return apps.mapNotNull { appInfo ->
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            // Optionally filter system apps
            // if (isSystem) return@mapNotNull null

            val profile = profileMap[appInfo.packageName]
            val label = pm.getApplicationLabel(appInfo).toString()
            val icon = appInfo.loadIcon(pm)

            AppInfo(
                packageName = appInfo.packageName,
                appName = label,
                icon = icon,
                assignedConfigId = profile?.assignedConfigId,
                isSystem = isSystem
            )
        }.sortedBy { it.appName.lowercase() }
    }

    fun onRefreshApps() {
        loadApps()
    }

    fun onAppConfigChange(packageName: String, configId: Long?) {
        viewModelScope.launch {
            val profile = database.appRoutingProfileDao().getByPackageName(packageName).firstOrNull()?.toModel()
                ?: AppRoutingProfile(
                    packageName = packageName,
                    appName = application.packageManager.getApplicationLabel(
                        application.packageManager.getApplicationInfo(packageName, 0)
                    ).toString(),
                    assignedConfigId = configId
                )

            val updatedProfile = profile.copy(assignedConfigId = configId)
            database.appRoutingProfileDao().upsertProfile(updatedProfile)
            loadApps()
        }
    }

    fun onAppRuleClick(packageName: String) {
        // Navigate to rule editor for this app
    }

    fun onBack() {
        // Navigation back
    }
}

data class AppRoutingUiState(
    val isLoading: Boolean,
    val apps: List<AppInfo>
)