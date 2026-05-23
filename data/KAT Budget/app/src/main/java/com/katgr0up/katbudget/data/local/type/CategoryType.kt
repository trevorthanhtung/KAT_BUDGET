package com.katgr0up.katbudget.data.local.type

object CategoryType {
    const val INCOME = "INCOME"
    const val EXPENSE = "EXPENSE"

    val values: Set<String> = setOf(INCOME, EXPENSE)

    fun isValid(type: String): Boolean {
        return type in values
    }
}
