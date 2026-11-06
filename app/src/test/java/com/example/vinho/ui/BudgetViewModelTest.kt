package com.example.vinho.ui

import com.example.vinho.FakeAuthRepository
import com.example.vinho.FakeBudgetRepository
import com.example.vinho.FakeTransactionRepository
import com.example.vinho.core.monthKey
import com.example.vinho.data.model.Budget
import com.example.vinho.data.model.Category
import com.example.vinho.data.model.TransactionType
import com.example.vinho.transaction
import com.example.vinho.ui.budget.BudgetViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `tiến độ và số tiền vượt ngân sách được tính đúng`() = runTest {
        val month = YearMonth.now()
        val budgets = FakeBudgetRepository().apply {
            this.budgets.value = listOf(Budget(Category.FOOD, 1_000_000L, month.monthKey()))
        }
        val transactions = FakeTransactionRepository().apply {
            this.transactions.value = listOf(
                transaction(1_200_000, TransactionType.EXPENSE, Category.FOOD),
                transaction(300_000, TransactionType.EXPENSE, Category.SHOPPING),
            )
        }

        val vm = BudgetViewModel(FakeAuthRepository(), budgets, transactions)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(1, state.lines.size)
        val line = state.lines.first()
        assertEquals(1_000_000L, line.limit)
        assertEquals(1_200_000L, line.spent)
        assertTrue(line.isOver)
        assertEquals(200_000L, line.over)
        assertEquals(1f, line.progress, 0.001f)
        assertEquals(200_000L, state.totalOver)
        assertFalse(state.unbudgetedCategories.contains(Category.FOOD))
        assertTrue(state.unbudgetedCategories.contains(Category.SHOPPING))

        job.cancel()
    }

    @Test
    fun `không lưu ngân sách có giới hạn bằng 0`() = runTest {
        val budgets = FakeBudgetRepository()
        val vm = BudgetViewModel(FakeAuthRepository(), budgets, FakeTransactionRepository())

        var result: Boolean? = null
        vm.saveBudget(Category.FOOD, 0L) { result = it }
        advanceUntilIdle()

        assertEquals(false, result)
        assertTrue(budgets.upserted.isEmpty())

        vm.saveBudget(Category.FOOD, 800_000L) { result = it }
        advanceUntilIdle()

        assertEquals(true, result)
        assertEquals(1, budgets.upserted.size)
        assertEquals(800_000L, budgets.upserted.first().limit)
    }
}
