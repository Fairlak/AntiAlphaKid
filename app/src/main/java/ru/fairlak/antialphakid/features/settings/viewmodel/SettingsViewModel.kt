package ru.fairlak.antialphakid.features.settings.viewmodel

import android.app.Application
import android.content.Context
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

    val activeColor: StateFlow<Color> = combine(isSystemActive, _onColorKey, _offColorKey) { active, onKey, offKey ->
        val key = if (active) onKey else offKey
        ThemeColors[key] ?: MatrixGreen
    }.stateIn(viewModelScope,
        SharingStarted.WhileSubscribed(5000), MatrixGreen)

    fun refreshState() {
        _isSystemActive.value = prefs.getBoolean("system_active", true)
        _isNotificEnabled.value = prefs.getBoolean("notifications_enabled", true)
        _hasPassword.value = !prefs.getString("app_password", null).isNullOrEmpty()
        _blockerText.value = prefs.getString("blocker_text", "ЛИМИТ ИСЧЕРПАН") ?: "ЛИМИТ ИСЧЕРПАН"
        _onColorKey.value = prefs.getString("color_on", "GREEN") ?: "GREEN"
        _offColorKey.value = prefs.getString("color_off", "RED") ?: "RED"
    }

    fun setOnColor(colorKey: String) {
        prefs.edit().putString("color_on", colorKey).apply()
        _onColorKey.value = colorKey
    }

    fun setOffColor(colorKey: String) {
        prefs.edit().putString("color_off", colorKey).apply()
        _offColorKey.value = colorKey
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

    fun updateBlockerText(newText: String) {
        if (newText.isNotBlank()) {
            prefs.edit().putString("blocker_text", newText).apply()
            _blockerText.value = newText
        }
    }
}