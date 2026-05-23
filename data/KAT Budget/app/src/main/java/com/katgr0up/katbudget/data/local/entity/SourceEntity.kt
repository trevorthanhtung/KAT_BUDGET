package com.katgr0up.katbudget.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = SourceEntity.TABLE_NAME,
    indices = [
        Index(value = ["name"]),
        Index(value = ["type"]),
        Index(value = ["include_in_total"]),
        Index(value = ["created_timestamp"])
    ]
)
data class SourceEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "include_in_total")
    val includeInTotal: Boolean = true,

    @ColumnInfo(name = "interest_rate")
    val interestRate: Double = 0.0,

    @ColumnInfo(name = "interest_period")
    val interestPeriod: String = InterestPeriod.NONE,

    @ColumnInfo(name = "created_timestamp")
    val createdTimestamp: Long = System.currentTimeMillis()
) {
    // ĐÃ XÓA KHỐI INIT ĐỂ CHỐNG CRASH NGẦM KHI ROOM QUERY DỮ LIỆU CŨ

    companion object {
        const val TABLE_NAME = "money_sources"
    }

    object SourceType {
        const val BANK = "BANK"
        const val WALLET = "WALLET"
        const val CASH = "CASH"
        const val SAVINGS = "SAVINGS"
    }

    object InterestPeriod {
        const val NONE = "NONE"
        const val DAILY = "DAILY"
        const val MONTHLY = "MONTHLY"
    }
}