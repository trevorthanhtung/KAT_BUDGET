package com.katgr0up.katbudget.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.AppDatabase
import com.katgr0up.katbudget.data.local.entity.SavingGoalEntity
import com.katgr0up.katbudget.data.local.entity.TransactionEntity
import com.katgr0up.katbudget.data.repository.TransactionRepository
import com.katgr0up.katbudget.managers.ExchangeRateManager
import com.katgr0up.katbudget.utils.TxType
import com.katgr0up.katbudget.widget.KatBudgetWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class GoalViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val repository = TransactionRepository(AppDatabase.getDatabase(application).transactionDao())

    val allGoals: Flow<List<SavingGoalEntity>> = repository.allGoals

    fun addGoal(name: String, target: Double, currency: String) {
        if (name.isBlank() || target <= 0.0) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertGoal(SavingGoalEntity(0, name.trim(), target, 0.0, normalizeCurrency(currency)))
        }
    }

    fun depositToGoal(goal: SavingGoalEntity, amount: Double, sourceName: String, currency: String) {
        if (amount <= 0.0) return
        viewModelScope.launch(Dispatchers.IO) {
            val latestGoals = repository.allGoals.first()
            val targetGoal = latestGoals.find { it.id == goal.id } ?: latestGoals.find { it.name == goal.name } ?: goal
            val depositCurrency = normalizeCurrency(currency)
            val contributionInGoalCurrency = convertAmount(
                amount = amount,
                fromCurrency = depositCurrency,
                toCurrency = targetGoal.currency
            )

            repository.updateGoal(
                targetGoal.copy(currentAmount = targetGoal.currentAmount + contributionInGoalCurrency)
            )

            if (sourceName.isNotBlank()) {
                // Tự động map text theo ngôn ngữ hệ thống bằng Resource ID
                val note = context.getString(R.string.vm_goal_note_deposit, targetGoal.name)
                val category = context.getString(R.string.vm_goal_cat_default)

                repository.insert(
                    TransactionEntity(
                        amount = amount,
                        type = TxType.GOAL_DEPOSIT,
                        category = category,
                        note = note,
                        timestamp = System.currentTimeMillis(),
                        sourceName = sourceName.trim(),
                        currency = depositCurrency,
                        projectTag = targetGoal.name
                    )
                )
            }
            KatBudgetWidgetProvider.refreshAll(context)
        }
    }

    fun deleteGoal(goal: SavingGoalEntity) = viewModelScope.launch(Dispatchers.IO) { repository.deleteGoal(goal) }

    private fun normalizeCurrency(c: String): String =
        when (c.trim().uppercase(Locale.ROOT)) { "VNĐ", "VND", "" -> "VND"; else -> c.trim().uppercase(Locale.ROOT) }

    private fun convertAmount(amount: Double, fromCurrency: String, toCurrency: String): Double {
        val from = normalizeCurrency(fromCurrency)
        val to = normalizeCurrency(toCurrency)
        if (from == to) return amount

        val rates = ExchangeRateManager.getRates()
        val sourceRate = rates[from] ?: 1.0
        val targetRate = rates[to] ?: 1.0
        if (sourceRate <= 0.0 || targetRate <= 0.0) return amount

        return amount * sourceRate / targetRate
    }
}
