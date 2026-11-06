package com.example.vinho.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinho.core.toUserMessage
import com.example.vinho.data.auth.AuthRepository
import com.example.vinho.data.budget.BudgetRepository
import com.example.vinho.data.model.Budget
import com.example.vinho.data.model.Category
import com.example.vinho.data.model.Transaction
import com.example.vinho.data.model.TransactionType
import com.example.vinho.data.model.UserProfile
import com.example.vinho.data.transaction.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth
import javax.inject.Inject

/** Tổng chi của một danh mục + tỉ lệ trên tổng chi (0f..1f). */
data class CategoryTotal(
    val category: Category,
    val amount: Long,
    val share: Float,
)

data class OverviewUiState(
    val month: YearMonth = YearMonth.now(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val userName: String = "",
    val income: Long = 0L,
    val expense: Long = 0L,
    val byCategory: List<CategoryTotal> = emptyList(),
    val recent: List<Transaction> = emptyList(),
    val budgetLimit: Long = 0L,
    val budgetSpent: Long = 0L,
) {
    val balance: Long get() = income - expense
    val hasBudget: Boolean get() = budgetLimit > 0
    val budgetProgress: Float
        get() = if (budgetLimit <= 0L) 0f else (budgetSpent.toFloat() / budgetLimit).coerceIn(0f, 1f)
    val budgetOver: Long get() = (budgetSpent - budgetLimit).coerceAtLeast(0L)
    val hasAnyTransaction: Boolean get() = income > 0L || expense > 0L
}

/**
 * ViewModel màn Tổng quan.
 *
 * Luồng dữ liệu: (user, tháng) -> observeMonth() của repository -> tổng hợp thành UiState.
 * Toàn bộ phép tính nằm trong [buildState] (hàm thuần) nên unit test chỉ cần fake repository.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    /** Tăng lên khi người dùng bấm "Thử lại" -> dựng lại toàn bộ luồng dữ liệu. */
    private val generation = MutableStateFlow(0)

    private val transactions: Flow<List<Transaction>> =
        combine(authRepository.currentUser, month) { user, m -> user to m }
            .flatMapLatest { (user, m) ->
                if (user == null) flowOf(emptyList())
                else transactionRepository.observeMonth(user.uid, m)
            }

    private val budgets: Flow<List<Budget>> =
        combine(authRepository.currentUser, month) { user, m -> user to m }
            .flatMapLatest { (user, m) ->
                if (user == null) flowOf(emptyList())
                else budgetRepository.observeMonth(user.uid, m)
            }

    val uiState: StateFlow<OverviewUiState> = generation
        .flatMapLatest {
            combine(authRepository.currentUser, month, transactions, budgets) { user, m, txs, bs ->
                buildState(user, m, txs, bs)
            }.catch { error ->
                emit(OverviewUiState(month = month.value, isLoading = false, error = error.toUserMessage()))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OverviewUiState(),
        )

    fun onPreviousMonth() = month.update { it.minusMonths(1) }

    fun onNextMonth() = month.update { it.plusMonths(1) }

    fun refresh() = generation.update { it + 1 }

    private fun buildState(
        user: UserProfile?,
        month: YearMonth,
        transactions: List<Transaction>,
        budgets: List<Budget>,
    ): OverviewUiState {
        val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
        val expense = expenseTransactions.sumOf { it.amount }

        val byCategory = expenseTransactions
            .groupBy { it.category }
            .map { (category, items) ->
                val amount = items.sumOf { it.amount }
                CategoryTotal(
                    category = category,
                    amount = amount,
                    share = if (expense <= 0L) 0f else amount.toFloat() / expense,
                )
            }
            .sortedByDescending { it.amount }

        // Chỉ tính vào "đã dùng ngân sách" những danh mục thực sự có đặt giới hạn
        val budgetedCategories = budgets.map { it.category }.toSet()
        val budgetSpent = expenseTransactions
            .filter { it.category in budgetedCategories }
            .sumOf { it.amount }

        return OverviewUiState(
            month = month,
            isLoading = false,
            error = null,
            userName = user?.displayName.orEmpty(),
            income = income,
            expense = expense,
            byCategory = byCategory,
            recent = transactions.take(5),
            budgetLimit = budgets.sumOf { it.limit },
            budgetSpent = budgetSpent,
        )
    }
}
