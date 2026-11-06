package com.example.vinho.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinho.core.toLocalDate
import com.example.vinho.core.toUserMessage
import com.example.vinho.data.auth.AuthRepository
import com.example.vinho.data.model.Category
import com.example.vinho.data.model.Transaction
import com.example.vinho.data.model.TransactionType
import com.example.vinho.data.transaction.TransactionNewestFirst
import com.example.vinho.data.transaction.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** Giao dịch của một ngày (để vẽ tiêu đề nhóm "Hôm nay / 05/11"). */
data class DayGroup(
    val date: LocalDate,
    val transactions: List<Transaction>,
) {
    val expense: Long get() = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val income: Long get() = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
}

data class TransactionsUiState(
    val month: YearMonth = YearMonth.now(),
    val query: String = "",
    val typeFilter: TransactionType? = null,
    val categoryFilter: Category? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val groups: List<DayGroup> = emptyList(),
    val totalCount: Int = 0,
    val filteredExpense: Long = 0L,
    val filteredIncome: Long = 0L,
) {
    val isFiltering: Boolean get() = query.isNotBlank() || typeFilter != null || categoryFilter != null
}

/**
 * ViewModel màn Giao dịch.
 *
 * Tìm kiếm và lọc chạy trong bộ nhớ trên danh sách của tháng đã tải: mỗi tháng chỉ vài chục
 * giao dịch nên không cần query mạng cho từng ký tự gõ vào. Nhờ vậy lọc tức thì và test được
 * mà không cần Firestore.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val query = MutableStateFlow("")
    private val typeFilter = MutableStateFlow<TransactionType?>(null)
    private val categoryFilter = MutableStateFlow<Category?>(null)
    private val generation = MutableStateFlow(0)

    private val transactions = combine(authRepository.currentUser, month) { user, m -> user to m }
        .flatMapLatest { (user, m) ->
            if (user == null) flowOf(emptyList())
            else transactionRepository.observeMonth(user.uid, m)
        }

    val uiState: StateFlow<TransactionsUiState> = generation
        .flatMapLatest {
            combine(
                transactions,
                query,
                typeFilter,
                categoryFilter,
                month,
            ) { txs, q, type, category, currentMonth ->
                buildState(txs, q, type, category, currentMonth)
            }.catch { error ->
                emit(TransactionsUiState(isLoading = false, error = error.toUserMessage()))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TransactionsUiState(),
        )

    fun onQueryChange(value: String) = query.update { value }

    /** Bấm lại đúng chip đang chọn = bỏ lọc. */
    fun onTypeFilterChange(value: TransactionType?) =
        typeFilter.update { if (it == value) null else value }

    fun onCategoryFilterChange(value: Category?) =
        categoryFilter.update { if (it == value) null else value }

    fun onPreviousMonth() = month.update { it.minusMonths(1) }

    fun onNextMonth() = month.update { it.plusMonths(1) }

    fun refresh() = generation.update { it + 1 }

    private fun buildState(
        transactions: List<Transaction>,
        query: String,
        typeFilter: TransactionType?,
        categoryFilter: Category?,
        month: YearMonth,
    ): TransactionsUiState {
        val keyword = query.trim()
        val filtered = transactions.filter { transaction ->
            (typeFilter == null || transaction.type == typeFilter) &&
                (categoryFilter == null || transaction.category == categoryFilter) &&
                (keyword.isEmpty() || transaction.note.contains(keyword, ignoreCase = true))
        }

        val groups = filtered
            .groupBy { it.date.toLocalDate() }
            .entries
            .sortedByDescending { it.key }
            .map { (date, items) -> DayGroup(date = date, transactions = items.sortedWith(TransactionNewestFirst)) }

        return TransactionsUiState(
            month = month,
            query = query,
            typeFilter = typeFilter,
            categoryFilter = categoryFilter,
            isLoading = false,
            error = null,
            groups = groups,
            totalCount = filtered.size,
            filteredExpense = filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
            filteredIncome = filtered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
        )
    }
}
