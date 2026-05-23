package com.katgr0up.katbudget.utils // Tên package của bạn

object TxType {
    const val EXPENSE = "EXPENSE"
    const val INCOME = "INCOME"
    const val TRANSFER_IN = "TRANSFER_IN"
    const val TRANSFER_OUT = "TRANSFER_OUT"
    const val GOAL_DEPOSIT = "GOAL_DEPOSIT"
    const val DEBT_PAYMENT = "DEBT_PAYMENT"
}

object TagPrefix {
    const val TRANSFER = "TRF_"
    const val DEBT_ROLLBACK = "DEBT_ROLLBACK_"
    const val RECURRING = "REC_"
    const val TERM = "TERM_"
}

val ALL_CURRENCIES = listOf(
    "VND", "USD", "EUR", "GBP", "JPY", "KRW", "CNY",
    "AUD", "CAD", "CHF", "DKK", "HKD", "INR", "KWD",
    "MYR", "NOK", "RUB", "SAR", "SEK", "SGD", "THB"
)
