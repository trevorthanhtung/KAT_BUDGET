package com.katgr0up.katbudget.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.katgr0up.katbudget.R
import java.util.Locale

@Composable
fun rememberCategoryNameLocalizer(isEng: Boolean): (String) -> String {
    val fallbackOther = katStringResource(id = R.string.fallback_other, isEng = isEng)
    val fallbackOtherIncome = katStringResource(id = R.string.fallback_other_income, isEng = isEng)
    val fallbackTransfer = katStringResource(id = R.string.fallback_transfer, isEng = isEng)
    val fallbackOpeningBalance = katStringResource(id = R.string.fallback_opening_balance, isEng = isEng)
    val categoryFood = katStringResource(id = R.string.category_food, isEng = isEng)
    val categoryTransport = katStringResource(id = R.string.category_transport, isEng = isEng)
    val categoryBills = katStringResource(id = R.string.category_bills, isEng = isEng)
    val categoryShopping = katStringResource(id = R.string.category_shopping, isEng = isEng)
    val categoryHealth = katStringResource(id = R.string.category_health, isEng = isEng)
    val categoryEntertainment = katStringResource(id = R.string.category_entertainment, isEng = isEng)
    val categoryEducation = katStringResource(id = R.string.category_education, isEng = isEng)
    val categorySalary = katStringResource(id = R.string.category_salary, isEng = isEng)
    val categoryBusiness = katStringResource(id = R.string.category_business, isEng = isEng)
    val categoryInvestment = katStringResource(id = R.string.category_investment, isEng = isEng)
    val categoryGifts = katStringResource(id = R.string.category_gifts, isEng = isEng)

    return remember(isEng) {
        val labels = mapOf(
            "thực phẩm" to categoryFood,
            "food" to categoryFood,
            "di chuyển" to categoryTransport,
            "transport" to categoryTransport,
            "hóa đơn" to categoryBills,
            "bills" to categoryBills,
            "mua sắm" to categoryShopping,
            "shopping" to categoryShopping,
            "sức khỏe" to categoryHealth,
            "health" to categoryHealth,
            "giải trí" to categoryEntertainment,
            "entertainment" to categoryEntertainment,
            "giáo dục" to categoryEducation,
            "education" to categoryEducation,
            "lương" to categorySalary,
            "salary" to categorySalary,
            "kinh doanh" to categoryBusiness,
            "business" to categoryBusiness,
            "đầu tư" to categoryInvestment,
            "investment" to categoryInvestment,
            "được tặng" to categoryGifts,
            "gifts" to categoryGifts,
            "gift" to categoryGifts,
            "khác" to fallbackOther,
            "other" to fallbackOther,
            "thu nhập khác" to fallbackOtherIncome,
            "other income" to fallbackOtherIncome,
            "chuyển tiền" to fallbackTransfer,
            "transfer" to fallbackTransfer,
            "số dư ban đầu" to fallbackOpeningBalance,
            "opening balance" to fallbackOpeningBalance
        )

        val localizer: (String) -> String = { rawCategory ->
            labels[rawCategory.trim().lowercase(Locale.ROOT)] ?: rawCategory
        }
        localizer
    }
}
