package com.example.vinho.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.vinho.core.formatDate
import com.example.vinho.core.formatMoney
import com.example.vinho.core.formatPercent
import com.example.vinho.core.monthLabel
import com.example.vinho.ui.components.EmptyState
import com.example.vinho.ui.components.ErrorState
import com.example.vinho.ui.components.LoadingState
import com.example.vinho.ui.components.MonthBar
import com.example.vinho.ui.components.MonthlyBarChart
import com.example.vinho.ui.theme.CategoryColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.stats_title)) }) },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.error != null -> ErrorState(state.error!!, viewModel::refresh, Modifier.padding(padding))
            !state.hasData -> EmptyState(
                icon = Icons.Filled.BarChart,
                title = stringResource(R.string.stats_empty),
                body = stringResource(R.string.tx_empty_body),
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { TrendCard(state) }
                item { MetricsCard(state) }
                item { TopCategoriesCard(state) }
            }
        }
    }
}

@Composable
private fun TrendCard(state: StatsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.stats_last_months), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendDot(stringResource(R.string.overview_income), MaterialTheme.colorScheme.primary)
                    LegendDot(stringResource(R.string.overview_expense), MaterialTheme.colorScheme.error)
                }
            }
            MonthlyBarChart(
                bars = state.months.map { total ->
                    MonthBar(
                        label = "T${total.month.monthValue}",
                        income = total.income,
                        expense = total.expense,
                    )
                },
            )
        }
    }
}

@Composable
private fun LegendDot(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MetricsCard(state: StatsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "${stringResource(R.string.stats_month_total)} · ${state.currentMonth.monthLabel()}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(formatMoney(state.currentExpense), style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile(
                    label = stringResource(R.string.stats_avg_day),
                    value = formatMoney(state.averagePerDay),
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    label = stringResource(R.string.stats_biggest),
                    value = state.biggestExpense?.let { formatMoney(it.amount) } ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }
            state.biggestExpense?.let { biggest ->
                Text(
                    text = "${stringResource(biggest.category.labelRes)} · ${formatDate(biggest.date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TopCategoriesCard(state: StatsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.stats_top_categories),
                style = MaterialTheme.typography.titleMedium,
            )
            state.topCategories.take(5).forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${item.category.emoji} ${stringResource(item.category.labelRes)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "${formatMoney(item.amount)} · ${formatPercent(item.share * 100f)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { item.share.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = CategoryColors.of(item.category),
                    )
                }
            }
        }
    }
}
