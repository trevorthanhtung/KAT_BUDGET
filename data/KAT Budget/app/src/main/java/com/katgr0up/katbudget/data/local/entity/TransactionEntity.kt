package com.katgr0up.katbudget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = TransactionEntity.TABLE_NAME,
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["type"]),
        Index(value = ["category"]),
        Index(value = ["source_name"]),
        Index(value = ["project_tag"]),
        Index(value = ["type", "timestamp"]),
        Index(value = ["source_name", "timestamp"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "amount")
    val amount: Double,

    /**
     * TransactionType.INCOME = Thu nhập
     * TransactionType.EXPENSE = Chi tiêu
     * TransactionType.TRANSFER_IN = Nhận chuyển tiền
     * TransactionType.TRANSFER_OUT = Chuyển tiền đi
     */
    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "note")
    val note: String = "",

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    /**
     * Dùng để gom các giao dịch liên quan với nhau.
     * Ví dụ: một lần chuyển tiền sẽ có TRANSFER_OUT và TRANSFER_IN cùng projectTag.
     */
    @ColumnInfo(name = "project_tag")
    val projectTag: String? = null,

    @ColumnInfo(name = "source_name")
    val sourceName: String = DEFAULT_SOURCE_NAME,

    @ColumnInfo(name = "currency")
    val currency: String = DEFAULT_CURRENCY,

    /**
     * Đường dẫn ảnh hóa đơn/chứng từ.
     * Null nếu giao dịch không có ảnh.
     */
    @ColumnInfo(name = "image_uri")
    val imageUri: String? = null
) {
    init {
        require(amount >= 0.0) {
            "amount must be greater than or equal to 0"
        }
        require(type.isNotBlank()) {
            "type must not be blank"
        }
        require(category.isNotBlank()) {
            "category must not be blank"
        }
        require(sourceName.isNotBlank()) {
            "sourceName must not be blank"
        }
    }

    companion object {
        const val TABLE_NAME = "transactions"
        const val DEFAULT_SOURCE_NAME = "Tiền mặt"
        const val DEFAULT_CURRENCY = "VND"
    }

    object TransactionType {
        const val INCOME = "INCOME"
        const val EXPENSE = "EXPENSE"
        const val TRANSFER_IN = "TRANSFER_IN"
        const val TRANSFER_OUT = "TRANSFER_OUT"
    }
}