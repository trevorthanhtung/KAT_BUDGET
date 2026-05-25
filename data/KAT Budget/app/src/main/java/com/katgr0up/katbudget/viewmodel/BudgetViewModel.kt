package com.katgr0up.katbudget.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.AppDatabase
import com.katgr0up.katbudget.data.local.entity.BudgetEntity
import com.katgr0up.katbudget.data.local.entity.CategoryEntity
import com.katgr0up.katbudget.data.local.entity.DebtEntity
import com.katgr0up.katbudget.data.local.entity.RecurringEntity
import com.katgr0up.katbudget.data.local.entity.SavingGoalEntity
import com.katgr0up.katbudget.data.local.entity.SourceEntity
import com.katgr0up.katbudget.data.local.entity.TransactionEntity
import com.katgr0up.katbudget.data.local.type.CategoryType
import com.katgr0up.katbudget.data.repository.TransactionRepository
import com.katgr0up.katbudget.managers.*
import com.katgr0up.katbudget.utils.TagPrefix
import com.katgr0up.katbudget.utils.TxType
import com.katgr0up.katbudget.widget.KatBudgetWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToLong

private const val TAG = "BudgetViewModel"

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val repository = TransactionRepository(AppDatabase.getDatabase(application).transactionDao())

    private val _selectedTag = MutableStateFlow(context.getString(R.string.fallback_all))
    val selectedTag: StateFlow<String> = _selectedTag.asStateFlow()

    private val _exchangeRates = MutableStateFlow(ExchangeRateManager.getRates())
    val exchangeRates: StateFlow<Map<String, Double>> = _exchangeRates.asStateFlow()

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<TransactionEntity>> = repository.recentTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSources: StateFlow<List<SourceEntity>> = repository.allSources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBudgets: StateFlow<List<BudgetEntity>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecurrings: StateFlow<List<RecurringEntity>> = repository.allRecurrings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenseCategories: StateFlow<List<CategoryEntity>> = repository.expenseCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomeCategories: StateFlow<List<CategoryEntity>> = repository.incomeCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasSources: StateFlow<Boolean> = allSources
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val expenseCategoryNames: Flow<List<String>> =
        expenseCategories.map { listOf(context.getString(R.string.fallback_all)) + it.map { cat -> cat.name } }

    val incomeCategoryNames: Flow<List<String>> =
        incomeCategories.map { listOf(context.getString(R.string.fallback_all)) + it.map { cat -> cat.name } }

    val filteredTransactions: Flow<List<TransactionEntity>> =
        combine(allTransactions, selectedTag) { txs, tag ->
            val allTag = context.getString(R.string.fallback_all)
            if (tag == allTag || tag == "Tất cả" || tag == "All") txs else txs.filter { it.category == tag }
        }

    val sourceBalances: Flow<Map<String, Map<String, Double>>> =
        combine(allTransactions, allSources) { txs, sources ->
            buildSourceBalances(txs, sources)
        }

    fun getCurrentBalance(defaultCurrency: String): Flow<Map<String, Double>> =
        combine(sourceBalances, allSources) { balancesBySource, sources ->
            val excludedSources = sources.filterNot { it.includeInTotal }.map { it.name.trim() }.toSet()
            val totals = mutableMapOf<String, Double>()
            balancesBySource.forEach { (sourceName, balancesByCurrency) ->
                if (sourceName.trim() !in excludedSources) {
                    balancesByCurrency.forEach { (currency, amount) ->
                        val normCurr = normalizeCurrency(currency)
                        totals[normCurr] = (totals[normCurr] ?: 0.0) + amount
                    }
                }
            }
            totals
        }

    fun getGrandTotalConverted(defaultCurrency: String): Flow<Double> =
        combine(getCurrentBalance(defaultCurrency), exchangeRates) { balances, rates ->
            val targetRate = rates[normalizeCurrency(defaultCurrency)] ?: 1.0
            balances.entries.sumOf { (currency, amount) ->
                amount * (rates[normalizeCurrency(currency)] ?: 1.0) / targetRate
            }
        }

    init {
        insertDefaultCategories()
        checkAndExecuteRecurrings()
        viewModelScope.launch {
            _exchangeRates.value = ExchangeRateManager.fetchLiveRatesIfNeeded(getApplication())
        }
    }

    fun setFilterTag(tag: String) {
        _selectedTag.value = tag.ifBlank { context.getString(R.string.fallback_all) }
    }

    // --- Logic quản lý giao dịch cơ bản ---
    fun addCategory(name: String, type: String) {
        val safeType = type.trim().uppercase(Locale.ROOT)
        if (name.isBlank() || !CategoryType.isValid(safeType)) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCategory(
                CategoryEntity(
                    name = name.trim(),
                    emoji = "",
                    type = safeType
                )
            )
        }
    }

    fun deleteCategory(name: String, type: String) {
        val safeName = name.trim()
        val defaultCategories = context.resources.getStringArray(R.array.default_expense_categories).toList() +
                context.resources.getStringArray(R.array.default_income_categories).toList() +
                listOf(
                    context.getString(R.string.fallback_other), context.getString(R.string.fallback_other_income), context.getString(R.string.fallback_all),
                    "Khác", "Other", "Thu nhập khác", "Other Income", "Tất cả", "All"
                )

        if (defaultCategories.contains(safeName)) return

        viewModelScope.launch(Dispatchers.IO) {
            val currentCategories = if (type == TxType.EXPENSE) repository.expenseCategories.firstOrNull() else repository.incomeCategories.firstOrNull()
            currentCategories?.find { it.name == safeName }?.let { repository.deleteCategory(it) }
        }
    }

    fun addOrUpdateTransaction(id: Int = 0, amount: Double, type: String, category: String, note: String, sourceName: String, currency: String, timestamp: Long = System.currentTimeMillis(), imageUri: String? = null) {
        if (amount < 0.0) return
        viewModelScope.launch(Dispatchers.IO) {
            val existing = id.takeIf { it > 0 }?.let { repository.getTransactionById(it) }
            val cleanType = type.trim()
            val transaction = TransactionEntity(
                id = id,
                amount = amount,
                type = cleanType,
                category = category.trim()
                    .ifEmpty { context.getString(R.string.fallback_other) },
                note = note.trim(),
                timestamp = timestamp,
                projectTag = existing?.projectTag.takeIf {
                    existing != null && shouldKeepProjectTag(existing, cleanType)
                },
                sourceName = sourceName.trim()
                    .ifEmpty { TransactionEntity.DEFAULT_SOURCE_NAME },
                currency = normalizeCurrency(currency),
                imageUri = imageUri
            )

            if (existing != null) {
                syncLinkedProgressOnTransactionUpdate(existing, transaction)
            }

            repository.insert(transaction)
            KatBudgetWidgetProvider.refreshAll(context)
        }
    }

    fun transferMoney(amount: Double, fromSource: String, toSource: String, currency: String, note: String, timestamp: Long) {
        if (amount <= 0.0) return
        viewModelScope.launch(Dispatchers.IO) {
            val transferTag = "${TagPrefix.TRANSFER}$timestamp"
            val suffix = if (note.trim().isBlank()) "" else " (${note.trim()})"

            val catTransfer = context.getString(R.string.fallback_transfer)
            val noteTo = context.getString(R.string.vm_budget_transfer_to, toSource.trim()) + suffix
            val noteFrom = context.getString(R.string.vm_budget_transfer_from, fromSource.trim()) + suffix

            repository.insert(
                TransactionEntity(
                    amount = amount,
                    type = TxType.TRANSFER_OUT,
                    category = catTransfer,
                    note = noteTo,
                    timestamp = timestamp,
                    sourceName = fromSource.trim(),
                    currency = normalizeCurrency(currency),
                    projectTag = transferTag
                )
            )
            repository.insert(
                TransactionEntity(
                    amount = amount,
                    type = TxType.TRANSFER_IN,
                    category = catTransfer,
                    note = noteFrom,
                    timestamp = timestamp + 1,
                    sourceName = toSource.trim(),
                    currency = normalizeCurrency(currency),
                    projectTag = transferTag
                )
            )
            KatBudgetWidgetProvider.refreshAll(context)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (deleteLinkedDebtOpening(transaction)) {
                KatBudgetWidgetProvider.refreshAll(context)
                return@launch
            }

            if (transaction.type == TxType.GOAL_DEPOSIT) rollbackGoalDeposit(transaction)
            if (isDebtLinkedTransaction(transaction)) rollbackDebtPayment(transaction)

            val tag = transaction.projectTag ?: ""
            if (tag.isNotEmpty() && tag.startsWith(TagPrefix.TRANSFER)) {
                repository.deleteTransactionsByTag(tag)
            } else {
                repository.delete(transaction)
            }
            KatBudgetWidgetProvider.refreshAll(context)
        }
    }

    // --- Logic quản lý Nguồn tiền (Sources) ---
    fun addOrUpdateSource(existingSource: SourceEntity?, name: String, type: String, includeInTotal: Boolean, interestRate: Double = 0.0, interestPeriod: String = SourceEntity.InterestPeriod.NONE) {
        if (name.trim().isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val src = SourceEntity(
                existingSource?.id ?: 0,
                name.trim(),
                type.trim(),
                includeInTotal,
                interestRate.coerceAtLeast(0.0),
                interestPeriod.trim().ifEmpty { SourceEntity.InterestPeriod.NONE },
                existingSource?.createdTimestamp ?: System.currentTimeMillis()
            )
            if (existingSource != null) repository.updateSource(src) else repository.insertSource(src)
            KatBudgetWidgetProvider.refreshAll(context)
        }
    }

    fun deleteSource(source: SourceEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteTransactionsBySourceName(
            sourceName = source.name,
            transferTagPattern = "${TagPrefix.TRANSFER}%"
        )
        repository.deleteSource(source)
        KatBudgetWidgetProvider.refreshAll(context)
    }

    // --- Logic Ngân sách & Định kỳ ---
    fun addBudget(category: String, amount: Double, currency: String) {
        if (category.isBlank() || amount < 0.0) return
        viewModelScope.launch(Dispatchers.IO) {
            val categoryId = findExpenseCategoryId(category) ?: return@launch
            repository.insertBudget(
                BudgetEntity(
                    categoryId = categoryId,
                    limitAmountMinor = toMinorAmount(amount, currency),
                    currencyCode = normalizeCurrency(currency),
                    monthYear = currentMonthYear()
                )
            )
        }
    }

    fun updateBudget(id: Int, category: String, amount: Double, currency: String) {
        if (category.isBlank() || amount < 0.0) return
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.allBudgets.firstOrNull()?.find { it.id == id }
            val categoryId = findExpenseCategoryId(category) ?: return@launch
            if (existing != null) {
                repository.updateBudget(
                    existing.copy(
                        categoryId = categoryId,
                        limitAmountMinor = toMinorAmount(amount, currency),
                        currencyCode = normalizeCurrency(currency)
                    )
                )
            }
        }
    }

    fun deleteBudget(budget: BudgetEntity) = viewModelScope.launch(Dispatchers.IO) { repository.deleteBudget(budget) }

    fun addRecurring(amount: Double, type: String, category: String, note: String, sourceName: String, currency: String, day: Int) {
        if (amount < 0.0 || category.isBlank() || sourceName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertRecurring(
                RecurringEntity(
                    amount = amount,
                    type = type.trim(),
                    category = category.trim(),
                    note = note.trim(),
                    sourceName = sourceName.trim(),
                    currency = normalizeCurrency(currency),
                    dayOfMonth = day.coerceIn(1, 31)
                )
            )
        }
    }

    fun updateRecurring(id: Int, amount: Double, type: String, category: String, note: String, sourceName: String, currency: String, day: Int) {
        if (amount < 0.0 || category.isBlank() || sourceName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.allRecurrings.firstOrNull()?.find { it.id == id }
            if (existing != null) {
                repository.updateRecurring(
                    existing.copy(
                        amount = amount,
                        type = type.trim(),
                        category = category.trim(),
                        note = note.trim(),
                        sourceName = sourceName.trim(),
                        currency = normalizeCurrency(currency),
                        dayOfMonth = day.coerceIn(1, 31)
                    )
                )
            }
        }
    }

    fun deleteRecurring(recurring: RecurringEntity) = viewModelScope.launch(Dispatchers.IO) { repository.deleteRecurring(recurring) }

    // --- Backup & Restore ---
    suspend fun exportFullBackupData(
        cS: List<SourceEntity>, cT: List<TransactionEntity>, cD: List<DebtEntity>,
        cB: List<BudgetEntity>, cR: List<RecurringEntity>, cG: List<SavingGoalEntity>
    ): ByteArray {
        val expenseCats = repository.expenseCategories.firstOrNull() ?: emptyList()
        val incomeCats = repository.incomeCategories.firstOrNull() ?: emptyList()
        val allCustomCategories = expenseCats + incomeCats

        return BackupManager.exportBackupDataBytes(cS, cT, cD, cB, cR, cG, allCustomCategories)
    }

    fun importBackupDataFromBytes(bytes: ByteArray, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                BackupManager.restoreDataFromBytes(bytes, repository)
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (error: Exception) {
                Log.e(TAG, "Unable to import backup data.", error)
                withContext(Dispatchers.Main) { onError() }
            }
        }
    }

    // --- Private helpers ---
    private suspend fun syncLinkedProgressOnTransactionUpdate(
        oldTransaction: TransactionEntity,
        newTransaction: TransactionEntity
    ) {
        syncDebtProgressOnTransactionUpdate(oldTransaction, newTransaction)
        syncGoalProgressOnTransactionUpdate(oldTransaction, newTransaction)
    }

    private suspend fun syncDebtProgressOnTransactionUpdate(
        oldTransaction: TransactionEntity,
        newTransaction: TransactionEntity
    ) {
        val oldDebtId = linkedDebtId(oldTransaction)
        val newDebtId = linkedDebtId(newTransaction)

        when {
            oldDebtId == null && newDebtId == null -> Unit
            oldDebtId != null && oldDebtId == newDebtId -> {
                repository.adjustDebtPaidAmount(oldDebtId, newTransaction.amount - oldTransaction.amount)
            }
            else -> {
                oldDebtId?.let { repository.adjustDebtPaidAmount(it, -oldTransaction.amount) }
                newDebtId?.let { repository.adjustDebtPaidAmount(it, newTransaction.amount) }
            }
        }
    }

    private suspend fun syncGoalProgressOnTransactionUpdate(
        oldTransaction: TransactionEntity,
        newTransaction: TransactionEntity
    ) {
        val oldGoalName = linkedGoalName(oldTransaction)
        val newGoalName = linkedGoalName(newTransaction)
        val oldContribution = oldGoalName?.let { goalContributionAmount(it, oldTransaction) }
        val newContribution = newGoalName?.let { goalContributionAmount(it, newTransaction) }

        when {
            oldGoalName == null && newGoalName == null -> Unit
            oldGoalName != null && oldGoalName == newGoalName -> {
                repository.adjustGoalCurrentAmountByName(
                    oldGoalName,
                    (newContribution ?: 0.0) - (oldContribution ?: 0.0)
                )
            }
            else -> {
                oldGoalName?.let { repository.adjustGoalCurrentAmountByName(it, -(oldContribution ?: 0.0)) }
                newGoalName?.let { repository.adjustGoalCurrentAmountByName(it, newContribution ?: 0.0) }
            }
        }
    }

    private suspend fun rollbackDebtPayment(transaction: TransactionEntity) {
        val debtId = linkedDebtId(transaction) ?: return
        repository.adjustDebtPaidAmount(debtId, -transaction.amount)
    }

    private suspend fun deleteLinkedDebtOpening(transaction: TransactionEntity): Boolean {
        val tag = transaction.projectTag ?: return false
        if (!tag.startsWith(TagPrefix.DEBT_OPENING)) return false

        val openingTimestamp = tag
            .removePrefix(TagPrefix.DEBT_OPENING)
            .toLongOrNull()
            ?: transaction.timestamp

        repository.allDebts.firstOrNull()
            ?.firstOrNull { it.timestamp == openingTimestamp }
            ?.let { repository.deleteDebt(it) }

        repository.deleteTransactionsByTag(tag)
        return true
    }

    private suspend fun rollbackGoalDeposit(transaction: TransactionEntity) {
        val goalName = linkedGoalName(transaction) ?: return
        repository.adjustGoalCurrentAmountByName(goalName, -goalContributionAmount(goalName, transaction))
    }

    private suspend fun goalContributionAmount(goalName: String, transaction: TransactionEntity): Double {
        val goalCurrency = repository.allGoals.firstOrNull()
            ?.firstOrNull { it.name == goalName }
            ?.currency
            ?: transaction.currency

        return convertAmount(
            amount = transaction.amount,
            fromCurrency = transaction.currency,
            toCurrency = goalCurrency
        )
    }

    private fun shouldKeepProjectTag(existing: TransactionEntity, newType: String): Boolean {
        val tag = existing.projectTag ?: return false
        return when {
            tag.startsWith(TagPrefix.DEBT_OPENING) -> newType == existing.type
            tag.startsWith(TagPrefix.DEBT_ROLLBACK) -> newType == existing.type
            existing.type == TxType.GOAL_DEPOSIT -> newType == TxType.GOAL_DEPOSIT
            tag.startsWith(TagPrefix.TRANSFER) -> newType == existing.type
            tag.startsWith(TagPrefix.RECURRING) -> newType == existing.type
            else -> false
        }
    }

    private fun isDebtLinkedTransaction(transaction: TransactionEntity): Boolean {
        return linkedDebtId(transaction) != null
    }

    private fun linkedDebtId(transaction: TransactionEntity): Int? {
        val tag = transaction.projectTag ?: return null
        return tag
            .takeIf { it.startsWith(TagPrefix.DEBT_ROLLBACK) }
            ?.removePrefix(TagPrefix.DEBT_ROLLBACK)
            ?.toIntOrNull()
    }

    private fun linkedGoalName(transaction: TransactionEntity): String? {
        if (transaction.type != TxType.GOAL_DEPOSIT) return null

        val goalName = when {
            transaction.projectTag?.isNotBlank() == true -> transaction.projectTag!!.trim()
            transaction.note.startsWith("Deposit to goal: ") -> transaction.note.removePrefix("Deposit to goal: ").trim()
            transaction.note.startsWith("Tiết kiệm cho mục tiêu: ") -> transaction.note.removePrefix("Tiết kiệm cho mục tiêu: ").trim()
            transaction.note.startsWith("Nuôi heo: ") -> transaction.note.removePrefix("Nuôi heo: ").trim()
            else -> ""
        }

        return goalName.takeIf { it.isNotBlank() }
    }

    private fun insertDefaultCategories() = viewModelScope.launch(Dispatchers.IO) {
        val currentExp = repository.expenseCategories.firstOrNull()?.map { it.name } ?: emptyList()
        val defaultExpNames = context.resources.getStringArray(R.array.default_expense_categories)
        val expEmojis = listOf("🍔", "🚗", "🧾", "🛍️", "💊", "🎬", "📚")

        defaultExpNames.mapIndexed { index, name ->
            CategoryEntity(
                name = name,
                emoji = expEmojis.getOrElse(index) { "💰" },
                type = TxType.EXPENSE
            )
        }.filter { it.name !in currentExp }.forEach { repository.insertCategory(it) }

        val currentInc = repository.incomeCategories.firstOrNull()?.map { it.name } ?: emptyList()
        val defaultIncNames = context.resources.getStringArray(R.array.default_income_categories)
        val incEmojis = listOf("💰", "📈", "💹", "🎁")

        defaultIncNames.mapIndexed { index, name ->
            CategoryEntity(
                name = name,
                emoji = incEmojis.getOrElse(index) { "💵" },
                type = TxType.INCOME
            )
        }.filter { it.name !in currentInc }.forEach { repository.insertCategory(it) }
    }

    private fun checkAndExecuteRecurrings() = viewModelScope.launch(Dispatchers.IO) {
        val recurringList = repository.allRecurrings.firstOrNull() ?: emptyList()
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_MONTH)
        val lastDayOfCurrentMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentMonthYear = currentMonthYear()
        val (monthStart, monthEnd) = currentMonthRangeMillis()
        val autoSuffix = context.getString(R.string.vm_budget_recurring_auto_suffix)

        recurringList.forEach { recurring ->
            val effectiveRunDay = recurring.dayOfMonth.coerceAtMost(lastDayOfCurrentMonth)
            val recurringTag = "${TagPrefix.RECURRING}${recurring.id}_$currentMonthYear"
            val alreadyCreatedThisMonth = repository.countTransactionsByProjectTagBetween(
                tag = recurringTag,
                startMillis = monthStart,
                endMillis = monthEnd
            ) > 0

            if (alreadyCreatedThisMonth && recurring.lastExecutedMonth != currentMonthYear) {
                repository.updateRecurring(recurring.copy(lastExecutedMonth = currentMonthYear))
                return@forEach
            }

            if (
                currentDay >= effectiveRunDay &&
                recurring.lastExecutedMonth != currentMonthYear &&
                !alreadyCreatedThisMonth
            ) {
                repository.insert(
                    TransactionEntity(
                        amount = recurring.amount,
                        type = recurring.type,
                        category = recurring.category,
                        note = "${recurring.note} $autoSuffix".trim(),
                        timestamp = System.currentTimeMillis(),
                        projectTag = recurringTag,
                        sourceName = recurring.sourceName,
                        currency = normalizeCurrency(recurring.currency)
                    )
                )
                repository.updateRecurring(recurring.copy(lastExecutedMonth = currentMonthYear))
            }
        }
    }

    private fun buildSourceBalances(transactions: List<TransactionEntity>, sources: List<SourceEntity>): Map<String, Map<String, Double>> {
        val balances = mutableMapOf<String, MutableMap<String, Double>>()
        val activeSourceNames = sources.map { it.name.trim() }.toSet()
        transactions.forEach { tx ->
            val srcName = tx.sourceName.trim()
            if (srcName !in activeSourceNames) return@forEach
            val curr = normalizeCurrency(tx.currency)
            val delta = when (tx.type) {
                TxType.INCOME, TxType.TRANSFER_IN -> tx.amount
                TxType.EXPENSE, TxType.TRANSFER_OUT -> -tx.amount
                else -> 0.0
            }
            if (delta != 0.0) balances.getOrPut(srcName) { mutableMapOf() }[curr] = (balances[srcName]?.get(curr) ?: 0.0) + delta
        }

        sources.filter { it.type == SourceEntity.SourceType.SAVINGS && it.interestRate > 0.0 }.forEach { src ->
            balances[src.name]?.let { srcBal ->
                srcBal.keys.toList().forEach { curr ->
                    val principal = srcBal[curr] ?: 0.0
                    val days = ((System.currentTimeMillis() - src.createdTimestamp) / (1000L * 60 * 60 * 24)).toDouble().coerceAtLeast(0.0)
                    val interest = when {
                        src.interestPeriod == SourceEntity.InterestPeriod.DAILY -> principal * (src.interestRate / 100.0) * (days / 365.0)
                        src.interestPeriod == SourceEntity.InterestPeriod.MONTHLY -> principal * (src.interestRate / 100.0) * (floor(days / 30.0) / 12.0)
                        src.interestPeriod.startsWith(TagPrefix.TERM) -> {
                            val terms = src.interestPeriod.removePrefix(TagPrefix.TERM).toIntOrNull() ?: 1
                            principal * (src.interestRate / 100.0) * ((terms * 30.0) / 365.0) * floor(days / (terms * 30.0))
                        }
                        else -> 0.0
                    }
                    srcBal[curr] = principal + interest
                }
            }
        }
        return balances
    }

    private suspend fun findExpenseCategoryId(categoryName: String): Int? {
        val safeName = categoryName.trim()
        return repository.expenseCategories.firstOrNull()
            ?.firstOrNull { it.name == safeName }
            ?.id
    }

    private fun currentMonthYear(): String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

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

    private fun toMinorAmount(amount: Double, currency: String): Long {
        val multiplier = if (normalizeCurrency(currency) in listOf("VND", "JPY", "KRW")) 1.0 else 100.0
        return (amount * multiplier).roundToLong().coerceAtLeast(0L)
    }

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

    private fun normalizeCurrency(c: String): String = when (c.trim().uppercase(Locale.ROOT)) { "VNĐ", "VND", "" -> "VND"; else -> c.trim().uppercase(Locale.ROOT) }
}
