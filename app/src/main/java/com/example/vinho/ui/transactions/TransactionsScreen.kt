package com.example.vinho.ui.transactions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vinho.R
import com.example.vinho.core.formatDate
import com.example.vinho.core.toEpochMillis
import com.example.vinho.core.formatMoney
import com.example.vinho.data.model.Category
import com.example.vinho.data.model.TransactionType
import com.example.vinho.ui.components.CategoryFilterChip
import com.example.vinho.ui.components.EmptyState
import com.example.vinho.ui.components.ErrorState
import com.example.vinho.ui.components.LoadingState
import com.example.vinho.ui.components.MonthSelector
import com.example.vinho.ui.components.TransactionRow
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onAddTransaction: () -> Unit,
    onTransactionClick: (String) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tx_title)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransaction,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add)) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            MonthSelector(
                month = state.month,
                onPrevious = viewModel::onPreviousMonth,
                onNext = viewModel::onNextMonth,
            )

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text(stringResource(R.string.tx_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )

            TypeFilterRow(
                selected = state.typeFilter,
                onSelect = viewModel::onTypeFilterChange,
            )

            // Chip danh mục chỉ hiện danh mục thuộc loại đang lọc -> danh sách ngắn, dễ chọn
            val categories = when (state.typeFilter) {
                TransactionType.INCOME -> Category.of(TransactionType.INCOME)
                TransactionType.EXPENSE -> Category.of(TransactionType.EXPENSE)
                null -> Category.entries.toList()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.categoryFilter == null,
                    onClick = { viewModel.onCategoryFilterChange(null) },
                    label = { Text(stringResource(R.string.tx_filter_all)) },
                )
                categories.forEach { category ->
                    CategoryFilterChip(
                        category = category,
                        selected = state.categoryFilter == category,
                        onClick = { viewModel.onCategoryFilterChange(category) },
                    )
                }
            }

            SummaryLine(state)

            when {
                state.isLoading -> LoadingState()
                state.error != null -> ErrorState(state.error!!, viewModel::refresh)
                state.groups.isEmpty() -> EmptyState(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = stringResource(R.string.tx_empty_title),
                    body = if (state.query.isNotBlank()) {
                        stringResource(R.string.tx_empty_search, state.query)
                    } else {
                        stringResource(R.string.tx_empty_body)
                    },
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.groups.forEach { group ->
                        item(key = "header-${group.date}") {
                            DayHeader(date = group.date, expense = group.expense, income = group.income)
                        }
                        items(group.transactions, key = { it.id }) { transaction ->
                            TransactionRow(
                                transaction = transaction,
                                onClick = { onTransactionClick(transaction.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeFilterRow(selected: TransactionType?, onSelect: (TransactionType?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.tx_filter_all)) },
        )
        FilterChip(
            selected = selected == TransactionType.INCOME,
            onClick = { onSelect(TransactionType.INCOME) },
            label = { Text(stringResource(R.string.tx_filter_income)) },
        )
        FilterChip(
            selected = selected == TransactionType.EXPENSE,
            onClick = { onSelect(TransactionType.EXPENSE) },
            label = { Text(stringResource(R.string.tx_filter_expense)) },
        )
    }
}

@Composable
private fun SummaryLine(state: TransactionsUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.tx_count, state.totalCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "−${formatMoney(state.filteredExpense)}   +${formatMoney(state.filteredIncome)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayHeader(date: LocalDate, expense: Long, income: Long) {
    val today = LocalDate.now()
    val label = when (date) {
        today -> stringResource(R.string.tx_today)
        today.minusDays(1) -> stringResource(R.string.tx_yesterday)
        else -> formatDate(date.toEpochMillis())
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = buildString {
                if (expense > 0L) append("−${formatMoney(expense)}")
                if (expense > 0L && income > 0L) append("  ")
                if (income > 0L) append("+${formatMoney(income)}")
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
