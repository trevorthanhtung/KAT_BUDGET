package com.katgr0up.katbudget.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.katgr0up.katbudget.data.local.entity.BudgetEntity
import com.katgr0up.katbudget.data.local.entity.CategoryEntity
import com.katgr0up.katbudget.data.local.entity.DebtEntity
import com.katgr0up.katbudget.data.local.entity.RecurringEntity
import com.katgr0up.katbudget.data.local.entity.SavingGoalEntity
import com.katgr0up.katbudget.data.local.entity.SourceEntity
import com.katgr0up.katbudget.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // --- GIAO DICH ---

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE category NOT IN ('Số dư ban đầu', 'Opening Balance')
        ORDER BY timestamp DESC
        LIMIT 5
        """
    )
    fun getRecentTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Int): TransactionEntity?

    @Query("DELETE FROM transactions WHERE project_tag = :tag")
    suspend fun deleteTransactionsByTag(tag: String)

    @Query(
        """
        DELETE FROM transactions
        WHERE TRIM(source_name) = TRIM(:sourceName)
        OR project_tag IN (
            SELECT project_tag FROM transactions
            WHERE TRIM(source_name) = TRIM(:sourceName)
            AND project_tag LIKE :transferTagPattern
        )
        """
    )
    suspend fun deleteTransactionsBySourceName(
        sourceName: String,
        transferTagPattern: String
    )

    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE project_tag = :tag
        AND timestamp >= :startMillis
        AND timestamp <= :endMillis
        """
    )
    suspend fun countTransactionsByProjectTagBetween(
        tag: String,
        startMillis: Long,
        endMillis: Long
    ): Int

    // --- NGUON TIEN / VI ---

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertSource(source: SourceEntity)

    @Update
    suspend fun updateSource(source: SourceEntity)

    @Delete
    suspend fun deleteSource(source: SourceEntity)

    @Query("SELECT * FROM money_sources ORDER BY include_in_total DESC, name ASC")
    fun getAllSources(): Flow<List<SourceEntity>>

    // --- NO / CHO VAY ---

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertDebt(debt: DebtEntity)

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)

    @Query("SELECT * FROM debts ORDER BY is_paid ASC, timestamp DESC")
    fun getAllDebts(): Flow<List<DebtEntity>>

    @Query("UPDATE debts SET is_paid = :isPaid WHERE id = :id")
    suspend fun updateDebtStatus(id: Int, isPaid: Boolean)

    @Query(
        """
        UPDATE debts
        SET
            paid_amount = CASE
                WHEN paid_amount + :delta < 0 THEN 0
                WHEN paid_amount + :delta > amount THEN amount
                ELSE paid_amount + :delta
            END,
            is_paid = CASE
                WHEN (
                    CASE
                        WHEN paid_amount + :delta < 0 THEN 0
                        WHEN paid_amount + :delta > amount THEN amount
                        ELSE paid_amount + :delta
                    END
                ) >= amount THEN 1 ELSE 0
            END
        WHERE id = :id
        """
    )
    suspend fun adjustDebtPaidAmount(id: Int, delta: Double)

    // --- HAN MUC NGAN SACH ---

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("SELECT * FROM budgets ORDER BY monthYear DESC, categoryId ASC, currencyCode ASC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    // --- GIAO DICH DINH KY ---

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertRecurring(recurring: RecurringEntity)

    @Update
    suspend fun updateRecurring(recurring: RecurringEntity)

    @Delete
    suspend fun deleteRecurring(recurring: RecurringEntity)

    @Query("SELECT * FROM recurring_transactions ORDER BY day_of_month ASC, category ASC")
    fun getAllRecurrings(): Flow<List<RecurringEntity>>

    // --- MUC TIEU TIET KIEM ---

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertGoal(goal: SavingGoalEntity)

    @Update
    suspend fun updateGoal(goal: SavingGoalEntity)

    @Delete
    suspend fun deleteGoal(goal: SavingGoalEntity)

    @Query("SELECT * FROM saving_goals ORDER BY name ASC")
    fun getAllGoals(): Flow<List<SavingGoalEntity>>

    @Query(
        """
        UPDATE saving_goals
        SET current_amount = CASE
            WHEN current_amount + :delta < 0 THEN 0
            ELSE current_amount + :delta
        END
        WHERE name = :goalName
        """
    )
    suspend fun adjustGoalCurrentAmountByName(goalName: String, delta: Double)

    // --- DANH MUC TU TAO ---

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE name = :name AND type = :type")
    suspend fun deleteCategoryByNameAndType(name: String, type: String)

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>
}
