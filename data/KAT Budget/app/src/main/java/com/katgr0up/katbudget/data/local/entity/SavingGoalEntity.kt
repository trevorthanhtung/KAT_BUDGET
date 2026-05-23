package com.katgr0up.katbudget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = SavingGoalEntity.TABLE_NAME,
    indices = [
        Index(value = ["name"]),
        Index(value = ["currency"]),
        Index(value = ["target_amount"])
    ]
)
data class SavingGoalEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    // Ten muc tieu, vi du: "Mua PC moi"
    @ColumnInfo(name = "name")
    val name: String,

    // So tien can dat
    @ColumnInfo(name = "target_amount")
    val targetAmount: Double,

    // So tien hien tai da tich luy
    @ColumnInfo(name = "current_amount")
    val currentAmount: Double = 0.0,

    @ColumnInfo(name = "currency")
    val currency: String = DEFAULT_CURRENCY
) {
    init {
        require(name.isNotBlank()) {
            "name must not be blank"
        }
        require(targetAmount > 0.0) {
            "targetAmount must be greater than 0"
        }
        require(currentAmount >= 0.0) {
            "currentAmount must be greater than or equal to 0"
        }
    }

    companion object {
        const val TABLE_NAME = "saving_goals"
        const val DEFAULT_CURRENCY = "VND"
    }
}