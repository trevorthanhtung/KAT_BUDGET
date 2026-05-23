package com.katgr0up.katbudget.managers

import com.katgr0up.katbudget.data.local.entity.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvReportManager {

    fun generateCsvReport(transactions: List<TransactionEntity>, isEng: Boolean): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val builder = StringBuilder()

        builder.append("\uFEFF")

        val isVi = !isEng

        val headers = if (isVi) {
            listOf("Ngày giờ", "Loại giao dịch", "Danh mục", "Số tiền", "Đơn vị", "Nguồn tiền", "Ghi chú")
        } else {
            listOf("Date & Time", "Transaction Type", "Category", "Amount", "Currency", "Account", "Note")
        }

        builder.appendLine(headers.joinToString(",") { it.toCsvCell() })

        transactions.forEach { transaction ->
            val row = listOf(
                dateFormat.format(Date(transaction.timestamp)),
                transaction.type.toDisplayTransactionType(isVi),
                transaction.category,
                String.format(Locale.US, "%.2f", transaction.amount).removeSuffix(".00"),
                normalizeCurrency(transaction.currency),
                transaction.sourceName,
                transaction.note
            )
            builder.appendLine(row.joinToString(",") { it.toCsvCell() })
        }
        return builder.toString()
    }

    private fun normalizeCurrency(currency: String): String {
        return when (currency.trim().uppercase(Locale.ROOT)) {
            "VNĐ", "VND", "" -> "VND"
            else -> currency.trim().uppercase(Locale.ROOT)
        }
    }

    private fun String.toDisplayTransactionType(isVi: Boolean): String {
        return if (isVi) {
            when (this) {
                "INCOME" -> "Thu nhập"
                "EXPENSE" -> "Chi tiêu"
                "TRANSFER_IN" -> "Nhận chuyển tiền"
                "TRANSFER_OUT" -> "Chuyển tiền đi"
                "GOAL_DEPOSIT" -> "Mục tiêu tiết kiệm"
                "DEBT_PAYMENT" -> "Trả nợ"
                "DEBT_COLLECTION" -> "Thu nợ"
                "LENDING" -> "Cho vay"
                "BORROWING" -> "Đi vay"
                else -> this
            }
        } else {
            when (this) {
                "INCOME" -> "Income"
                "EXPENSE" -> "Expense"
                "TRANSFER_IN" -> "Transfer In"
                "TRANSFER_OUT" -> "Transfer Out"
                "GOAL_DEPOSIT" -> "Goal Deposit"
                "DEBT_PAYMENT" -> "Debt Payment"
                "DEBT_COLLECTION" -> "Debt Collection"
                "LENDING" -> "Lending"
                "BORROWING" -> "Borrowing"
                else -> this
            }
        }
    }

    private fun String.toCsvCell(): String {
        if (this.isBlank()) return "\"\""

        val trimmedStart = this.trimStart()
        val spreadsheetSafePrefix = if (trimmedStart.firstOrNull() in listOf('=', '+', '-', '@')) "'" else ""
        val safeContent = (spreadsheetSafePrefix + this).replace("\"", "\"\"")
            .replace("\n", " ")
            .replace("\r", "")

        return "\"$safeContent\""
    }
}
