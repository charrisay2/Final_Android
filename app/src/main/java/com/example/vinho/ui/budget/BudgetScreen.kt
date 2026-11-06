package com.example.vinho.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vinho.R
import com.example.vinho.core.formatMoney
import com.example.vinho.core.formatNumber
import com.example.vinho.core.parseAmount
import com.example.vinho.data.model.Category
import com.example.vinho.ui.components.CategoryAvatar
import com.example.vinho.ui.components.EmptyState
import com.example.vinho.ui.components.ErrorState
import com.example.vinho.ui.components.LoadingState
import com.example.vinho.ui.components.MonthSelector

private sealed interface EditorState {
    data object New : EditorState
    data class Edit(val line: BudgetLine) : EditorState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(viewModel: BudgetViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<EditorState?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.budget_title)) }) },
        floatingActionButton = {
            if (state.hasBudgets && state.unbudgetedCategories.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { editor = EditorState.New },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.budget_add)) },
                )
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            MonthSelector(
                month = state.month,
                onPrevious = viewModel::onPreviousMonth,
                onNext = viewModel::onNextMonth,
            )

            when {
                state.isLoading -> LoadingState()
                state.error != null -> ErrorState(state.error!!, viewModel::refresh)
                !state.hasBudgets -> EmptyState(
                    icon = Icons.Filled.Savings,
                    title = stringResource(R.string.budget_empty_title),
                    body = stringResource(R.string.budget_empty_body),
                    action = {
                        TextButton(onClick = { editor = EditorState.New }) {
                            Text(stringResource(R.string.budget_add))
                        }
                    },
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { BudgetSummary(state) }
                    items(state.lines, key = { it.category.name }) { line ->
                        BudgetRow(line = line, onClick = { editor = EditorState.Edit(line) })
                    }
                }
            }
        }
    }

    editor?.let { current ->
        BudgetEditorDialog(
            editorState = current,
            availableCategories = when (current) {
                is EditorState.New -> state.unbudgetedCategories
                is EditorState.Edit -> listOf(current.line.category)
            },
            onDismiss = { editor = null },
            onSave = { category, limit ->
                viewModel.saveBudget(category, limit) { saved -> if (saved) editor = null }
            },
            onDelete = { category ->
                viewModel.deleteBudget(category)
                editor = null
            },
        )
    }
}

@Composable
private fun BudgetSummary(state: BudgetUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.budget_total), style = MaterialTheme.typography.labelLarge)
            Text(formatMoney(state.totalLimit), style = MaterialTheme.typography.headlineSmall)
            LinearProgressIndicator(
                progress = { state.totalProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (state.totalOver > 0L) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
            )
            Text(
                text = if (state.totalOver > 0L) {
                    stringResource(R.string.budget_over, formatMoney(state.totalOver))
                } else {
                    stringResource(R.string.budget_remaining, formatMoney(state.totalRemaining))
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun BudgetRow(line: BudgetLine, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryAvatar(line.category, size = 36.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(line.category.labelRes),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            R.string.budget_spent,
                            formatMoney(line.spent),
                            formatMoney(line.limit),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = if (line.isOver) {
                        stringResource(R.string.budget_over, formatMoney(line.over))
                    } else {
                        stringResource(R.string.budget_remaining, formatMoney(line.remaining))
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (line.isOver) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                )
            }
            LinearProgressIndicator(
                progress = { line.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (line.isOver) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BudgetEditorDialog(
    editorState: EditorState,
    availableCategories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Category, Long) -> Unit,
    onDelete: (Category) -> Unit,
) {
    val initial = (editorState as? EditorState.Edit)?.line
    var category by remember { mutableStateOf(initial?.category ?: availableCategories.firstOrNull()) }
    var amountText by remember {
        mutableStateOf(initial?.limit?.let { formatNumber(it) } ?: "")
    }
    val amount = parseAmount(amountText)
    val isValid = category != null && amount != null && amount > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.budget_add else R.string.budget_edit,
                ),
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (initial == null) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        availableCategories.forEach { item ->
                            FilterChip(
                                selected = category == item,
                                onClick = { category = item },
                                label = { Text("${item.emoji} ${stringResource(item.labelRes)}") },
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryAvatar(initial.category, size = 32.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(initial.category.labelRes))
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(12)
                        amountText = digits.toLongOrNull()?.let { formatNumber(it) } ?: ""
                    },
                    label = { Text(stringResource(R.string.budget_limit_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (isValid) onSave(category!!, amount!!) }, enabled = isValid) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row {
                if (initial != null) {
                    TextButton(onClick = { onDelete(initial.category) }) {
                        Text(
                            text = stringResource(R.string.budget_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}
