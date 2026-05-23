package com.katgr0up.katbudget.ui.tabs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.BudgetEntity
import com.katgr0up.katbudget.data.local.entity.CategoryEntity
import com.katgr0up.katbudget.data.local.entity.SourceEntity
import com.katgr0up.katbudget.data.local.entity.TransactionEntity
import com.katgr0up.katbudget.managers.AppUpdater
import com.katgr0up.katbudget.ui.components.*
import com.katgr0up.katbudget.ui.utils.katStringResource
import com.katgr0up.katbudget.ui.utils.rememberCategoryNameLocalizer

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardTab(
    isEng: Boolean,
    defaultCurrency: String,
    transactions: List<TransactionEntity>,
    allTransactions: List<TransactionEntity>,
    isShowingAllTransactions: Boolean,
    balance: Map<String, Double>,
    grandTotal: Double,
    sources: List<SourceEntity>,
    sourceBalances: Map<String, Map<String, Double>>,
    budgets: List<BudgetEntity>,
    categories: List<CategoryEntity>,
    selectedSourceFilter: String,
    searchQuery: String,
    colors: BudgetColors,
    exchangeRates: Map<String, Double>,
    onSourceFilterChanged: (String) -> Unit,
    onSearchChanged: (String) -> Unit,
    onCreateSource: () -> Unit,
    onEditSource: (SourceEntity) -> Unit,
    onDeleteSource: (SourceEntity) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onToggleTransactionsExpanded: () -> Unit,
    onViewImage: (String) -> Unit
) {
    val context = LocalContext.current

    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateApkUrl by remember { mutableStateOf("") }
    var updateReleaseNotes by remember { mutableStateOf("") }
    var updateVersionName by remember { mutableStateOf("") }

    val fallbackNew = katStringResource(id = R.string.fallback_version_new, isEng = isEng)
    val fallbackNotes = katStringResource(id = R.string.settings_update_release_notes_fallback, isEng = isEng)

    LaunchedEffect(Unit) {
        val result = AppUpdater.checkForUpdate(
            context = context,
            fallbackVersionName = fallbackNew,
            fallbackReleaseNotes = fallbackNotes
        )

        if (result is AppUpdater.UpdateCheckResult.Available) {
            updateApkUrl = result.update.apkUrl
            updateVersionName = result.update.versionName
            updateReleaseNotes = result.update.releaseNotes
            showUpdateDialog = true
        }
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = colors.surface,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "${katStringResource(id = R.string.settings_update_available_title, isEng = isEng)} $updateVersionName",
                    color = colors.text,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = updateReleaseNotes,
                    color = colors.subText
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(48.dp),
                    onClick = {
                        showUpdateDialog = false
                        AppUpdater.downloadApk(context, updateApkUrl)
                    }
                ) {
                    Text(
                        text = katStringResource(id = R.string.btn_update_now, isEng = isEng),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text(
                        text = katStringResource(id = R.string.btn_later, isEng = isEng),
                        color = colors.subText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }

    val allTag = katStringResource(id = R.string.fallback_all, isEng = isEng)
    val localizeCategory = rememberCategoryNameLocalizer(isEng)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 112.dp)
    ) {
        item {
            AssetSummaryCard(
                isEng = isEng,
                defaultCurrency = defaultCurrency,
                selectedSourceFilter = selectedSourceFilter,
                balance = balance,
                grandTotal = grandTotal,
                sourceBalances = sourceBalances,
                colors = colors,
                exchangeRates = exchangeRates
            )
        }

        item {
            SectionHeader(
                title = katStringResource(id = R.string.tab_header_accounts, isEng = isEng),
                action = katStringResource(id = R.string.btn_add, isEng = isEng),
                colors = colors,
                onAction = onCreateSource
            )
        }

        item {
            SourceFilterRow(
                isEng = isEng,
                sources = sources,
                sourceBalances = sourceBalances,
                defaultCurrency = defaultCurrency,
                selectedSourceFilter = selectedSourceFilter,
                colors = colors,
                exchangeRates = exchangeRates,
                onSourceFilterChanged = onSourceFilterChanged,
                onEditSource = onEditSource
            )
        }

        item {
            val isAll = selectedSourceFilter == allTag ||
                    selectedSourceFilter == "Tất cả" ||
                    selectedSourceFilter == "All"
            val source = sources.find { it.name == selectedSourceFilter }

            val titleText = if (isAll) {
                katStringResource(id = R.string.tab_header_recent_tx, isEng = isEng)
            } else {
                "${katStringResource(id = R.string.tab_header_tx, isEng = isEng)}: $selectedSourceFilter"
            }

            SectionHeader(
                title = titleText,
                action = if (!isAll) katStringResource(id = R.string.btn_delete, isEng = isEng) else null,
                colors = colors,
                onAction = { if (!isAll) source?.let(onDeleteSource) }
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChanged,
                placeholder = {
                    Text(
                        text = katStringResource(id = R.string.tab_search_placeholder, isEng = isEng),
                        color = colors.subText
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                singleLine = true,
                colors = appTextFieldColors(colors),
                shape = RoundedCornerShape(20.dp)
            )
        }

        val filteredTransactions = transactions
            .filter {
                selectedSourceFilter == allTag ||
                        selectedSourceFilter == "Tất cả" ||
                        selectedSourceFilter == "All" ||
                        it.sourceName == selectedSourceFilter
            }
            .filter {
                searchQuery.isBlank() ||
                        it.category.contains(searchQuery, true) ||
                        localizeCategory(it.category).contains(searchQuery, true) ||
                        it.note.contains(searchQuery, true) ||
                        it.amount.toString().contains(searchQuery)
            }

        if (filteredTransactions.isEmpty()) {
            item {
                EmptyState(
                    text = katStringResource(id = R.string.tab_empty_transactions, isEng = isEng),
                    colors = colors
                )
            }
        } else {
            val visibleTransactions = if (isShowingAllTransactions) {
                filteredTransactions
            } else {
                filteredTransactions.take(5)
            }

            items(visibleTransactions, key = { it.id }) { transaction ->
                TransactionRow(
                    modifier = Modifier.animateItem(),
                    transaction = transaction,
                    transactions = allTransactions,
                    budgets = budgets,
                    categories = categories,
                    isEng = isEng,
                    colors = colors,
                    onEdit = { onEditTransaction(transaction) },
                    onDelete = { onDeleteTransaction(transaction) },
                    onViewImage = onViewImage
                )
            }

            if (filteredTransactions.size > 5) {
                item {
                    TextButton(
                        onClick = onToggleTransactionsExpanded,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = katStringResource(
                                id = if (isShowingAllTransactions) {
                                    R.string.dashboard_show_less_transactions
                                } else {
                                    R.string.dashboard_view_more_transactions
                                },
                                isEng = isEng
                            ),
                            color = colors.accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
