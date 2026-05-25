package com.katgr0up.katbudget.ui.tabs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.DebtEntity
import com.katgr0up.katbudget.data.local.entity.SourceEntity
import com.katgr0up.katbudget.ui.components.EmptyState
import com.katgr0up.katbudget.ui.components.SectionHeader
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.ui.dialogs.DebtPaymentDialog
import com.katgr0up.katbudget.ui.utils.KatSpringSpec
import com.katgr0up.katbudget.ui.utils.formatDateOnly
import com.katgr0up.katbudget.ui.utils.formatSmartCurrency
import com.katgr0up.katbudget.ui.utils.katStringResource
import com.katgr0up.katbudget.ui.utils.parseMoney

@Composable
fun KatTabRow(
    titles: List<String>,
    selectedTab: Int,
    colors: BudgetColors,
    onTabSelected: (Int) -> Unit
) {
    val shape = RoundedCornerShape(24.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(colors.card.copy(alpha = 0.64f))
            .border(1.dp, colors.border.copy(alpha = 0.52f), shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        titles.forEachIndexed { index, title ->
            val selected = selectedTab == index
            val itemShape = RoundedCornerShape(20.dp)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(itemShape)
                    .background(if (selected) colors.accent.copy(alpha = 0.12f) else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (selected) colors.accent.copy(alpha = 0.32f) else Color.Transparent,
                        shape = itemShape
                    )
                    .clickable { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (selected) colors.accent else colors.subText,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtRow(
    isEng: Boolean,
    debt: DebtEntity,
    sources: List<SourceEntity>,
    colors: BudgetColors,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPay: (Double, String) -> Unit
) {
    var showPayDialog by remember { mutableStateOf(false) }
    var payAmountInput by remember { mutableStateOf("") }
    var payCurrency by remember(debt.currency) { mutableStateOf(debt.currency) }

    val fallbackCash = katStringResource(id = R.string.source_type_cash, isEng = isEng)
    var selectedSourceName by remember { mutableStateOf(sources.firstOrNull()?.name ?: fallbackCash) }

    LaunchedEffect(sources, fallbackCash) {
        selectedSourceName = when {
            sources.isEmpty() -> fallbackCash
            sources.none { it.name == selectedSourceName } -> sources.first().name
            selectedSourceName.isBlank() -> sources.first().name
            else -> selectedSourceName
        }
    }

    val rowInteractionSource = remember { MutableInteractionSource() }
    val isPressed by rowInteractionSource.collectIsPressedAsState()
    val rowScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = KatSpringSpec,
        label = "debtRowScale"
    )

    @Suppress("DEPRECATION")
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (!debt.isPaid) {
                        payAmountInput = ""
                        showPayDialog = true
                    }
                    false
                }
                SwipeToDismissBoxValue.Settled -> true
            }
        }
    )

    val isBorrowedDebt = debt.type == DebtEntity.TYPE_DEBT
    val indicatorColor = if (isBorrowedDebt) {
        colors.warning
    } else {
        colors.info
    }
    val statusColor = when {
        debt.isPaid -> colors.positive
        isBorrowedDebt -> colors.warning
        else -> colors.info
    }
    val rowShape = RoundedCornerShape(22.dp)

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !debt.isPaid,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val bgColor by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> colors.negative.copy(alpha = 0.18f)
                    SwipeToDismissBoxValue.StartToEnd -> colors.positive.copy(alpha = 0.18f)
                    else -> Color.Transparent
                },
                label = "debtSwipeBg"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .clip(rowShape)
                    .background(bgColor),
                contentAlignment = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                }
            ) {
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Text(
                        text = katStringResource(id = R.string.debt_swipe_pay, isEng = isEng),
                        color = colors.positive,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 24.dp)
                    )
                    SwipeToDismissBoxValue.EndToStart -> Text(
                        text = katStringResource(id = R.string.btn_delete, isEng = isEng),
                        color = colors.negative,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(end = 24.dp)
                    )
                    else -> Unit
                }
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .scale(rowScale)
                .clip(rowShape)
                .background(colors.card.copy(alpha = 0.9f))
                .border(1.dp, colors.border.copy(alpha = 0.56f), rowShape)
                .clickable(
                    interactionSource = rowInteractionSource,
                    indication = LocalIndication.current,
                    onClick = onEdit
                )
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(indicatorColor.copy(alpha = 0.12f))
                            .border(
                                width = 1.dp,
                                color = indicatorColor.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (debt.type == DebtEntity.TYPE_DEBT) {
                                katStringResource(id = R.string.debt_row_borrow, isEng = isEng)
                            } else {
                                katStringResource(id = R.string.debt_row_lend, isEng = isEng)
                            },
                            color = indicatorColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = debt.personName,
                        color = colors.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (debt.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = debt.note,
                        color = colors.subText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatDateOnly(debt.timestamp),
                    color = colors.subText,
                    fontSize = 10.sp,
                    maxLines = 1
                )

                if (debt.dueDate != null && !debt.isPaid) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${katStringResource(id = R.string.debt_label_due_date, isEng = isEng)} ${formatDateOnly(debt.dueDate!!)}",
                        color = if (debt.dueDate!! < System.currentTimeMillis()) colors.negative else colors.subText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formatSmartCurrency(debt.amount, debt.currency, isEng),
                    color = colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 128.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                val prefixLabel = if (debt.type == DebtEntity.TYPE_DEBT) {
                    katStringResource(id = R.string.debt_prefix_paid, isEng = isEng)
                } else {
                    katStringResource(id = R.string.debt_prefix_recv, isEng = isEng)
                }

                Text(
                    text = if (debt.isPaid) {
                        katStringResource(id = R.string.debt_status_completed, isEng = isEng)
                    } else {
                        "$prefixLabel ${formatSmartCurrency(debt.paidAmount, debt.currency, isEng)}"
                    },
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(statusColor.copy(alpha = 0.10f))
                        .border(
                            width = 1.dp,
                            color = statusColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }

    if (showPayDialog) {
        val accountOptions = sources.map { it.name }.ifEmpty { listOf(fallbackCash) }

        DebtPaymentDialog(
            isEng = isEng,
            debt = debt,
            colors = colors,
            payAmountInput = payAmountInput,
            onPayAmountChange = { payAmountInput = it },
            payCurrency = payCurrency,
            onPayCurrencyChange = { payCurrency = it },
            accountOptions = accountOptions,
            selectedSourceName = selectedSourceName,
            onSourceSelected = { selectedSourceName = it },
            onDismiss = {
                showPayDialog = false
                payAmountInput = ""
            },
            onConfirm = {
                val inputVal = parseMoney(payAmountInput)
                if (inputVal > 0.0 && selectedSourceName.isNotBlank()) {
                    onPay(inputVal, selectedSourceName)
                    showPayDialog = false
                    payAmountInput = ""
                }
            }
        )
    }
}

@Composable
fun DebtTab(
    isEng: Boolean,
    colors: BudgetColors,
    debts: List<DebtEntity>,
    sources: List<SourceEntity>,
    onCreateDebt: () -> Unit,
    onEditDebt: (DebtEntity) -> Unit,
    onDeleteDebt: (DebtEntity) -> Unit,
    onPayDebt: (DebtEntity, Double, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val titleAll = katStringResource(id = R.string.debt_tab_all, isEng = isEng)
    val titleLent = katStringResource(id = R.string.debt_chip_lent, isEng = isEng)
    val titleBorrowed = katStringResource(id = R.string.debt_chip_borrowed, isEng = isEng)
    val tabTitles = listOf(titleAll, titleLent, titleBorrowed)

    val (activeDebts, paidDebts) = debts.partition { !it.isPaid }

    val filteredActiveDebts = activeDebts.filter { debt ->
        when (selectedTab) {
            1 -> debt.type == DebtEntity.TYPE_LOAN
            2 -> debt.type == DebtEntity.TYPE_DEBT
            else -> true
        }
    }
    val filteredPaidDebts = paidDebts.filter { debt ->
        when (selectedTab) {
            1 -> debt.type == DebtEntity.TYPE_LOAN
            2 -> debt.type == DebtEntity.TYPE_DEBT
            else -> true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        KatTabRow(
            titles = tabTitles,
            selectedTab = selectedTab,
            colors = colors,
            onTabSelected = { selectedTab = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader(
            title = katStringResource(id = R.string.debt_tab_title, isEng = isEng),
            action = katStringResource(id = R.string.btn_add, isEng = isEng),
            colors = colors,
            onAction = onCreateDebt
        )

        if (filteredActiveDebts.isEmpty() && filteredPaidDebts.isEmpty()) {
            EmptyState(
                text = katStringResource(id = R.string.debt_no_debts, isEng = isEng),
                colors = colors
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (filteredActiveDebts.isNotEmpty()) {
                    items(filteredActiveDebts, key = { it.id }) { debt ->
                        DebtRow(
                            isEng = isEng,
                            debt = debt,
                            sources = sources,
                            colors = colors,
                            onEdit = { onEditDebt(debt) },
                            onDelete = { onDeleteDebt(debt) },
                            onPay = { amount, source -> onPayDebt(debt, amount, source) }
                        )
                    }
                }
                
                if (filteredPaidDebts.isNotEmpty()) {
                    if (filteredActiveDebts.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    items(filteredPaidDebts, key = { it.id }) { debt ->
                        DebtRow(
                            isEng = isEng,
                            debt = debt,
                            sources = sources,
                            colors = colors,
                            onEdit = { onEditDebt(debt) },
                            onDelete = { onDeleteDebt(debt) },
                            onPay = { amount, source -> onPayDebt(debt, amount, source) }
                        )
                    }
                }
            }
        }
    }
}
