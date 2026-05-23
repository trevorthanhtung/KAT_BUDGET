package com.katgr0up.katbudget.managers

import android.content.Context
import androidx.core.content.edit

class PreferencesManager(context: Context) {
    private val appContext = context.applicationContext
    private val sharedPrefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        migrateLegacyPreferences()
    }

    // Lưu mã ngôn ngữ chuẩn ISO thay vì cờ Boolean để tăng tính mở rộng khi thêm ngôn ngữ mới
    fun getLanguageCode(): String = sharedPrefs.getString(PREF_LANGUAGE_CODE, "vi") ?: "vi"
    fun setLanguageCode(langCode: String) = sharedPrefs.edit {
        putString(PREF_LANGUAGE_CODE, langCode)
    }

    fun getDefaultCurrency(): String = sharedPrefs.getString(PREF_DEFAULT_CURRENCY, "VND") ?: "VND"
    fun setDefaultCurrency(currency: String) = sharedPrefs.edit {
        putString(PREF_DEFAULT_CURRENCY, currency)
    }

    fun getLockTimer(): Int = sharedPrefs.getInt(PREF_LOCK_TIMER, 0)
    fun setLockTimer(minutes: Int) = sharedPrefs.edit {
        putInt(PREF_LOCK_TIMER, minutes)
    }

    fun getPin(): String? = sharedPrefs.getString(PREF_APP_PIN, null)
    fun savePin(pin: String) = sharedPrefs.edit {
        putString(PREF_APP_PIN, pin)
    }
    fun clearPin() = sharedPrefs.edit {
        remove(PREF_APP_PIN)
    }

    fun isAutoBackupEnabled(): Boolean = sharedPrefs.getBoolean(PREF_AUTO_BACKUP, false)
    fun setAutoBackupEnabled(enabled: Boolean) = sharedPrefs.edit {
        putBoolean(PREF_AUTO_BACKUP, enabled)
    }

    fun getAutoBackupPath(): String? = sharedPrefs.getString(PREF_AUTO_BACKUP_PATH, null)
    fun setAutoBackupPath(path: String) = sharedPrefs.edit {
        putString(PREF_AUTO_BACKUP_PATH, path)
    }

    fun isPrivacyModeEnabled(): Boolean = sharedPrefs.getBoolean(PREF_PRIVACY_MODE, false)
    fun setPrivacyModeEnabled(enabled: Boolean) = sharedPrefs.edit {
        putBoolean(PREF_PRIVACY_MODE, enabled)
    }

    fun isBiometricEnabled(): Boolean = sharedPrefs.getBoolean(PREF_BIOMETRIC_MODE, false)
    fun setBiometricEnabled(enabled: Boolean) = sharedPrefs.edit {
        putBoolean(PREF_BIOMETRIC_MODE, enabled)
    }

    private fun migrateLegacyPreferences() {
        val legacyPrefs = appContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        if (legacyPrefs.all.isEmpty()) return

        sharedPrefs.edit {
            legacyPrefs.all.forEach { (key, value) ->
                if (sharedPrefs.contains(key)) return@forEach

                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Float -> putFloat(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is String -> putString(key, value)
                }
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "katbudget_prefs"
        private const val LEGACY_PREFS_NAME = "kat_budget_prefs"
        private const val PREF_LANGUAGE_CODE = "app_language_code"
        private const val PREF_DEFAULT_CURRENCY = "default_currency"
        private const val PREF_LOCK_TIMER = "lock_timer"
        private const val PREF_APP_PIN = "app_pin"
        private const val PREF_AUTO_BACKUP = "auto_backup_enabled"
        private const val PREF_AUTO_BACKUP_PATH = "auto_backup_path"
        private const val PREF_PRIVACY_MODE = "privacy_mode_enabled"
        private const val PREF_BIOMETRIC_MODE = "biometric_mode_enabled"
    }
}
