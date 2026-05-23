package com.katgr0up.katbudget.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.katgr0up.katbudget.data.local.type.CategoryType

@Entity(
    tableName = "categories",
    indices = [
        Index(
            value = ["name", "type"],
            unique = true
        )
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    val emoji: String = "",

    /**
     * Must be one of [CategoryType.INCOME] or [CategoryType.EXPENSE].
     */
    val type: String
)
