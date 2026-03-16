package ru.fairlak.antialphakid.features.dashboard.viewmodel

import android.app.Application

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.fairlak.antialphakid.core.database.AppDatabase
import ru.fairlak.antialphakid.core.database.AppUsageEntity

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).appUsageDao()
    private val packageManager = application.packageManager

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