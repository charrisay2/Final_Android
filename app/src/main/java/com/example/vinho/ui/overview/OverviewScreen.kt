package com.example.vinho.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vinho.R
import com.example.vinho.core.formatMoney
import com.example.vinho.core.formatPercent
import com.example.vinho.ui.components.DonutChart
import com.example.vinho.ui.components.DonutSlice
import com.example.vinho.ui.components.EmptyState
import com.example.vinho.ui.components.ErrorState
import com.example.vinho.ui.components.LoadingState
import com.example.vinho.ui.components.MonthSelector
import com.example.vinho.ui.components.SectionHeader
import com.example.vinho.ui.components.StatCard
import com.example.vinho.ui.components.TransactionRow
import com.example.vinho.ui.theme.CategoryColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    onAddTransaction: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onSeeAllTransactions: () -> Unit,
    onOpenBudget: () -> Unit,
    viewModel: OverviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (state.userName.isBlank()) stringResource(R.string.overview_title)
                            else stringResource(R.string.overview_greeting, state.userName),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransaction,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add)) },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.error != null -> ErrorState(
                message = state.error!!,
                onRetry = viewModel::refresh,
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    MonthSelector(
                        month = state.month,
                        onPrevious = viewModel::onPreviousMonth,
                        onNext = viewModel::onNextMonth,
                    )
                }

                item {
                    BalanceCard(
                        balance = state.balance,
                        income = state.income,
                        expense = state.expense,
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(
                            label = stringResource(R.string.overview_income),
                            value = formatMoney(state.income),
                            icon = Icons.Filled.ArrowUpward,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            label = stringResource(R.string.overview_expense),
                            value = formatMoney(state.expense),
                            icon = Icons.Filled.ArrowDownward,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                item { BudgetCard(state = state, onOpenBudget = onOpenBudget) }

                if (state.byCategory.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.overview_by_category),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    item { CategoryBreakdown(state = state) }
                }

                item {
                    SectionHeader(
                        title = stringResource(R.string.overview_recent),
                        modifier = Modifier.padding(top = 4.dp),
                        action = {
                            TextButton(onClick = onSeeAllTransactions) {
                                Text(stringResource(R.string.overview_see_all))
                            }
                        },
                    )
                }

                if (!state.hasAnyTransaction) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            EmptyState(
                                icon = Icons.Filled.PieChart,
                                title = stringResource(R.string.tx_empty_title),
                                body = stringResource(R.string.overview_empty),
                                modifier = Modifier.height(220.dp),
                            )
                        }
                    }
                } else {
                    items(state.recent, key = { it.id }) { transaction ->
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

@Composable
private fun BalanceCard(balance: Long, income: Long, expense: Long) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.overview_balance), style = MaterialTheme.typography.labelLarge)
            Text(
                text = formatMoney(balance),
                style = MaterialTheme.typography.displaySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "↑ ${formatMoney(income)}   ↓ ${formatMoney(expense)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun BudgetCard(state: OverviewUiState, onOpenBudget: () -> Unit) {
    Card(
        onClick = onOpenBudget,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.budget_total), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (state.hasBudget) {
                        "${formatPercent(state.budgetSpent.toFloat() / state.budgetLimit * 100f)}"
                    } else {
                        stringResource(R.string.budget_no_limit)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (state.budgetOver > 0L) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                )
            }

            if (state.hasBudget) {
                LinearProgressIndicator(
                    progress = { state.budgetProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = if (state.budgetOver > 0L) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (state.budgetOver > 0L) {
                        stringResource(R.string.overview_budget_over, formatMoney(state.budgetOver))
                    } else {
                        stringResource(
                            R.string.budget_spent,
                            formatMoney(state.budgetSpent),
                            formatMoney(state.budgetLimit),
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.overview_no_budget),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdown(state: OverviewUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DonutChart(
                slices = state.byCategory.map {
                    DonutSlice(CategoryColors.of(it.category), it.amount.toFloat(), "")
                },
                centerLabel = stringResource(R.string.overview_expense),
                centerValue = formatMoney(state.expense),
                modifier = Modifier.size(132.dp),
                strokeWidth = 18.dp,
            )
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.byCategory.take(4).forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = CategoryColors.of(item.category),
                            modifier = Modifier.size(10.dp),
                            shape = CircleShape,
                        ) {}
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(item.category.labelRes),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = formatPercent(item.share * 100f),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}
