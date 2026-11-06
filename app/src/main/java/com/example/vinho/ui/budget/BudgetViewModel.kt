package com.example.vinho.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinho.core.monthKey
import com.example.vinho.core.toUserMessage
import com.example.vinho.data.auth.AuthRepository
import com.example.vinho.data.budget.BudgetRepository
import com.example.vinho.data.model.Budget
import com.example.vinho.data.model.Category
import com.example.vinho.data.model.TransactionType
import com.example.vinho.data.transaction.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

/** Một dòng ngân sách: giới hạn + đã chi trong tháng. */
data class BudgetLine(
    val category: Category,
    val limit: Long,
    val spent: Long,
) {
    /** 0f..1f (đã kẹp) để vẽ progress bar. */
    val progress: Float get() = if (limit <= 0L) 0f else (spent.toFloat() / limit).coerceIn(0f, 1f)

    val remaining: Long get() = (limit - spent).coerceAtLeast(0L)

    val over: Long get() = (spent - limit).coerceAtLeast(0L)

    val isOver: Boolean get() = over > 0L
}

data class BudgetUiState(
    val month: YearMonth = YearMonth.now(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val lines: List<BudgetLine> = emptyList(),
    val totalLimit: Long = 0L,
    val totalSpent: Long = 0L,
) {
    val hasBudgets: Boolean get() = lines.isNotEmpty()
    val totalProgress: Float
        get() = if (totalLimit <= 0L) 0f else (totalSpent.toFloat() / totalLimit).coerceIn(0f, 1f)
    val totalOver: Long get() = (totalSpent - totalLimit).coerceAtLeast(0L)
    val totalRemaining: Long get() = (totalLimit - totalSpent).coerceAtLeast(0L)

    /** Danh mục chi chưa có ngân sách — dùng cho hộp thoại thêm mới. */
    val unbudgetedCategories: List<Category>
        get() = Category.of(TransactionType.EXPENSE).filter { category ->
            lines.none { it.category == category }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val generation = MutableStateFlow(0)

    /** Lỗi khi ghi dữ liệu (thêm/xoá ngân sách) — gộp vào UiState để UI hiện được. */
    private val errorState = MutableStateFlow<String?>(null)

    private val budgets: Flow<List<Budget>> =
        combine(authRepository.currentUser, month) { user, m -> user to m }
            .flatMapLatest { (user, m) ->
                if (user == null) flowOf(emptyList())
                else budgetRepository.observeMonth(user.uid, m)
            }

    /** Tháng + tổng chi theo danh mục trong tháng đó. */
    private val transactions: Flow<Pair<YearMonth, Map<Category, Long>>> =
        combine(authRepository.currentUser, month) { user, m -> user to m }
            .flatMapLatest { (user, m) ->
                if (user == null) {
                    flowOf(m to emptyMap())
                } else {
                    transactionRepository.observeMonth(user.uid, m).map { txs ->
                        m to txs
                            .filter { it.type == TransactionType.EXPENSE }
                            .groupBy { it.category }
                            .mapValues { (_, items) -> items.sumOf { it.amount } }
                    }
                }
            }

    val uiState: StateFlow<BudgetUiState> = generation
        .flatMapLatest {
            combine(budgets, transactions, errorState) { budgetList, (currentMonth, spentByCategory), error ->
                buildState(currentMonth, budgetList, spentByCategory).copy(error = error)
            }.catch { error ->
                emit(BudgetUiState(isLoading = false, error = error.toUserMessage()))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BudgetUiState(),
        )

    fun onPreviousMonth() = month.update { it.minusMonths(1) }

    fun onNextMonth() = month.update { it.plusMonths(1) }

    fun refresh() = generation.update { it + 1 }

    /** Thêm hoặc cập nhật giới hạn. Trả về false nếu số tiền không hợp lệ. */
    fun saveBudget(category: Category, limit: Long, onResult: (Boolean) -> Unit = {}) {
        if (limit <= 0L) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            try {
                val uid = currentUid()
                budgetRepository.upsert(uid, Budget(category, limit, month.value.monthKey()))
                onResult(true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorState.value = e.toUserMessage()
                onResult(false)
            }
        }
    }

    fun deleteBudget(category: Category) {
        viewModelScope.launch {
            try {
                budgetRepository.delete(currentUid(), month.value, category)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorState.value = e.toUserMessage()
            }
        }
    }

    private fun buildState(
        month: YearMonth,
        budgets: List<Budget>,
        spentByCategory: Map<Category, Long>,
    ): BudgetUiState {
        val lines = budgets
            .map { budget ->
                BudgetLine(
                    category = budget.category,
                    limit = budget.limit,
                    spent = spentByCategory[budget.category] ?: 0L,
                )
            }
            .sortedWith(compareByDescending<BudgetLine> { it.progress }.thenBy { it.category.name })

        return BudgetUiState(
            month = month,
            isLoading = false,
            error = null,
            lines = lines,
            totalLimit = lines.sumOf { it.limit },
            totalSpent = lines.sumOf { it.spent },
        )
    }

    private suspend fun currentUid(): String =
        authRepository.currentUser.first()?.uid ?: error("Chưa đăng nhập")
}
