package com.katgr0up.katbudget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = DebtEntity.TABLE_NAME,
    indices = [
        Index(value = ["person_name"]),
        Index(value = ["is_paid"]),
        Index(value = ["due_date"]),
        Index(value = ["timestamp"])
    ]
)
data class DebtEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "person_name")
    val personName: String,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "currency")
    val currency: String = DEFAULT_CURRENCY,

    /**
     * TYPE_LOAN = Cho vay
     * TYPE_DEBT = Đi vay
     */
    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "note")
    val note: String = "",

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_paid")
    val isPaid: Boolean = false,

    /**
     * Hạn trả nợ.
     * Null nếu không có hạn.
     */
    @ColumnInfo(name = "due_date")
    val dueDate: Long? = null,

    /**
     * Lưu tiến độ số tiền đã tích lũy trả/thu.
     */
    @ColumnInfo(name = "paid_amount")
    val paidAmount: Double = 0.0
) {
    companion object {
        const val TABLE_NAME = "debts"
        const val DEFAULT_CURRENCY = "VND"

        const val TYPE_DEBT = "DEBT" // Đi vay
        const val TYPE_LOAN = "LOAN" // Cho vay
    }
}