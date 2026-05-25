package com.katgr0up.katbudget

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.katgr0up.katbudget.managers.PreferencesManager
import com.katgr0up.katbudget.ui.screens.DashboardScreen
import com.katgr0up.katbudget.ui.theme.KatBudgetTheme
import com.katgr0up.katbudget.ui.utils.LocalAppLanguageCode
import com.katgr0up.katbudget.viewmodel.DebtViewModel
import com.katgr0up.katbudget.viewmodel.GoalViewModel
import com.katgr0up.katbudget.viewmodel.SettingsViewModel
import com.katgr0up.katbudget.viewmodel.BudgetViewModel
import com.katgr0up.katbudget.workers.AutoBackupWorker
import kotlin.system.exitProcess

private const val TAG = "MainActivity"

class MainActivity : FragmentActivity() {
    private var shouldOpenQuickAdd by mutableStateOf(false)
    private var quickAddInitialType by mutableStateOf<String?>(null)

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            DailyReminderScheduler.schedule(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shouldOpenQuickAdd = intent?.getBooleanExtra(EXTRA_OPEN_QUICK_ADD, false) == true
        quickAddInitialType = intent?.getStringExtra(EXTRA_QUICK_ADD_TYPE)?.takeIf { it.isQuickAddType() }

        installEmergencyBackupCrashHandler()

        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        setContent {
            val context = LocalContext.current

            val sharedPreferences = remember(context) {
                context.getSharedPreferences(
                    "katbudget_prefs",
                    android.content.Context.MODE_PRIVATE
                )
            }

            val systemTheme = isSystemInDarkTheme()

            var isDarkTheme by remember {
                mutableStateOf(
                    sharedPreferences.getBoolean("IS_DARK_THEME", systemTheme)
                )
            }

            KatBudgetTheme(darkTheme = isDarkTheme) {
                val viewModel: BudgetViewModel = viewModel()
                val settingsViewModel: SettingsViewModel = viewModel()
                val debtViewModel: DebtViewModel = viewModel()
                val goalViewModel: GoalViewModel = viewModel()
                val languageCode by settingsViewModel.languageCode.collectAsState()


                CompositionLocalProvider(LocalAppLanguageCode provides languageCode) {
                    DashboardScreen(
                        viewModel = viewModel,
                        settingsViewModel = settingsViewModel,
                        debtViewModel = debtViewModel,
                        goalViewModel = goalViewModel,
                        isDarkTheme = isDarkTheme,
                        openQuickAddOnStart = shouldOpenQuickAdd,
                        quickAddInitialType = quickAddInitialType,
                        onQuickAddConsumed = {
                            shouldOpenQuickAdd = false
                            quickAddInitialType = null
                        },
                        onToggleTheme = {
                            val newTheme = !isDarkTheme
                            isDarkTheme = newTheme

                            sharedPreferences.edit {
                                putBoolean("IS_DARK_THEME", newTheme)
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_QUICK_ADD, false)) {
            shouldOpenQuickAdd = true
            quickAddInitialType = intent.getStringExtra(EXTRA_QUICK_ADD_TYPE)?.takeIf { it.isQuickAddType() }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            DailyReminderScheduler.schedule(this)
            return
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            DailyReminderScheduler.schedule(this)
        } else {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun installEmergencyBackupCrashHandler() {
        if (isEmergencyBackupCrashHandlerInstalled) return

        synchronized(MainActivity::class.java) {
            if (isEmergencyBackupCrashHandlerInstalled) return

            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                scheduleEmergencyBackupAfterCrash()
                defaultHandler?.uncaughtException(thread, throwable) ?: exitProcess(1)
            }

            isEmergencyBackupCrashHandlerInstalled = true
        }
    }

    private fun scheduleEmergencyBackupAfterCrash() {
        runCatching {
            val prefsManager = PreferencesManager(applicationContext)
            if (!prefsManager.isAutoBackupEnabled() || prefsManager.getAutoBackupPath().isNullOrBlank()) {
                return
            }

            val instantBackupWork = OneTimeWorkRequestBuilder<AutoBackupWorker>().build()
            WorkManager.getInstance(applicationContext).enqueue(instantBackupWork)
            Thread.sleep(400)
        }.onFailure { error ->
            Log.e(TAG, "Unable to schedule emergency backup after crash.", error)
        }
    }

    companion object {
        const val EXTRA_OPEN_QUICK_ADD = "OPEN_QUICK_ADD"
        const val EXTRA_QUICK_ADD_TYPE = "QUICK_ADD_TYPE"

        @Volatile
        private var isEmergencyBackupCrashHandlerInstalled = false
    }
}

private fun String.isQuickAddType(): Boolean = this == "EXPENSE" || this == "INCOME"
