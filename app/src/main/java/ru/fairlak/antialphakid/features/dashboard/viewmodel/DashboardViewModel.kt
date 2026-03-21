package ru.fairlak.antialphakid.features.dashboard.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.combine
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.fairlak.antialphakid.core.database.AppDatabase
import ru.fairlak.antialphakid.core.database.AppUsageEntity
import kotlin.text.contains

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).appUsageDao()
    private val packageManager = application.packageManager
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _installedApps = MutableStateFlow<List<ApplicationInfo>>(emptyList())

    val filteredApps: StateFlow<List<ApplicationInfo>> = combine(_installedApps, _searchQuery) { apps, query ->
        if (query.isBlank()) apps
        else apps.filter { getAppName(it.packageName).contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                .sortedBy { getAppName(it.packageName) }
            _installedApps.value = apps
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }



    val appLimits: StateFlow<List<AppUsageEntity>> = dao.getAllLimits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveLimit(packageName: String, minutes: Int) {
        viewModelScope.launch {
            dao.saveLimit(AppUsageEntity(packageName, minutes))
        }
    }

    fun removeLimit(packageName: String) {
        viewModelScope.launch {
            val entity = AppUsageEntity(packageName, 0)
            dao.deleteLimit(entity)
        }
    }

    fun getAppName(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName
        }
    }
}