package com.katgr0up.katbudget.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.katgr0up.katbudget.managers.ExchangeRateManager
import com.katgr0up.katbudget.managers.PreferencesManager
import com.katgr0up.katbudget.workers.AutoBackupWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsManager = PreferencesManager(application)

    private val _isEnglish = MutableStateFlow(prefsManager.getLanguageCode() == "en")
    val isEnglish: StateFlow<Boolean> = _isEnglish.asStateFlow()

    private val _defaultCurrency = MutableStateFlow(normalizeCurrency(prefsManager.getDefaultCurrency()))
    val defaultCurrency: StateFlow<String> = _defaultCurrency.asStateFlow()

    private val _hasPin = MutableStateFlow(prefsManager.getPin() != null)
    val hasPin: StateFlow<Boolean> = _hasPin.asStateFlow()

    private val _isAutoBackupEnabled = MutableStateFlow(prefsManager.isAutoBackupEnabled())
    val isAutoBackupEnabled: StateFlow<Boolean> = _isAutoBackupEnabled.asStateFlow()

    private val _autoBackupPath = MutableStateFlow(prefsManager.getAutoBackupPath())
    val autoBackupPath: StateFlow<String?> = _autoBackupPath.asStateFlow()

    private val _isPrivacyModeEnabled = MutableStateFlow(prefsManager.isPrivacyModeEnabled())
    val isPrivacyModeEnabled: StateFlow<Boolean> = _isPrivacyModeEnabled.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefsManager.isBiometricEnabled())
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    // --- BIẾN TRẠNG THÁI LƯU TỶ GIÁ ---
    private val _exchangeRates = MutableStateFlow(ExchangeRateManager.getRates())
    val exchangeRates: StateFlow<Map<String, Double>> = _exchangeRates.asStateFlow()

    private var lastBackgroundTime: Long = 0L

    // --- HÀM CẬP NHẬT TỶ GIÁ TỪ API ---
    fun fetchExchangeRates(context: Context) {
        viewModelScope.launch {
            _exchangeRates.value = ExchangeRateManager.fetchLiveRatesIfNeeded(context)
        }
    }

    fun setLanguage(isEng: Boolean) {
        prefsManager.setLanguageCode(if (isEng) "en" else "vi")
        _isEnglish.value = isEng
    }

    fun togglePrivacyMode(enabled: Boolean) {
        prefsManager.setPrivacyModeEnabled(enabled)
        _isPrivacyModeEnabled.value = enabled
    }

    fun toggleBiometric(enabled: Boolean) {
        val safeEnabled = enabled && prefsManager.getPin() != null
        prefsManager.setBiometricEnabled(safeEnabled)
        _isBiometricEnabled.value = safeEnabled
    }

    fun setDefaultCurrency(currency: String) {
        val norm = normalizeCurrency(currency)
        prefsManager.setDefaultCurrency(norm)
        _defaultCurrency.value = norm
    }

    fun setAutoBackupPath(path: String) {
        prefsManager.setAutoBackupPath(path)
        _autoBackupPath.value = path
    }

    fun toggleAutoBackup(enabled: Boolean) {
        prefsManager.setAutoBackupEnabled(enabled)
        _isAutoBackupEnabled.value = enabled
        val workManager = WorkManager.getInstance(getApplication())
        workManager.cancelUniqueWork(LEGACY_AUTO_BACKUP_WORK_NAME)

        if (enabled) {
            val backupWork = PeriodicWorkRequestBuilder<AutoBackupWorker>(1, TimeUnit.DAYS).build()
            workManager.enqueueUniquePeriodicWork(
                AUTO_BACKUP_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                backupWork
            )
        } else {
            workManager.cancelUniqueWork(AUTO_BACKUP_WORK_NAME)
        }
    }

    // --- HÀM KÍCH HOẠT SAO LƯU KHẨN CẤP THÔNG MINH ---
    fun triggerInstantBackup() {
        if (prefsManager.isAutoBackupEnabled() && !prefsManager.getAutoBackupPath().isNullOrBlank()) {
            val workManager = WorkManager.getInstance(getApplication())
            val instantBackupWork = OneTimeWorkRequestBuilder<AutoBackupWorker>().build()
            workManager.enqueue(instantBackupWork)
        }
    }

    fun savePin(pin: String) {
        if (pin.isNotBlank()) {
            prefsManager.savePin(pin)
            _hasPin.value = true
        }
    }

    fun getPin(): String? = prefsManager.getPin()

    fun clearPin() {
        prefsManager.clearPin()
        prefsManager.setBiometricEnabled(false)
        _hasPin.value = false
        _isBiometricEnabled.value = false
    }

    fun setAppBackgroundedTime() {
        lastBackgroundTime = System.currentTimeMillis()
    }

    fun shouldLockApp(): Boolean {
        if (prefsManager.getPin() == null || lastBackgroundTime == 0L) return false
        val lockTimeoutMillis = prefsManager.getLockTimer().coerceAtLeast(0) * 60 * 1000L
        return (System.currentTimeMillis() - lastBackgroundTime) >= lockTimeoutMillis
    }

    fun unlockApp() { lastBackgroundTime = 0L }

    private fun normalizeCurrency(c: String): String =
        when (c.trim().uppercase(Locale.ROOT)) { "VNĐ", "VND", "" -> "VND"; else -> c.trim().uppercase(Locale.ROOT) }

    companion object {
        private const val AUTO_BACKUP_WORK_NAME = "KatBudgetAutoBackup"
        // Keep only to cancel auto-backup jobs scheduled by old test builds.
        private const val LEGACY_AUTO_BACKUP_WORK_NAME = "KatWalletAutoBackup"
    }
}
