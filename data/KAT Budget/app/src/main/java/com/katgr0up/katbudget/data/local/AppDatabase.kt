package com.katgr0up.katbudget.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.katgr0up.katbudget.data.local.dao.TransactionDao
import com.katgr0up.katbudget.data.local.entity.BudgetEntity
import com.katgr0up.katbudget.data.local.entity.CategoryEntity
import com.katgr0up.katbudget.data.local.entity.DebtEntity
import com.katgr0up.katbudget.data.local.entity.RecurringEntity
import com.katgr0up.katbudget.data.local.entity.SavingGoalEntity
import com.katgr0up.katbudget.data.local.entity.SourceEntity
import com.katgr0up.katbudget.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        SourceEntity::class,
        DebtEntity::class,
        BudgetEntity::class,
        RecurringEntity::class,
        SavingGoalEntity::class,
        CategoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        private const val DATABASE_NAME = "kat_budget_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: createDatabase(context).also { database ->
                    INSTANCE = database
                }
            }
        }

        private fun createDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                // Quan trọng: không dùng fallbackToDestructiveMigration() cho bản release.
                // Nếu app đã từng phát hành bản cũ, phải thêm migration thật ở đây:
                // .addMigrations(MIGRATION_8_9)
                .build()
        }
    }
}