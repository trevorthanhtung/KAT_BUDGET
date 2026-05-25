package com.katgr0up.katbudget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.katgr0up.katbudget.MainActivity
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.AppDatabase
import com.katgr0up.katbudget.data.local.entity.TransactionEntity
import com.katgr0up.katbudget.managers.ExchangeRateManager
import com.katgr0up.katbudget.managers.PreferencesManager
import com.katgr0up.katbudget.ui.utils.convertCurrency
import com.katgr0up.katbudget.ui.utils.formatDate
import com.katgr0up.katbudget.ui.utils.formatCompactCurrency
import com.katgr0up.katbudget.ui.utils.normalizeCurrency
import com.katgr0up.katbudget.utils.TxType
import java.util.Calendar
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class KatBudgetWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val summary = loadWidgetSummary(context)
        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(
                appWidgetId,
                buildRemoteViews(context, summary)
            )
        }
    }

    companion object {
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, KatBudgetWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                KatBudgetWidgetProvider().onUpdate(context, manager, ids)
            }
        }

        private fun buildRemoteViews(
            context: Context,
            summary: WidgetSummary
        ): RemoteViews {
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_APP
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_OPEN_APP,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val quickExpensePendingIntent = buildQuickAddPendingIntent(
                context,
                TxType.EXPENSE,
                REQUEST_QUICK_ADD_EXPENSE
            )
            val quickIncomePendingIntent = buildQuickAddPendingIntent(
                context,
                TxType.INCOME,
                REQUEST_QUICK_ADD_INCOME
            )

            return RemoteViews(context.packageName, R.layout.widget_layout).apply {
                setTextViewText(
                    R.id.widget_balance_value,
                    formatWidgetAmount(summary.balance, summary.currency, summary.isPrivacyModeEnabled)
                )
                setTextViewText(
                    R.id.widget_income_value,
                    formatWidgetAmount(summary.monthIncome, summary.currency, summary.isPrivacyModeEnabled)
                )
                setTextViewText(
                    R.id.widget_expense_value,
                    formatWidgetAmount(summary.monthExpense, summary.currency, summary.isPrivacyModeEnabled)
                )
                bindRecentTransaction(context, summary.recentTransaction, summary.isPrivacyModeEnabled)
                setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
                setOnClickPendingIntent(R.id.widget_btn_expense, quickExpensePendingIntent)
                setOnClickPendingIntent(R.id.widget_btn_income, quickIncomePendingIntent)
            }
        }

        private fun buildQuickAddPendingIntent(
            context: Context,
            type: String,
            requestCode: Int
        ): PendingIntent {
            val quickAddIntent = Intent(context, MainActivity::class.java).apply {
                action = "$ACTION_QUICK_ADD.$type"
                putExtra(MainActivity.EXTRA_OPEN_QUICK_ADD, true)
                putExtra(MainActivity.EXTRA_QUICK_ADD_TYPE, type)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            return PendingIntent.getActivity(
                context,
                requestCode,
                quickAddIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun loadWidgetSummary(context: Context): WidgetSummary {
            return runBlocking(Dispatchers.IO) {
                runCatching {
                    val dao = AppDatabase.getDatabase(context).transactionDao()
                    val transactions = dao.getAllTransactions().first()
                    val sources = dao.getAllSources().first()
                    val preferences = PreferencesManager(context)
                    val defaultCurrency = normalizeCurrency(preferences.getDefaultCurrency())
                    val isPrivacyModeEnabled = preferences.isPrivacyModeEnabled()
                    val rates = ExchangeRateManager.getRates()
                    val includedSources = sources
                        .filter { it.includeInTotal }
                        .map { it.name.trim() }
                        .toSet()
                    val hasSourceFilters = sources.isNotEmpty()
                    val monthRange = currentMonthRangeMillis()

                    var balance = 0.0
                    var monthIncome = 0.0
                    var monthExpense = 0.0
                    val openingBalanceLabels = setOf(
                        context.getString(R.string.fallback_opening_balance),
                        "Số dư ban đầu",
                        "Opening Balance"
                    )
                    val visibleTransactions = transactions.filter { tx ->
                        (!hasSourceFilters || tx.sourceName.trim() in includedSources) &&
                            tx.category !in openingBalanceLabels
                    }

                    transactions.forEach { tx ->
                        if (hasSourceFilters && tx.sourceName.trim() !in includedSources) return@forEach

                        val amount = convertToDefault(tx, defaultCurrency, rates)
                        when (tx.type) {
                            TxType.INCOME, TxType.TRANSFER_IN -> balance += amount
                            TxType.EXPENSE, TxType.TRANSFER_OUT -> balance -= amount
                        }

                        if (tx.timestamp in monthRange.first..monthRange.second) {
                            when (tx.type) {
                                TxType.INCOME -> monthIncome += amount
                                TxType.EXPENSE -> monthExpense += amount
                            }
                        }
                    }

                    WidgetSummary(
                        balance = balance,
                        monthIncome = monthIncome,
                        monthExpense = monthExpense,
                        recentTransaction = visibleTransactions
                            .filter { it.type.isWidgetRecentType() }
                            .maxByOrNull { it.timestamp }
                            ?.toWidgetRecent(),
                        currency = defaultCurrency,
                        isPrivacyModeEnabled = isPrivacyModeEnabled
                    )
                }.getOrElse {
                    val preferences = PreferencesManager(context)
                    WidgetSummary(
                        currency = normalizeCurrency(preferences.getDefaultCurrency()),
                        isPrivacyModeEnabled = preferences.isPrivacyModeEnabled()
                    )
                }
            }
        }

        private fun formatWidgetAmount(
            amount: Double,
            currency: String,
            isPrivacyModeEnabled: Boolean
        ): String {
            return if (isPrivacyModeEnabled) "***" else formatCompactCurrency(amount, currency)
        }

        private fun RemoteViews.bindRecentTransaction(
            context: Context,
            recentTransaction: WidgetRecentTransaction?,
            isPrivacyModeEnabled: Boolean
        ) {
            if (recentTransaction == null) {
                setTextViewText(R.id.widget_recent_title, context.getString(R.string.widget_no_recent))
                setTextViewText(R.id.widget_recent_subtitle, "")
                setTextViewText(R.id.widget_recent_amount, "")
                setViewVisibility(R.id.widget_recent_subtitle, View.GONE)
                return
            }

            setViewVisibility(R.id.widget_recent_subtitle, View.VISIBLE)
            setTextViewText(R.id.widget_recent_title, recentTransaction.title)
            setTextViewText(R.id.widget_recent_subtitle, recentTransaction.subtitle)
            setTextViewText(
                R.id.widget_recent_amount,
                if (isPrivacyModeEnabled) "***" else recentTransaction.amount
            )
            setTextColor(R.id.widget_recent_amount, recentTransaction.amountColor)
        }

        private fun TransactionEntity.toWidgetRecent(): WidgetRecentTransaction {
            val isNegative = type.isNegativeWidgetType()
            val prefix = if (isNegative) "-" else "+"
            return WidgetRecentTransaction(
                title = category,
                subtitle = "${sourceName.trim()} - ${formatDate(timestamp)}",
                amount = "$prefix${formatCompactCurrency(abs(amount), currency)}",
                amountColor = if (isNegative) WIDGET_EXPENSE_COLOR else WIDGET_INCOME_COLOR
            )
        }

        private fun String.isWidgetRecentType(): Boolean {
            return this == TxType.INCOME ||
                this == TxType.EXPENSE ||
                this == TxType.TRANSFER_IN ||
                this == TxType.TRANSFER_OUT
        }

        private fun String.isNegativeWidgetType(): Boolean {
            return this == TxType.EXPENSE || this == TxType.TRANSFER_OUT
        }

        private fun convertToDefault(
            transaction: TransactionEntity,
            defaultCurrency: String,
            rates: Map<String, Double>
        ): Double {
            return convertCurrency(
                amount = transaction.amount,
                from = transaction.currency,
                to = defaultCurrency,
                exchangeRates = rates
            )
        }

        private fun currentMonthRangeMillis(): Pair<Long, Long> {
            val start = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val end = (start.clone() as Calendar).apply {
                add(Calendar.MONTH, 1)
                add(Calendar.MILLISECOND, -1)
            }
            return start.timeInMillis to end.timeInMillis
        }
    }
}

private data class WidgetSummary(
    val balance: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val recentTransaction: WidgetRecentTransaction? = null,
    val currency: String = "VND",
    val isPrivacyModeEnabled: Boolean = false
)

private data class WidgetRecentTransaction(
    val title: String,
    val subtitle: String,
    val amount: String,
    val amountColor: Int
)

private const val ACTION_OPEN_APP = "com.katgr0up.katbudget.widget.OPEN_APP"
private const val ACTION_QUICK_ADD = "com.katgr0up.katbudget.widget.QUICK_ADD"
private const val REQUEST_OPEN_APP = 1000
private const val REQUEST_QUICK_ADD_EXPENSE = 1001
private const val REQUEST_QUICK_ADD_INCOME = 1002
private val WIDGET_INCOME_COLOR = Color.parseColor("#16A34A")
private val WIDGET_EXPENSE_COLOR = Color.parseColor("#EF4444")
