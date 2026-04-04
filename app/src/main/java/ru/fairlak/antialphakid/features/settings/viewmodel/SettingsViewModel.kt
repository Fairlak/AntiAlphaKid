package ru.fairlak.antialphakid.features.settings.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("anti_alpha_prefs", Context.MODE_PRIVATE)

    private val _isSystemActive = MutableStateFlow(prefs.getBoolean("system_active", true))
    val isSystemActive: StateFlow<Boolean> = _isSystemActive
    private val _isNotificEnabled = MutableStateFlow(prefs.getBoolean("notifications_enabled", true))
    val isNotificEnabled: StateFlow<Boolean> = _isNotificEnabled

    fun refreshState() {
        _isSystemActive.value = prefs.getBoolean("system_active", true)
        _isNotificEnabled.value = prefs.getBoolean("notifications_enabled", true)
    }

    fun toggleNotifications() {
        val newState = !_isNotificEnabled.value
        _isNotificEnabled.value = newState
        prefs.edit().putBoolean("notifications_enabled", newState).apply()
    }
}