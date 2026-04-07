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
    private val _hasPassword = MutableStateFlow(!prefs.getString("app_password", null).isNullOrEmpty())
    val hasPassword: StateFlow<Boolean> = _hasPassword

    fun refreshState() {
        _isSystemActive.value = prefs.getBoolean("system_active", true)
        _isNotificEnabled.value = prefs.getBoolean("notifications_enabled", true)
        _hasPassword.value = !prefs.getString("app_password", null).isNullOrEmpty()
    }

    fun toggleNotifications() {
        val newState = !_isNotificEnabled.value
        _isNotificEnabled.value = newState
        prefs.edit().putBoolean("notifications_enabled", newState).apply()
    }

    fun savePassword(newPassword: String) {
        prefs.edit().putString("app_password", newPassword).apply()
        _hasPassword.value = newPassword.isNotEmpty()
    }

    fun checkPassword(input: String): Boolean {
        val saved = prefs.getString("app_password", "")
        return saved == input
    }

    fun clearPassword() {
        prefs.edit().remove("app_password").apply()
        _hasPassword.value = false
    }
}