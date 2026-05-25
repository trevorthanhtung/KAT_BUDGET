package com.katgr0up.katbudget.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.AppDatabase
import com.katgr0up.katbudget.data.local.entity.DebtEntity
import com.katgr0up.katbudget.data.local.entity.TransactionEntity
import com.katgr0up.katbudget.data.repository.TransactionRepository
import com.katgr0up.katbudget.utils.TagPrefix
import com.katgr0up.katbudget.utils.TxType
import com.katgr0up.katbudget.widget.KatBudgetWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class DebtViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val repository = TransactionRepository(AppDatabase.getDatabase(application).transactionDao())

    val allDebts: Flow<List<DebtEntity>> = repository.allDebts

    fun addOrUpdateDebt(
        id: Int = 0,
        personName: String,
        amount: Double,
        currency: String,
        type: String,
        note: String,
        dueDate: Long?,
        sourceName: String = "",
        includeInCashFlow: Boolean = true,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (personName.isBlank() || amount < 0.0) return
        viewModelScope.launch(Dispatchers.IO) {
            val existingDebt = if (id != 0) repository.allDebts.first().find { it.id == id } else null
            val effectiveTimestamp = existingDebt?.timestamp ?: timestamp
            val cleanType = type.trim()
            val cleanCurrency = normalizeCurrency(currency)
            val cleanPersonName = personName.trim()

            repository.insertDebt(
                DebtEntity(
                    id,
                    cleanPersonName,
                    amount,
                    cleanCurrency,
                    cleanType,
                    note.trim(),
                    effectiveTimestamp,
                    existingDebt?.isPaid ?: false,
                    dueDate,
                    existingDebt?.paidAmount ?: 0.0
                )
            )

            val openingTag = "${TagPrefix.DEBT_OPENING}$effectiveTimestamp"
            val transactionType = openingTransactionType(cleanType, includeInCashFlow)
            val transactionNote = openingTransactionNote(cleanType, cleanPersonName)
            val category = context.getString(R.string.vm_debt_cat_default)

            if (id == 0 && sourceName.isNotBlank()) {
                repository.insert(
                    TransactionEntity(
                        amount = amount,
                        type = transactionType,
                        category = category,
                        note = transactionNote,
                        timestamp = effectiveTimestamp,
                        sourceName = sourceName.trim(),
                        currency = cleanCurrency,
                        projectTag = openingTag
                    )
                )
            } else if (id != 0) {
                val linkedTransactions = repository.allTransactions.first()
                linkedTransactions
                    .firstOrNull { it.projectTag == openingTag }
                    ?.let { openingTransaction ->
                        repository.insert(
                            openingTransaction.copy(
                                amount = amount,
                                type = transactionType,
                                category = category,
                                note = transactionNote,
                                currency = cleanCurrency
                            )
                        )
                    }

                linkedTransactions
                    .filter { it.projectTag == "${TagPrefix.DEBT_ROLLBACK}$id" }
                    .forEach { debtTransaction ->
                        repository.insert(
                            debtTransaction.copy(
                                type = paymentTransactionType(cleanType, includeInCashFlow),
                                category = category,
                                currency = cleanCurrency
                            )
                        )
                    }
            }
            KatBudgetWidgetProvider.refreshAll(context)
        }
    }

    fun payDebt(debt: DebtEntity, payAmount: Double, sourceName: String = TransactionEntity.DEFAULT_SOURCE_NAME) {
        if (payAmount <= 0.0) return
        viewModelScope.launch(Dispatchers.IO) {
            val totalPaid = (debt.paidAmount + payAmount).coerceAtMost(debt.amount)
            repository.insertDebt(debt.copy(paidAmount = totalPaid, isPaid = totalPaid >= debt.amount))

            val includeInCashFlow = debtAffectsCashFlow(debt)
            val txType = paymentTransactionType(debt.type, includeInCashFlow)

            // Tự động nạp chuỗi động có kèm tham số tên người liên quan (%1$s) từ strings.xml
            val note = if (debt.type == DebtEntity.TYPE_DEBT) {
                context.getString(R.string.vm_debt_note_paid, debt.personName)
            } else {
                context.getString(R.string.vm_debt_note_collected, debt.personName)
            }

            val category = context.getString(R.string.vm_debt_cat_default)

            repository.insert(
                TransactionEntity(
                    amount = payAmount,
                    type = txType,
                    category = category,
                    note = note,
                    timestamp = System.currentTimeMillis(),
                    sourceName = sourceName.trim().ifEmpty { TransactionEntity.DEFAULT_SOURCE_NAME },
                    currency = normalizeCurrency(debt.currency),
                    projectTag = "${TagPrefix.DEBT_ROLLBACK}${debt.id}"
                )
            )
            KatBudgetWidgetProvider.refreshAll(context)
        }
    }

    fun deleteDebt(debt: DebtEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteTransactionsByTag("${TagPrefix.DEBT_OPENING}${debt.timestamp}")
        repository.deleteTransactionsByTag("${TagPrefix.DEBT_ROLLBACK}${debt.id}")
        repository.deleteDebt(debt)
        KatBudgetWidgetProvider.refreshAll(context)
    }

    private suspend fun debtAffectsCashFlow(debt: DebtEntity): Boolean {
        val openingTag = "${TagPrefix.DEBT_OPENING}${debt.timestamp}"
        val openingType = repository.allTransactions.first()
            .firstOrNull { it.projectTag == openingTag }
            ?.type

        return openingType == null || openingType == TxType.INCOME || openingType == TxType.EXPENSE
    }

    private fun openingTransactionType(debtType: String, includeInCashFlow: Boolean): String {
        return when {
            includeInCashFlow && debtType == DebtEntity.TYPE_DEBT -> TxType.INCOME
            includeInCashFlow -> TxType.EXPENSE
            debtType == DebtEntity.TYPE_DEBT -> TxType.BORROWING
            else -> TxType.LENDING
        }
    }

    private fun paymentTransactionType(debtType: String, includeInCashFlow: Boolean): String {
        return when {
            includeInCashFlow && debtType == DebtEntity.TYPE_DEBT -> TxType.EXPENSE
            includeInCashFlow -> TxType.INCOME
            debtType == DebtEntity.TYPE_DEBT -> TxType.DEBT_PAYMENT
            else -> TxType.DEBT_COLLECTION
        }
    }

    private fun openingTransactionNote(debtType: String, personName: String): String {
        return if (debtType == DebtEntity.TYPE_DEBT) {
            context.getString(R.string.vm_debt_note_borrowed_initial, personName)
        } else {
            context.getString(R.string.vm_debt_note_lent_initial, personName)
        }
    }

    private fun normalizeCurrency(c: String): String =
        when (c.trim().uppercase(Locale.ROOT)) { "VNĐ", "VND", "" -> "VND"; else -> c.trim().uppercase(Locale.ROOT) }
}
