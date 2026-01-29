package com.example.riffle.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _isDarkMode = kotlinx.coroutines.flow.MutableStateFlow(sharedPreferences.getBoolean("dark_mode", false))
    val isDarkModeFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _isDarkMode

    fun isDarkMode(): Boolean = _isDarkMode.value

    fun setDarkMode(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("dark_mode", enabled).apply()
        _isDarkMode.value = enabled
    }

    private val _isMarkAsReadOnScroll = kotlinx.coroutines.flow.MutableStateFlow(sharedPreferences.getBoolean("mark_on_scroll", false))
    val isMarkAsReadOnScrollFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _isMarkAsReadOnScroll

    fun isMarkAsReadOnScroll(): Boolean = _isMarkAsReadOnScroll.value

    fun setMarkAsReadOnScroll(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("mark_on_scroll", enabled).apply()
        _isMarkAsReadOnScroll.value = enabled
    }

    fun getGeminiApiKey(): String {
        return sharedPreferences.getString("gemini_api_key", "") ?: ""
    }

    fun setGeminiApiKey(key: String) {
        sharedPreferences.edit().putString("gemini_api_key", key).apply()
    }

    private val _language = kotlinx.coroutines.flow.MutableStateFlow(sharedPreferences.getString("language", "system") ?: "system")
    val languageFlow: kotlinx.coroutines.flow.StateFlow<String> = _language

    fun getLanguage(): String = _language.value

    fun setLanguage(language: String) {
        sharedPreferences.edit().putString("language", language).apply()
        _language.value = language
    }

    private val _syncInterval = kotlinx.coroutines.flow.MutableStateFlow(sharedPreferences.getLong("sync_interval", 1L))
    val syncIntervalFlow: kotlinx.coroutines.flow.StateFlow<Long> = _syncInterval

    fun getSyncInterval(): Long = _syncInterval.value

    fun setSyncInterval(hours: Long) {
        sharedPreferences.edit().putLong("sync_interval", hours).apply()
        _syncInterval.value = hours
    }
}
