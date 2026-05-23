package com.katgr0up.katbudget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.katgr0up.katbudget.MainActivity
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.AppDatabase
import com.katgr0up.katbudget.data.local.entity.TransactionEntity
import com.katgr0up.katbudget.managers.ExchangeRateManager
import com.katgr0up.katbudget.managers.PreferencesManager
import com.katgr0up.katbudget.ui.utils.convertCurrency
import com.katgr0up.katbudget.ui.utils.formatCompactCurrency
import com.katgr0up.katbudget.ui.utils.normalizeCurrency
import com.katgr0up.katbudget.utils.TxType
import java.util.Calendar
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
            val quickAddIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_QUICK_ADD, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                quickAddIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            return RemoteViews(context.packageName, R.layout.widget_layout).apply {
                setTextViewText(
                    R.id.widget_balance_value,
                    formatCompactCurrency(summary.balance, summary.currency)
                )
                setTextViewText(
                    R.id.widget_income_value,
                    formatCompactCurrency(summary.monthIncome, summary.currency)
                )
                setTextViewText(
                    R.id.widget_expense_value,
                    formatCompactCurrency(summary.monthExpense, summary.currency)
                )
                setOnClickPendingIntent(R.id.widget_btn_add, pendingIntent)
            }
        }

        private fun loadWidgetSummary(context: Context): WidgetSummary {
            return runBlocking(Dispatchers.IO) {
                runCatching {
                    val dao = AppDatabase.getDatabase(context).transactionDao()
                    val transactions = dao.getAllTransactions().first()
                    val sources = dao.getAllSources().first()
                    val defaultCurrency = normalizeCurrency(PreferencesManager(context).getDefaultCurrency())
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
                        currency = defaultCurrency
                    )
                }.getOrElse {
                    WidgetSummary(currency = normalizeCurrency(PreferencesManager(context).getDefaultCurrency()))
                }
            }
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
    val currency: String = "VND"
)
