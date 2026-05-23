package com.katgr0up.katbudget.data.repository

import com.katgr0up.katbudget.data.local.entity.BudgetEntity
import com.katgr0up.katbudget.data.local.entity.CategoryEntity
import com.katgr0up.katbudget.data.local.entity.DebtEntity
import com.katgr0up.katbudget.data.local.entity.RecurringEntity
import com.katgr0up.katbudget.data.local.entity.SavingGoalEntity
import com.katgr0up.katbudget.data.local.entity.SourceEntity
import com.katgr0up.katbudget.data.local.dao.TransactionDao
import com.katgr0up.katbudget.data.local.entity.TransactionEntity
import com.katgr0up.katbudget.data.local.type.CategoryType
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val recentTransactions: Flow<List<TransactionEntity>> = transactionDao.getRecentTransactions()
    val allSources: Flow<List<SourceEntity>> = transactionDao.getAllSources()
    val allDebts: Flow<List<DebtEntity>> = transactionDao.getAllDebts()
    val allBudgets: Flow<List<BudgetEntity>> = transactionDao.getAllBudgets()
    val allRecurrings: Flow<List<RecurringEntity>> = transactionDao.getAllRecurrings()
    val allGoals: Flow<List<SavingGoalEntity>> = transactionDao.getAllGoals()

    val expenseCategories: Flow<List<CategoryEntity>> =
        transactionDao.getCategoriesByType(CategoryType.EXPENSE)

    val incomeCategories: Flow<List<CategoryEntity>> =
        transactionDao.getCategoriesByType(CategoryType.INCOME)

    suspend fun insert(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun getTransactionById(id: Int): TransactionEntity? {
        return transactionDao.getTransactionById(id)
    }

    suspend fun update(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun delete(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionsByTag(tag: String) {
        transactionDao.deleteTransactionsByTag(tag)
    }

    suspend fun countTransactionsByProjectTagBetween(
        tag: String,
        startMillis: Long,
        endMillis: Long
    ): Int {
        return transactionDao.countTransactionsByProjectTagBetween(tag, startMillis, endMillis)
    }

    suspend fun insertSource(source: SourceEntity) {
        transactionDao.insertSource(source)
    }

    suspend fun updateSource(source: SourceEntity) {
        transactionDao.updateSource(source)
    }

    suspend fun deleteSource(source: SourceEntity) {
        transactionDao.deleteSource(source)
    }

    suspend fun insertDebt(debt: DebtEntity) {
        transactionDao.insertDebt(debt)
    }

    suspend fun updateDebt(debt: DebtEntity) {
        transactionDao.updateDebt(debt)
    }

    suspend fun deleteDebt(debt: DebtEntity) {
        transactionDao.deleteDebt(debt)
    }

    suspend fun updateDebtStatus(id: Int, isPaid: Boolean) {
        transactionDao.updateDebtStatus(id, isPaid)
    }

    suspend fun adjustDebtPaidAmount(id: Int, delta: Double) {
        transactionDao.adjustDebtPaidAmount(id, delta)
    }

    suspend fun insertBudget(budget: BudgetEntity) {
        transactionDao.insertBudget(budget)
    }

    suspend fun updateBudget(budget: BudgetEntity) {
        transactionDao.updateBudget(budget)
    }

    suspend fun deleteBudget(budget: BudgetEntity) {
        transactionDao.deleteBudget(budget)
    }

    suspend fun insertRecurring(recurring: RecurringEntity) {
        transactionDao.insertRecurring(recurring)
    }

    suspend fun updateRecurring(recurring: RecurringEntity) {
        transactionDao.updateRecurring(recurring)
    }

    suspend fun deleteRecurring(recurring: RecurringEntity) {
        transactionDao.deleteRecurring(recurring)
    }

    suspend fun insertGoal(goal: SavingGoalEntity) {
        transactionDao.insertGoal(goal)
    }

    suspend fun updateGoal(goal: SavingGoalEntity) {
        transactionDao.updateGoal(goal)
    }

    suspend fun adjustGoalCurrentAmountByName(goalName: String, delta: Double) {
        transactionDao.adjustGoalCurrentAmountByName(goalName, delta)
    }

    suspend fun deleteGoal(goal: SavingGoalEntity) {
        transactionDao.deleteGoal(goal)
    }

    suspend fun insertCategory(category: CategoryEntity) {
        transactionDao.insertCategory(category)
    }

    // Hàm gọi khi nhấn giữ Xóa Danh Mục từ TransactionDialog -> BudgetViewModel
    suspend fun deleteCategory(category: CategoryEntity) {
        transactionDao.deleteCategory(category)
    }

    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> {
        return transactionDao.getCategoriesByType(type)
    }
}
