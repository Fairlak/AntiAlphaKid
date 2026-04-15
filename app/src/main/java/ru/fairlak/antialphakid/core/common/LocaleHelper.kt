package ru.fairlak.antialphakid.core.common

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import java.util.Locale
import androidx.core.content.edit

object LocaleHelper {
    // В LocaleHelper.kt
    fun wrapContext(context: Context): Context {
        val prefs = context.getSharedPreferences("anti_alpha_prefs", Context.MODE_PRIVATE)
        val systemLang = if (Locale.getDefault().language == "ru") "ru" else "en"

        if (!prefs.contains("app_lang")) {
            prefs.edit { putString("app_lang", systemLang) }
        }

        val lang = prefs.getString("app_lang", systemLang) ?: systemLang
        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
fun Context.txt(@StringRes id: Int): String = getString(id)