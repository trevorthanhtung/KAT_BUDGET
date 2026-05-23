package com.katgr0up.katbudget.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.ui.utils.katStringResource
import java.util.Locale

@Composable
fun ExchangeRateDialog(
    isEng: Boolean,
    colors: BudgetColors,
    exchangeRates: Map<String, Double>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = katStringResource(id = R.string.settings_rates_title, isEng = isEng),
                color = colors.text,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = katStringResource(id = R.string.settings_rates_description, isEng = isEng),
                    color = colors.subText,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    exchangeRates
                        .filter { it.key != "VND" }
                        .toSortedMap()
                        .forEach { (currency, rate) ->
                            ExchangeRateRow(currency, rate, isEng, colors)
                        }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isEng) {
                        "* Source: Vietcombank Retail Banking"
                    } else {
                        "* Nguồn dữ liệu: Ngân hàng Vietcombank"
                    },
                    color = colors.subText.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }
        },
        confirmButton = {
            SettingsDialogPrimaryButton(
                text = katStringResource(id = R.string.btn_close, isEng = isEng),
                colors = colors,
                onClick = onDismiss
            )
        }
    )
}

@Composable
private fun ExchangeRateRow(
    currency: String,
    rate: Double,
    isEng: Boolean,
    colors: BudgetColors
) {
    val rateText = if (rate > 0) {
        String.format(Locale.GERMAN, "%,.2f", rate).removeSuffix(",00")
    } else {
        katStringResource(id = R.string.settings_rates_updating, isEng = isEng)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.12f))
                    .border(1.dp, colors.accent.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currency.take(1),
                    color = colors.accent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "1 $currency",
                color = colors.text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Text(
            text = if (rate > 0) "= $rateText VND" else rateText,
            color = if (rate > 0) colors.positive else colors.subText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
    }
}
