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

    fun isDarkMode(): Boolean {
        return sharedPreferences.getBoolean("dark_mode", false)
    }

    fun setDarkMode(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun isMarkAsReadOnScroll(): Boolean {
        return sharedPreferences.getBoolean("mark_on_scroll", false)
    }

    fun setMarkAsReadOnScroll(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("mark_on_scroll", enabled).apply()
    }
}
