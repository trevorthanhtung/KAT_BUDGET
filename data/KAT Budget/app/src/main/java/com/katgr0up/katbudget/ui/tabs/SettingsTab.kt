package com.katgr0up.katbudget.ui.tabs

import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.managers.AppUpdater
import com.katgr0up.katbudget.ui.components.ChoiceChip
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.ui.dialogs.*
import com.katgr0up.katbudget.ui.utils.katStringResource
import com.katgr0up.katbudget.ui.utils.normalizeCurrency
import kotlinx.coroutines.launch

@Composable
fun SettingsTab(
    isEng: Boolean,
    isDarkTheme: Boolean,
    defaultCurrency: String,
    hasPinSetup: Boolean,
    isBiometricEnabled: Boolean,
    isPrivacyModeEnabled: Boolean,
    isAutoBackupEnabled: Boolean,
    autoBackupPath: String?,
    colors: BudgetColors,
    availableCurrencies: List<String>,
    exchangeRates: Map<String, Double>,
    onCurrencyChanged: (String) -> Unit,
    onBudgetClick: () -> Unit,
    onRecurringClick: () -> Unit,
    onGoalClick: () -> Unit,
    onExportCsv: () -> Unit,
    onBackup: () -> Unit,
    onAutoBackupToggle: (Boolean) -> Unit,
    onSelectBackupFolder: () -> Unit,
    onRestore: () -> Unit,
    onPinToggle: (Boolean) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onPrivacyToggle: (Boolean) -> Unit,
    onThemeToggle: (Boolean) -> Unit,
    onSupportClick: () -> Unit,
    onAboutClick: () -> Unit,
    onDonateClick: () -> Unit,
    onRefreshRates: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showExchangeRateDialog by remember { mutableStateOf(false) }
    var isCheckingForUpdate by remember { mutableStateOf(false) }
    var showAvailableDialog by remember { mutableStateOf(false) }
    var showNoticeDialog by remember { mutableStateOf(false) }
    var isNoticeError by remember { mutableStateOf(false) }

    var apkUrl by remember { mutableStateOf("") }
    var versionName by remember { mutableStateOf("") }
    var releaseNotes by remember { mutableStateOf("") }

    val fallbackNew = katStringResource(id = R.string.fallback_version_new, isEng = isEng)
    val biometricStatus = remember(context) {
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
    }
    val isBiometricAvailable = biometricStatus == BiometricManager.BIOMETRIC_SUCCESS
    val canEnableBiometric = hasPinSetup && isBiometricAvailable
    val biometricChecked = isBiometricEnabled && canEnableBiometric
    val biometricSubtitle = when {
        !hasPinSetup -> katStringResource(id = R.string.settings_item_biometric_sub_need_pin, isEng = isEng)
        !isBiometricAvailable -> katStringResource(id = R.string.settings_item_biometric_sub_unavailable, isEng = isEng)
        else -> katStringResource(id = R.string.settings_item_biometric_sub, isEng = isEng)
    }

    LaunchedEffect(canEnableBiometric, isBiometricEnabled) {
        if (isBiometricEnabled && !canEnableBiometric) {
            onBiometricToggle(false)
        }
    }

    fun checkForUpdates() {
        if (isCheckingForUpdate) return
        isCheckingForUpdate = true

        coroutineScope.launch {
            val result = AppUpdater.checkForUpdate(
                context = context,
                fallbackVersionName = fallbackNew
            )
            isCheckingForUpdate = false

            when (result) {
                is AppUpdater.UpdateCheckResult.Available -> {
                    apkUrl = result.update.apkUrl
                    versionName = result.update.versionName
                    releaseNotes = result.update.releaseNotes
                    showAvailableDialog = true
                }
                AppUpdater.UpdateCheckResult.UpToDate -> {
                    isNoticeError = false
                    showNoticeDialog = true
                }
                is AppUpdater.UpdateCheckResult.Failed -> {
                    isNoticeError = true
                    showNoticeDialog = true
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 112.dp)
    ) {
        item {
            SettingsSectionTitle(
                title = katStringResource(id = R.string.settings_section_currency, isEng = isEng),
                colors = colors
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(availableCurrencies, key = { it }) { currency ->
                    ChoiceChip(
                        text = currency,
                        selected = normalizeCurrency(defaultCurrency) == currency,
                        colors = colors,
                        onClick = { onCurrencyChanged(currency) }
                    )
                }
            }
        }

        item {
            SettingsSectionTitle(
                title = katStringResource(id = R.string.settings_section_tools, isEng = isEng),
                colors = colors
            )

            SettingsGroup(colors) {
                SettingsRow(
                    title = katStringResource(id = R.string.settings_item_budget_title, isEng = isEng),
                    subtitle = katStringResource(id = R.string.settings_item_budget_sub, isEng = isEng),
                    colors = colors,
                    onClick = onBudgetClick
                )
                SettingsDivider(colors)

                SettingsRow(
                    title = katStringResource(id = R.string.settings_item_recurring_title, isEng = isEng),
                    subtitle = katStringResource(id = R.string.settings_item_recurring_sub, isEng = isEng),
                    colors = colors,
                    onClick = onRecurringClick
                )
                SettingsDivider(colors)

                SettingsRow(
                    title = katStringResource(id = R.string.settings_item_goals_title, isEng = isEng),
                    subtitle = katStringResource(id = R.string.settings_item_goals_sub, isEng = isEng),
                    colors = colors,
                    onClick = onGoalClick
                )
                SettingsDivider(colors)

                SettingsRow(
                    title = katStringResource(id = R.string.settings_item_rates_title, isEng = isEng),
                    subtitle = katStringResource(id = R.string.settings_item_rates_sub, isEng = isEng),
                    colors = colors,
                    onClick = {
                        onRefreshRates()
                        showExchangeRateDialog = true
                    }
                )
                SettingsDivider(colors)

                SettingsRow(
                    title = katStringResource(id = R.string.settings_item_csv_title, isEng = isEng),
                    subtitle = katStringResource(id = R.string.settings_item_csv_sub, isEng = isEng),
                    colors = colors,
                    onClick = onExportCsv
                )
            }
        }

        item {
            SettingsSectionTitle(
                title = katStringResource(id = R.string.settings_section_security, isEng = isEng),
                colors = colors
            )

            SettingsGroup(colors) {
                SettingsSwitchRow(
                    title = katStringResource(id = R.string.settings_item_lock_title, isEng = isEng),
                    subtitle = katStringResource(id = R.string.settings_item_lock_sub, isEng = isEng),
                    checked = hasPinSetup,
                    colors = colors,
                    onRowClick = null,
                    onCheckedChange = onPinToggle
                )
                SettingsDivider(colors)
 
                SettingsSwitchRow(
                    title = katStringResource(id = R.string.settings_item_biometric_title, isEng = isEng),
                    subtitle = biometricSubtitle,
                    checked = biometricChecked,
                    colors = colors,
                    onRowClick = null,
                    onCheckedChange = onBiometricToggle,
                    enabled = canEnableBiometric
                )
                SettingsDivider(colors)

                SettingsSwitchRow(
                    title = katStringResource(id = R.string.settings_item_privacy_title, isEng = isEng),
                    subtitle = katStringResource(id = R.string.settings_item_privacy_sub, isEng = isEng),
                    checked = isPrivacyModeEnabled,
                    colors = colors,
                    onRowClick = null,
                    onCheckedChange = onPrivacyToggle
                )
                SettingsDivider(colors)

                SettingsSwitchRow(
                    title = katStringResource(id = R.string.settings_item_theme_title, isEng = isEng),
                    subtitle = katStringResource(id = R.string.settings_item_theme_sub, isEng = isEng),
                    checked = isDarkTheme,
                    colors = colors,
                    onRowClick = null,
                    onCheckedChange = onThemeToggle
                )
            }
        }

        item {
            SettingsSectionTitle(
                title = katStringResource(id = R.string.settings_section_sync, isEng = isEng),
                colors = colors
            )

            SettingsGroup(colors) {
                val autoBackupSubtitle = if (autoBackupPath.isNullOrBlank()) {
                    katStringResource(id = R.string.settings_folder_hint_select, isEng = isEng)
                } else {
                    katStringResource(id = R.string.settings_folder_hint_selected, isEng = isEng)
                }

                SettingsSwitchRow(
                    title = katStringResource(id = R.string.settings_item_auto_backup, isEng = isEng),
                    subtitle = autoBackupSubtitle,
                    checked = isAutoBackupEnabled,
                    colors = colors,
                    onRowClick = onSelectBackupFolder,
                    onCheckedChange = { enabled ->
                        if (enabled && autoBackupPath.isNullOrBlank()) {
                            onSelectBackupFolder()
                        } else {
                            onAutoBackupToggle(enabled)
                        }
                    }
                )
                SettingsDivider(colors)

                SettingsRow(
                    title = katStringResource(id = R.string.settings_item_manual_backup_title, isEng = isEng),
                    subtitle = katStringResource(id = R.string.settings_item_manual_backup_sub, isEng = isEng),
                    colors = colors,
                    onClick = onBackup
                )
                SettingsDivider(colors)

                SettingsRow(
                    title = katStringResource(id = R.string.settings_item_restore_title, isEng = isEng),
                    subtitle = katStringResource(id = R.string.settings_item_restore_sub, isEng = isEng),
                    colors = colors,
                    onClick = onRestore
                )
            }
        }

        item {
            SettingsSectionTitle(
                title = katStringResource(id = R.string.settings_section_info, isEng = isEng),
                colors = colors
            )

            SettingsGroup(colors) {
                SettingsRow(
                    title = katStringResource(id = R.string.settings_item_support_title, isEng = isEng),
                    subtitle = katStringResource(id = R.string.settings_item_support_sub, isEng = isEng),
                    colors = colors,
                    onClick = onSupportClick
                )
                SettingsDivider(colors)

                SettingsRow(
                    title = katStringResource(id = R.string.settings_item_about_title, isEng = isEng),
                    subtitle = katStringResource(id = R.string.settings_item_about_sub, isEng = isEng),
                    colors = colors,
                    onClick = onAboutClick
                )
                SettingsDivider(colors)

                val updateSubtitle = if (isCheckingForUpdate) {
                    katStringResource(id = R.string.settings_item_update_sub_loading, isEng = isEng)
                } else {
                    katStringResource(id = R.string.settings_item_update_sub_ready, isEng = isEng)
                }

                SettingsRow(
                    title = katStringResource(id = R.string.settings_item_update_title, isEng = isEng),
                    subtitle = updateSubtitle,
                    colors = colors,
                    enabled = !isCheckingForUpdate,
                    onClick = ::checkForUpdates
                )
                SettingsDivider(colors)

                SettingsRow(
                    title = katStringResource(id = R.string.settings_item_donate_title, isEng = isEng),
                    subtitle = katStringResource(id = R.string.settings_item_donate_sub, isEng = isEng),
                    colors = colors,
                    onClick = onDonateClick
                )
            }
        }
    }

    if (showExchangeRateDialog) {
        ExchangeRateDialog(
            isEng = isEng,
            colors = colors,
            exchangeRates = exchangeRates,
            onDismiss = { showExchangeRateDialog = false }
        )
    }

    if (isCheckingForUpdate) {
        UpdateLoadingDialog(isEng = isEng, colors = colors)
    }

    if (showAvailableDialog) {
        UpdateAvailableDialog(
            isEng = isEng,
            colors = colors,
            versionName = versionName,
            releaseNotes = releaseNotes,
            onDismiss = { showAvailableDialog = false },
            onConfirm = {
                showAvailableDialog = false
                AppUpdater.downloadApk(context, apkUrl)
            }
        )
    }

    if (showNoticeDialog) {
        SettingsUpdateNoticeDialog(
            isEng = isEng,
            colors = colors,
            isError = isNoticeError,
            onDismiss = { showNoticeDialog = false }
        )
    }
}
