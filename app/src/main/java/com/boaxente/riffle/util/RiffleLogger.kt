package com.example.riffle.util

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RiffleLogger @Inject constructor() {

    init {
        instance = this
        // Set default keys if needed, e.g.
        // setCustomKey("logger_initialized", true)
    }

    fun log(message: String) {
        FirebaseCrashlytics.getInstance().log(message)
    }

    fun recordException(t: Throwable) {
        if (t is retrofit2.HttpException) {
            try {
                // Extract URL safely
                val url = t.response()?.raw()?.request?.url?.toString()
                if (url != null) {
                    setCustomKey("request_url", url)
                }
                
                // Extract status code
                val code = t.code()
                setCustomKey("status_code", code)
            } catch (e: Exception) {
                // Failsafe to ensure original exception is still recorded even if extraction fails
                e.printStackTrace()
            }
        }
        FirebaseCrashlytics.getInstance().recordException(t)
    }

    fun setCustomKey(key: String, value: String) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Boolean) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Int) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Double) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }

    companion object {
        @Volatile
        private var instance: RiffleLogger? = null

        fun getInstance(): RiffleLogger {
             return instance ?: synchronized(this) {
                instance ?: RiffleLogger().also { instance = it }
            }
        }

        fun log(message: String) = getInstance().log(message)
        fun recordException(t: Throwable) = getInstance().recordException(t)
        fun setCustomKey(key: String, value: String) = getInstance().setCustomKey(key, value)
        fun setCustomKey(key: String, value: Boolean) = getInstance().setCustomKey(key, value)
        fun setCustomKey(key: String, value: Int) = getInstance().setCustomKey(key, value)
        fun setCustomKey(key: String, value: Double) = getInstance().setCustomKey(key, value)
    }
}
