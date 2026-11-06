package com.example.vinho.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinho.core.toUserMessage
import com.example.vinho.data.auth.AuthRepository
import com.example.vinho.data.model.Category
import com.example.vinho.data.model.Transaction
import com.example.vinho.data.model.TransactionType
import com.example.vinho.data.transaction.TransactionRepository
import com.example.vinho.ui.overview.CategoryTotal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class MonthlyTotal(
    val month: YearMonth,
    val income: Long,
    val expense: Long,
)

data class StatsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    /** 6 tháng gần nhất, cũ → mới. */
    val months: List<MonthlyTotal> = emptyList(),
    val currentMonth: YearMonth = YearMonth.now(),
    val currentIncome: Long = 0L,
    val currentExpense: Long = 0L,
    val topCategories: List<CategoryTotal> = emptyList(),
    val averagePerDay: Long = 0L,
    val biggestExpense: Transaction? = null,
) {
    val hasData: Boolean get() = months.any { it.expense > 0L || it.income > 0L }
}

/**
 * ViewModel màn Thống kê.
 *
 * Ghép 6 luồng observeMonth (mỗi tháng một luồng) bằng combine: mọi tháng đều là dữ liệu
 * thật từ Firestore, và khi một tháng thay đổi thì biểu đồ tự cập nhật.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val window: List<YearMonth> = run {
        val now = YearMonth.now()
        (MONTHS - 1 downTo 0).map { now.minusMonths(it.toLong()) }
    }

    private val monthlyData = authRepository.currentUser.flatMapLatest { user ->
        if (user == null) {
            flowOf(emptyList())
        } else {
            combine(window.map { month -> transactionRepository.observeMonth(user.uid, month) }) { arrays ->
                window.zip(arrays.toList())
            }
        }
    }

    /** Tăng khi bấm "Thử lại" -> dựng lại luồng dữ liệu. */
    private val generation = MutableStateFlow(0)

    val uiState: StateFlow<StatsUiState> = generation
        .flatMapLatest {
            monthlyData
                .map { data -> buildState(data) }
                .catch { error -> emit(StatsUiState(isLoading = false, error = error.toUserMessage())) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatsUiState(),
        )

    fun refresh() = generation.update { it + 1 }

    private fun buildState(data: List<Pair<YearMonth, List<Transaction>>>): StatsUiState {
        if (data.isEmpty()) {
            return StatsUiState(isLoading = false, months = emptyList())
        }

        val totals = data.map { (month, transactions) ->
            MonthlyTotal(
                month = month,
                income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
            )
        }

        val (currentMonth, currentTransactions) = data.last()
        val currentExpenses = currentTransactions.filter { it.type == TransactionType.EXPENSE }
        val currentExpense = currentExpenses.sumOf { it.amount }

        val topCategories = currentExpenses
            .groupBy { it.category }
            .map { (category: Category, items) ->
                val amount = items.sumOf { it.amount }
                CategoryTotal(
                    category = category,
                    amount = amount,
                    share = if (currentExpense <= 0L) 0f else amount.toFloat() / currentExpense,
                )
            }
            .sortedByDescending { it.amount }

        // Số ngày đã trôi qua của tháng hiện tại (tháng cũ thì lấy cả tháng)
        val today = LocalDate.now()
        val days = if (currentMonth == YearMonth.from(today)) today.dayOfMonth else currentMonth.lengthOfMonth()

        return StatsUiState(
            isLoading = false,
            months = totals,
            currentMonth = currentMonth,
            currentIncome = currentTransactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount },
            currentExpense = currentExpense,
            topCategories = topCategories,
            averagePerDay = if (days <= 0) 0L else currentExpense / days,
            biggestExpense = currentExpenses.maxByOrNull { it.amount },
        )
    }

    private companion object {
        const val MONTHS = 6
    }
}
