package com.katgr0up.katbudget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = RecurringEntity.TABLE_NAME,
    indices = [
        Index(value = ["type"]),
        Index(value = ["category"]),
        Index(value = ["source_name"]),
        Index(value = ["day_of_month"]),
        Index(value = ["day_of_month", "last_executed_month"])
    ]
)
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "amount")
    val amount: Double,

    /**
     * RecurringType.INCOME = Thu nhập
     * RecurringType.EXPENSE = Chi tiêu
     */
    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "note")
    val note: String = "",

    @ColumnInfo(name = "source_name")
    val sourceName: String = "",

    @ColumnInfo(name = "currency")
    val currency: String = DEFAULT_CURRENCY,

    /**
     * Ngày tự động kích hoạt hàng tháng.
     * Giá trị hợp lệ: 1..31
     */
    @ColumnInfo(name = "day_of_month")
    val dayOfMonth: Int,

    /**
     * Tháng gần nhất đã chạy.
     * Nên lưu dạng "yyyy-MM", ví dụ: "2026-05".
     * Để rỗng nếu chưa từng chạy.
     */
    @ColumnInfo(name = "last_executed_month")
    val lastExecutedMonth: String = ""
) {
    init {
        require(dayOfMonth in 1..31) {
            "dayOfMonth must be between 1 and 31"
        }
    }

    companion object {
        const val TABLE_NAME = "recurring_transactions"
        const val DEFAULT_CURRENCY = "VND"
    }

    object RecurringType {
        const val INCOME = "INCOME"
        const val EXPENSE = "EXPENSE"
    }
}
