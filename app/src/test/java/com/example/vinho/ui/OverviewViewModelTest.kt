package com.example.vinho.ui

import com.example.vinho.FakeAuthRepository
import com.example.vinho.FakeBudgetRepository
import com.example.vinho.FakeTransactionRepository
import com.example.vinho.core.monthKey
import com.example.vinho.data.model.Budget
import com.example.vinho.data.model.Category
import com.example.vinho.data.model.TransactionType
import com.example.vinho.transaction
import com.example.vinho.ui.overview.OverviewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        transactions: FakeTransactionRepository = FakeTransactionRepository(),
        budgets: FakeBudgetRepository = FakeBudgetRepository(),
    ) = OverviewViewModel(FakeAuthRepository(), transactions, budgets)

    @Test
    fun `tổng thu, tổng chi và tỉ lệ từng danh mục được tính đúng`() = runTest {
        val transactions = FakeTransactionRepository().apply {
            this.transactions.value = listOf(
                transaction(5_000_000, TransactionType.INCOME, Category.SALARY, note = "Lương"),
                transaction(300_000, TransactionType.EXPENSE, Category.FOOD, note = "Ăn uống"),
                transaction(200_000, TransactionType.EXPENSE, Category.TRANSPORT, note = "Xe bus"),
            )
        }
        val vm = viewModel(transactions = transactions)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(5_000_000L, state.income)
        assertEquals(500_000L, state.expense)
        assertEquals(4_500_000L, state.balance)
        assertEquals(3, state.recent.size)

        assertEquals(2, state.byCategory.size)
        assertEquals(Category.FOOD, state.byCategory[0].category)
        assertEquals(300_000L, state.byCategory[0].amount)
        assertEquals(0.6f, state.byCategory[0].share, 0.001f)
        assertEquals(Category.TRANSPORT, state.byCategory[1].category)
        assertEquals(0.4f, state.byCategory[1].share, 0.001f)

        job.cancel()
    }

    @Test
    fun `chỉ chi tiêu trong danh mục có ngân sách mới bị tính vào tiến độ`() = runTest {
        val month = YearMonth.now()
        val transactions = FakeTransactionRepository().apply {
            this.transactions.value = listOf(
                transaction(300_000, TransactionType.EXPENSE, Category.FOOD),
                transaction(200_000, TransactionType.EXPENSE, Category.TRANSPORT),
            )
        }
        val budgets = FakeBudgetRepository().apply {
            this.budgets.value = listOf(Budget(Category.FOOD, 500_000L, month.monthKey()))
        }

        val vm = viewModel(transactions, budgets)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.hasBudget)
        assertEquals(500_000L, state.budgetLimit)
        // 300k Ăn uống nằm trong ngân sách, 200k Di chuyển không có ngân sách nên không tính
        assertEquals(300_000L, state.budgetSpent)
        assertEquals(0.6f, state.budgetProgress, 0.001f)
        assertEquals(0L, state.budgetOver)
        assertTrue(state.hasAnyTransaction)

        job.cancel()
    }
}
