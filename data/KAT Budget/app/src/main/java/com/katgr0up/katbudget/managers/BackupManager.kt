package com.katgr0up.katbudget.managers

import com.katgr0up.katbudget.data.local.entity.BudgetEntity
import com.katgr0up.katbudget.data.local.entity.CategoryEntity
import com.katgr0up.katbudget.data.local.entity.DebtEntity
import com.katgr0up.katbudget.data.local.entity.RecurringEntity
import com.katgr0up.katbudget.data.local.entity.SavingGoalEntity
import com.katgr0up.katbudget.data.local.entity.SourceEntity
import com.katgr0up.katbudget.data.local.entity.TransactionEntity
import com.katgr0up.katbudget.data.local.type.CategoryType
import com.katgr0up.katbudget.data.repository.TransactionRepository
import com.katgr0up.katbudget.utils.TxType
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object BackupManager {

    fun exportBackupDataBytes(
        cS: List<SourceEntity>, cT: List<TransactionEntity>, cD: List<DebtEntity>,
        cB: List<BudgetEntity>, cR: List<RecurringEntity>, cG: List<SavingGoalEntity>,
        cC: List<CategoryEntity> = emptyList()
    ): ByteArray {
        val categoryNamesById = cC.associate { it.id to it.name }
        val root = JSONObject()
            .put("sources", cS.toJsonArray { source ->
                JSONObject().put("name", source.name).put("type", source.type)
                    .put("includeInTotal", source.includeInTotal).put("interestRate", source.interestRate)
                    .put("interestPeriod", source.interestPeriod).put("createdTimestamp", source.createdTimestamp)
            })
            .put("transactions", cT.toJsonArray { transaction ->
                JSONObject().put("amount", transaction.amount).put("type", transaction.type)
                    .put("category", transaction.category).put("note", transaction.note)
                    .put("timestamp", transaction.timestamp).put("sourceName", transaction.sourceName)
                    .put("currency", normalizeCurrency(transaction.currency))
                    .put("projectTag", transaction.projectTag ?: JSONObject.NULL)
                    .put("imageUri", transaction.imageUri ?: JSONObject.NULL)
            })
            .put("debts", cD.toJsonArray { debt ->
                JSONObject().put("personName", debt.personName).put("amount", debt.amount)
                    .put("currency", normalizeCurrency(debt.currency)).put("type", debt.type)
                    .put("note", debt.note).put("timestamp", debt.timestamp)
                    .put("isPaid", debt.isPaid).put("dueDate", debt.dueDate ?: JSONObject.NULL)
                    .put("paidAmount", debt.paidAmount)
            })
            .put("budgets", cB.toJsonArray { budget ->
                JSONObject().put("categoryId", budget.categoryId)
                    .put("category", categoryNamesById[budget.categoryId].orEmpty())
                    .put("limitAmount", budget.limitAmountMinor)
                    .put("currency", normalizeCurrency(budget.currencyCode)).put("monthYear", budget.monthYear)
            })
            .put("recurrings", cR.toJsonArray { recurring ->
                JSONObject().put("amount", recurring.amount).put("type", recurring.type)
                    .put("category", recurring.category).put("note", recurring.note)
                    .put("sourceName", recurring.sourceName).put("currency", normalizeCurrency(recurring.currency))
                    .put("dayOfMonth", recurring.dayOfMonth).put("lastExecutedMonth", recurring.lastExecutedMonth)
            })
            .put("goals", cG.toJsonArray { goal ->
                JSONObject().put("name", goal.name).put("targetAmount", goal.targetAmount)
                    .put("currentAmount", goal.currentAmount).put("currency", normalizeCurrency(goal.currency))
            })

        if (cC.isNotEmpty()) {
            root.put("categories", cC.toJsonArray { category ->
                JSONObject().put("id", category.id).put("name", category.name)
                    .put("emoji", category.emoji).put("type", category.type)
            })
        }

        return gzip(root.toString())
    }

    suspend fun restoreDataFromBytes(bytes: ByteArray, repository: TransactionRepository) {
        val root = parseBackupData(bytes) ?: throw IllegalArgumentException("Invalid JSON backup file")
        val clearCategories = root.optJSONArray("categories") != null

        val isVi = Locale.getDefault().language == "vi"
        val fallbackCategory = if (isVi) "Khác" else "Other"
        val fallbackSourceName = if (isVi) "Tiền mặt" else "Cash"
        val fallbackGoalName = if (isVi) "Mục tiêu đã khôi phục" else "Restored Goal"

        // 1. Dọn sạch dữ liệu cũ. Xóa bảng phụ thuộc trước để tránh dữ liệu mồ côi.
        repository.allTransactions.first().forEach { repository.delete(it) }
        repository.allBudgets.first().forEach { repository.deleteBudget(it) }
        repository.allRecurrings.first().forEach { repository.deleteRecurring(it) }
        repository.allGoals.first().forEach { repository.deleteGoal(it) }
        repository.allDebts.first().forEach { repository.deleteDebt(it) }
        repository.allSources.first().forEach { repository.deleteSource(it) }
        if (clearCategories) {
            repository.expenseCategories.first().forEach { repository.deleteCategory(it) }
            repository.incomeCategories.first().forEach { repository.deleteCategory(it) }
        }

        // 2. Bóc tách JSON và chèn dữ liệu mới với fallback chuẩn quốc tế hóa
        root.optJSONArray("categories")?.forEachObject { item ->
            val categoryName = item.optCleanString("name", "")
            if (categoryName.isBlank()) return@forEachObject

            repository.insertCategory(
                CategoryEntity(
                    id = item.optInt("id", 0).coerceAtLeast(0),
                    name = categoryName,
                    emoji = item.optCleanString("emoji", ""),
                    type = item.optCleanString("type", CategoryType.EXPENSE)
                        .takeIf { CategoryType.isValid(it) }
                        ?: CategoryType.EXPENSE
                )
            )
        }

        root.optJSONArray("sources")?.forEachObject { item ->
            val sourceName = item.optCleanString("name", fallbackSourceName)
            if (sourceName.isBlank()) return@forEachObject

            repository.insertSource(
                SourceEntity(
                    name = sourceName,
                    type = item.optCleanString("type", SourceEntity.SourceType.CASH)
                        .takeIf { SourceType.isValid(it) }
                        ?: SourceEntity.SourceType.CASH,
                    includeInTotal = item.optBoolean("includeInTotal", true),
                    interestRate = item.optDouble("interestRate", 0.0).coerceAtLeast(0.0),
                    interestPeriod = item.optCleanString(
                        "interestPeriod",
                        SourceEntity.InterestPeriod.NONE
                    ),
                    createdTimestamp = item.optLong("createdTimestamp", System.currentTimeMillis())
                )
            )
        }

        ensureReferencedSourcesExist(root, repository, fallbackSourceName)

        root.optJSONArray("transactions")?.forEachObject { item ->
            repository.insert(
                TransactionEntity(
                    amount = item.optDouble("amount", 0.0).coerceAtLeast(0.0),
                    type = item.optCleanString("type", TxType.EXPENSE),
                    category = item.optCleanString("category", fallbackCategory),
                    note = item.optCleanString("note", ""),
                    timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                    sourceName = item.optCleanString(
                        "sourceName",
                        fallbackSourceName
                    ),
                    currency = normalizeCurrency(item.optCleanString("currency", "VND")),
                    projectTag = item.optNullableString("projectTag"),
                    imageUri = item.optNullableString("imageUri")
                )
            )
        }
        root.optJSONArray("debts")?.forEachObject { item ->
            repository.insertDebt(
                DebtEntity(
                    personName = item.optCleanString("personName", ""),
                    amount = item.optDouble("amount", 0.0).coerceAtLeast(0.0),
                    currency = normalizeCurrency(item.optCleanString("currency", "VND")),
                    type = item.optCleanString("type", DebtEntity.TYPE_DEBT),
                    note = item.optCleanString("note", ""),
                    timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                    isPaid = item.optBoolean("isPaid", false),
                    dueDate = item.optNullableLong("dueDate"),
                    paidAmount = item.optDouble("paidAmount", 0.0)
                        .coerceIn(0.0, item.optDouble("amount", 0.0).coerceAtLeast(0.0))
                )
            )
        }
        root.optJSONArray("budgets")?.forEachObject { item ->
            val categoryName = item.optCleanString("category", fallbackCategory)
            val currency = normalizeCurrency(item.optCleanString("currency", "VND"))
            val categoryId = resolveExpenseCategoryId(repository, categoryName, fallbackCategory)
            val limitAmountMinor = readBudgetLimitAmountMinor(item, currency)

            repository.insertBudget(
                BudgetEntity(
                    categoryId = categoryId,
                    limitAmountMinor = limitAmountMinor,
                    monthYear = item.optCleanString("monthYear", currentMonthYear()),
                    currencyCode = currency
                )
            )
        }
        root.optJSONArray("recurrings")?.forEachObject { item ->
            repository.insertRecurring(
                RecurringEntity(
                    amount = item.optDouble("amount", 0.0).coerceAtLeast(0.0),
                    type = item.optCleanString("type", RecurringEntity.RecurringType.EXPENSE),
                    category = item.optCleanString("category", fallbackCategory),
                    note = item.optCleanString("note", ""),
                    sourceName = item.optCleanString(
                        "sourceName",
                        fallbackSourceName
                    ),
                    currency = normalizeCurrency(item.optCleanString("currency", "VND")),
                    dayOfMonth = item.optInt("dayOfMonth", 1).coerceIn(1, 31),
                    lastExecutedMonth = item.optCleanString("lastExecutedMonth", "")
                )
            )
        }
        var restoredGoalIndex = 1
        root.optJSONArray("goals")?.forEachObject { item ->
            val currentAmount = item.optDouble("currentAmount", 0.0).coerceAtLeast(0.0)
            val targetAmount = item.optDouble("targetAmount", 0.0)
                .takeIf { it > 0.0 }
                ?: currentAmount.coerceAtLeast(1.0)
            val goalName = item.optCleanString("name", "")
                .ifBlank { "$fallbackGoalName ${restoredGoalIndex++}" }

            repository.insertGoal(
                SavingGoalEntity(
                    name = goalName,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    currency = normalizeCurrency(item.optCleanString("currency", "VND"))
                )
            )
        }
    }

    private fun parseBackupData(bytes: ByteArray): JSONObject? {
        return runCatching {
            JSONObject(readBackupString(bytes))
        }.getOrNull()
    }

    private fun normalizeCurrency(currency: String): String {
        return when (currency.trim().uppercase(Locale.ROOT)) {
            "VNĐ", "VND", "" -> "VND"
            else -> currency.trim().uppercase(Locale.ROOT)
        }
    }

    private suspend fun resolveExpenseCategoryId(
        repository: TransactionRepository,
        categoryName: String,
        fallbackCategory: String
    ): Int {
        val safeName = categoryName.ifBlank { fallbackCategory }
        repository.expenseCategories.first().firstOrNull { it.name == safeName }?.let {
            return it.id
        }

        repository.insertCategory(
            CategoryEntity(
                name = safeName,
                emoji = "",
                type = CategoryType.EXPENSE
            )
        )

        return repository.expenseCategories.first()
            .firstOrNull { it.name == safeName }
            ?.id
            ?: throw IllegalStateException("Cannot restore budget category: $safeName")
    }

    private suspend fun ensureReferencedSourcesExist(
        root: JSONObject,
        repository: TransactionRepository,
        fallbackSourceName: String
    ) {
        val referencedSourceNames = linkedSetOf<String>()

        root.optJSONArray("transactions")?.forEachObject { item ->
            referencedSourceNames += item.optCleanString("sourceName", fallbackSourceName)
        }
        root.optJSONArray("recurrings")?.forEachObject { item ->
            referencedSourceNames += item.optCleanString("sourceName", fallbackSourceName)
        }

        if (referencedSourceNames.isEmpty()) return

        val existingNames = repository.allSources.first()
            .map { it.name.trim() }
            .toMutableSet()

        referencedSourceNames
            .map { it.trim().ifBlank { fallbackSourceName } }
            .filter { it !in existingNames }
            .forEach { missingSourceName ->
                repository.insertSource(
                    SourceEntity(
                        name = missingSourceName,
                        type = SourceEntity.SourceType.CASH
                    )
                )
                existingNames += missingSourceName
            }
    }

    private fun readBudgetLimitAmountMinor(item: JSONObject, currency: String): Long {
        return when {
            item.has("limitAmountMinor") -> item.optLong("limitAmountMinor", 0L)
            item.has("limitAmount") -> item.optLong("limitAmount", 0L)
            item.has("limit") -> toMinorAmount(item.optDouble("limit", 0.0), currency)
            else -> 0L
        }.coerceAtLeast(0L)
    }

    private fun toMinorAmount(amount: Double, currency: String): Long {
        val multiplier = if (normalizeCurrency(currency) in listOf("VND", "JPY", "KRW")) 1.0 else 100.0
        return Math.round(amount * multiplier).coerceAtLeast(0L)
    }

    private fun currentMonthYear(): String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    private fun <T> List<T>.toJsonArray(mapper: (T) -> JSONObject): JSONArray {
        val array = JSONArray()
        forEach { item -> array.put(mapper(item)) }
        return array
    }

    private fun gzip(value: String): ByteArray {
        val byteStream = ByteArrayOutputStream()
        GZIPOutputStream(byteStream).use { it.write(value.toByteArray(Charsets.UTF_8)) }
        return byteStream.toByteArray()
    }

    private fun readBackupString(bytes: ByteArray): String {
        return try {
            GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (error: Exception) {
            String(bytes, Charsets.UTF_8)
        }
    }
}

private inline fun JSONArray.forEachObject(action: (JSONObject) -> Unit) {
    for (index in 0 until length()) action(getJSONObject(index))
}

private object SourceType {
    private val values = setOf(
        SourceEntity.SourceType.BANK,
        SourceEntity.SourceType.WALLET,
        SourceEntity.SourceType.CASH,
        SourceEntity.SourceType.SAVINGS
    )

    fun isValid(type: String): Boolean = type in values
}

private fun JSONObject.optNullableString(key: String): String? = if (has(key) && !isNull(key)) optString(key) else null
private fun JSONObject.optNullableLong(key: String): Long? = if (has(key) && !isNull(key)) optLong(key) else null
private fun JSONObject.optCleanString(key: String, fallback: String): String = optNullableString(key)?.trim()?.ifEmpty { fallback } ?: fallback
