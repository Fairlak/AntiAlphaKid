package ru.fairlak.antialphakid.features.settings.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ru.fairlak.antialphakid.core.ui.theme.MatrixGreen
import ru.fairlak.antialphakid.core.ui.theme.ThemeColors
import ru.fairlak.antialphakid.features.monitor.service.MonitoringService
import androidx.core.content.edit

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("anti_alpha_prefs", Context.MODE_PRIVATE)

    private val _isSystemActive = MutableStateFlow(prefs.getBoolean("system_active", true))
    val isSystemActive: StateFlow<Boolean> = _isSystemActive
    private val _isNotificEnabled = MutableStateFlow(prefs.getBoolean("notifications_enabled", true))
    val isNotificEnabled: StateFlow<Boolean> = _isNotificEnabled
    private val _hasPassword = MutableStateFlow(!prefs.getString("app_password", null).isNullOrEmpty())
    val hasPassword: StateFlow<Boolean> = _hasPassword
    private val _blockerText = MutableStateFlow(prefs.getString("blocker_text", "ЛИМИТ ИСЧЕРПАН") ?: "ЛИМИТ ИСЧЕРПАН")
    val blockerText: StateFlow<String> = _blockerText
    private val _onColorKey = MutableStateFlow(prefs.getString("color_on", "GREEN") ?: "GREEN")
    val onColorKey: StateFlow<String> = _onColorKey
    private val _offColorKey = MutableStateFlow(prefs.getString("color_off", "RED") ?: "RED")
    val offColorKey: StateFlow<String> = _offColorKey
    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked

    val activeColor: StateFlow<Color> = combine(
        isSystemActive,
        _onColorKey,
        _offColorKey
    ) { active, onKey, offKey ->
        val key = if (active) onKey else offKey
        ThemeColors[key] ?: MatrixGreen
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ThemeColors[if (_isSystemActive.value) _onColorKey.value else _offColorKey.value] ?: MatrixGreen
    )

    fun refreshState() {
        _isSystemActive.value = prefs.getBoolean("system_active", true)
        _isNotificEnabled.value = prefs.getBoolean("notifications_enabled", true)
        _hasPassword.value = !prefs.getString("app_password", null).isNullOrEmpty()
        _blockerText.value = prefs.getString("blocker_text", "ЛИМИТ ИСЧЕРПАН") ?: "ЛИМИТ ИСЧЕРПАН"
        _onColorKey.value = prefs.getString("color_on", "GREEN") ?: "GREEN"
        _offColorKey.value = prefs.getString("color_off", "RED") ?: "RED"
    }

    fun setOnColor(colorKey: String) {
        prefs.edit { putString("color_on", colorKey) }
        _onColorKey.value = colorKey
    }

    fun setOffColor(colorKey: String) {
        prefs.edit { putString("color_off", colorKey) }
        _offColorKey.value = colorKey
    }

//    fun setAppUnlocked(unlocked: Boolean) {
//        _isAppUnlocked.value = unlocked
//    }
    fun toggleNotifications() {
        val newState = !_isNotificEnabled.value
        _isNotificEnabled.value = newState
        prefs.edit { putBoolean("notifications_enabled", newState) }
    }

    fun savePassword(newPassword: String) {
        prefs.edit { putString("app_password", newPassword) }
        _hasPassword.value = newPassword.isNotEmpty()
        _isAppUnlocked.value = true
    }

    fun checkPassword(input: String): Boolean {
        val saved = prefs.getString("app_password", "")
        val isValid = saved == input
        if (isValid) {
            _isAppUnlocked.value = true
        }
        return isValid
    }

    fun clearPassword() {
        prefs.edit { remove("app_password") }
        _hasPassword.value = false
        _isAppUnlocked.value = false
    }

    fun updateBlockerText(newText: String) {
        if (newText.isNotBlank()) {
            prefs.edit { putString("blocker_text", newText) }
            _blockerText.value = newText
        }
    }

    fun toggleSystemState() {
        val newState = !_isSystemActive.value
        _isSystemActive.value = newState
        prefs.edit { putBoolean("system_active", newState) }

        val intent = Intent(getApplication(), MonitoringService::class.java)
        if (newState) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                getApplication<Application>().startForegroundService(intent)
            } else {
                getApplication<Application>().startService(intent)
            }
        } else {
            getApplication<Application>().stopService(intent)
        }
    }
}