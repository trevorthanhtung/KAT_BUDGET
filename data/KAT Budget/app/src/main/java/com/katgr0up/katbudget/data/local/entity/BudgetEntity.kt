package com.katgr0up.katbudget.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["categoryId", "monthYear", "currencyCode"], unique = true)
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val categoryId: Int,

    /**
     * Amount stored in the minor unit of the currency.
     *
     * Examples:
     * - VND 500,000 -> 500000
     * - USD 12.34 -> 1234
     * - JPY 1,000 -> 1000
     */
    val limitAmountMinor: Long,

    /**
     * ISO 4217 currency code.
     *
     * Examples: VND, USD, EUR, JPY.
     */
    val currencyCode: String = DEFAULT_CURRENCY_CODE,

    /**
     * Budget month in yyyy-MM format.
     *
     * Example: 2026-05.
     */
    val monthYear: String
) {
    companion object {
        const val DEFAULT_CURRENCY_CODE = "VND"
    }
}