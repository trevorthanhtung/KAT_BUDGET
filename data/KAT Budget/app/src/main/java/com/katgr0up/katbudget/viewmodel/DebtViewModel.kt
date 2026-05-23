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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class DebtViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val repository = TransactionRepository(AppDatabase.getDatabase(application).transactionDao())

    val allDebts: Flow<List<DebtEntity>> = repository.allDebts

    fun addOrUpdateDebt(id: Int = 0, personName: String, amount: Double, currency: String, type: String, note: String, dueDate: Long?, timestamp: Long = System.currentTimeMillis()) {
        if (personName.isBlank() || amount < 0.0) return
        viewModelScope.launch(Dispatchers.IO) {
            val existingDebt = if (id != 0) repository.allDebts.first().find { it.id == id } else null
            repository.insertDebt(DebtEntity(id, personName.trim(), amount, normalizeCurrency(currency), type.trim(), note.trim(), timestamp, existingDebt?.isPaid ?: false, dueDate, existingDebt?.paidAmount ?: 0.0))
        }
    }

    fun payDebt(debt: DebtEntity, payAmount: Double, sourceName: String = TransactionEntity.DEFAULT_SOURCE_NAME) {
        if (payAmount <= 0.0) return
        viewModelScope.launch(Dispatchers.IO) {
            val totalPaid = (debt.paidAmount + payAmount).coerceAtMost(debt.amount)
            repository.insertDebt(debt.copy(paidAmount = totalPaid, isPaid = totalPaid >= debt.amount))

            val txType = if (debt.type == DebtEntity.TYPE_DEBT) TxType.EXPENSE else TxType.INCOME

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
        }
    }

    fun deleteDebt(debt: DebtEntity) = viewModelScope.launch(Dispatchers.IO) { repository.deleteDebt(debt) }

    private fun normalizeCurrency(c: String): String =
        when (c.trim().uppercase(Locale.ROOT)) { "VNĐ", "VND", "" -> "VND"; else -> c.trim().uppercase(Locale.ROOT) }
}