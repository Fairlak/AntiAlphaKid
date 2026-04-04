package ru.fairlak.antialphakid.features.dashboard.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.flow.combine
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.fairlak.antialphakid.core.database.AppDatabase
import ru.fairlak.antialphakid.core.database.AppUsageEntity
import ru.fairlak.antialphakid.features.monitor.data.AppDetector
import ru.fairlak.antialphakid.features.monitor.service.MonitoringService
import kotlin.text.contains

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).appUsageDao()
    private val packageManager = application.packageManager
    private var statsUpdateJob: Job? = null
    private val detector = AppDetector(application)
    private val _usageStats = MutableStateFlow<Map<String, Long>?>(null)
    val usageStats: StateFlow<Map<String, Long>?> = _usageStats
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _installedApps = MutableStateFlow<List<ApplicationInfo>>(emptyList())
    private val prefs = application.getSharedPreferences("anti_alpha_prefs", Context.MODE_PRIVATE)
    private val _isSystemActive = MutableStateFlow(prefs.getBoolean("system_active", true))
    val isSystemActive: StateFlow<Boolean> = _isSystemActive
    private val appNamesCache = mutableMapOf<String, String>()


    val appLimits: StateFlow<List<AppUsageEntity>> = dao.getAllLimits()
        .map { limits -> limits.sortedBy { it.addedAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredApps: StateFlow<List<ApplicationInfo>> = combine(
        _installedApps,
        _searchQuery,
        appLimits
    ) { apps, query, limits ->

        val existingPackages = limits.map { it.packageName }.toSet()

        if (query.isBlank()) {
            apps.filter { !existingPackages.contains(it.packageName) }
        } else {
            val lowerQuery = query.lowercase()
            apps.filter { app ->
                val isNotAdded = !existingPackages.contains(app.packageName)
                val name = appNamesCache[app.packageName] ?: app.packageName
                val matchesQuery = name.lowercase().contains(lowerQuery)
                isNotAdded && matchesQuery
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadInstalledApps()
        viewModelScope.launch {
            appLimits.collect { limits ->
                if (limits.isNotEmpty()) {
                    updateUsageStatsSync()
                } else {
                    _usageStats.value = emptyMap()
                }
            }
        }
    }

    fun toggleSystemState() {
        val newState = !_isSystemActive.value
        _isSystemActive.value = newState
        prefs.edit().putBoolean("system_active", newState).apply()

        val intent = Intent(getApplication(), MonitoringService::class.java)
        if (newState) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getApplication<Application>().startForegroundService(intent)
            } else {
                getApplication<Application>().startService(intent)
            }
        } else {
            getApplication<Application>().stopService(intent)
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val myPackageName = getApplication<Application>().packageName
            val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { app ->
                    val hasLauncher = packageManager.getLaunchIntentForPackage(app.packageName) != null
                    app.packageName != myPackageName && hasLauncher
                }
            apps.forEach { app ->
                if (!appNamesCache.containsKey(app.packageName)) {
                    appNamesCache[app.packageName] = packageManager.getApplicationLabel(app).toString()
                }
            }

            _installedApps.value = apps.sortedBy { appNamesCache[it.packageName] ?: it.packageName }
        }
    }


    private fun updateUsageStatsSync() {
        val limits = appLimits.value
        val packages = limits.map { it.packageName }
        if (packages.isNotEmpty()) {
            val (stats, _) = detector.getTodayStatsPackages(packages)
            _usageStats.value = stats
        }
    }

    fun updateUsageStats() {
        viewModelScope.launch(Dispatchers.IO) {
            updateUsageStatsSync()
        }
    }

    fun startStatsUpdates() {
        statsUpdateJob?.cancel()
        statsUpdateJob = viewModelScope.launch(Dispatchers.IO) {
            updateUsageStatsSync()

            while (isActive) {
                delay(300_000)
                updateUsageStatsSync()
            }
        }
    }

    fun stopStatsUpdates() {
        statsUpdateJob?.cancel()
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }


    fun saveLimit(packageName: String, minutes: Int) {
        viewModelScope.launch {
            val existing = appLimits.value.find { it.packageName == packageName }
            val entity = existing?.
            copy(limitMinutes = minutes) ?: AppUsageEntity(packageName, minutes)
            dao.saveLimit(entity)
        }
    }

    fun removeLimit(packageName: String) {
        viewModelScope.launch {
            val entity = AppUsageEntity(packageName, 0)
            dao.deleteLimit(entity)
        }
    }

    fun getAppName(packageName: String): String {
        return appNamesCache[packageName] ?: try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            val name = packageManager.getApplicationLabel(info).toString()
            appNamesCache[packageName] = name
            name
        } catch (_: Exception) {
            packageName
        }
    }

    fun getAppIcon(packageName: String): android.graphics.drawable.Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (_: Exception) {
            null
        }
    }
}