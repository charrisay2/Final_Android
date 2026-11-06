package com.example.vinho.ui

import com.example.vinho.FakeAuthRepository
import com.example.vinho.FakeTransactionRepository
import com.example.vinho.data.model.Category
import com.example.vinho.data.model.TransactionType
import com.example.vinho.transaction
import com.example.vinho.ui.transactions.TransactionsViewModel
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun repository() = FakeTransactionRepository().apply {
        transactions.value = listOf(
            transaction(50_000, TransactionType.EXPENSE, Category.FOOD, note = "Ăn trưa"),
            transaction(20_000, TransactionType.EXPENSE, Category.TRANSPORT, note = "Grab"),
            transaction(1_000_000, TransactionType.INCOME, Category.SALARY, note = "Lương tháng 11"),
        )
    }

    @Test
    fun `lọc theo loại và từ khoá ghi chú`() = runTest {
        val vm = TransactionsViewModel(FakeAuthRepository(), repository())
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(3, vm.uiState.value.totalCount)
        assertEquals(70_000L, vm.uiState.value.filteredExpense)
        assertEquals(1_000_000L, vm.uiState.value.filteredIncome)

        vm.onTypeFilterChange(TransactionType.EXPENSE)
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.totalCount)
        assertEquals(TransactionType.EXPENSE, vm.uiState.value.typeFilter)

        vm.onQueryChange("grab")
        advanceUntilIdle()
        val filtered = vm.uiState.value
        assertEquals(1, filtered.totalCount)
        assertEquals("Grab", filtered.groups.first().transactions.first().note)

        // Bấm lại đúng chip đang chọn -> bỏ lọc loại
        vm.onTypeFilterChange(TransactionType.EXPENSE)
        advanceUntilIdle()
        assertEquals(null, vm.uiState.value.typeFilter)

        job.cancel()
    }

    @Test
    fun `gom nhóm theo ngày, ngày mới nhất lên đầu`() = runTest {
        val today = LocalDate.now()
        val repository = FakeTransactionRepository().apply {
            transactions.value = listOf(
                transaction(
                    100_000,
                    TransactionType.EXPENSE,
                    Category.FOOD,
                    note = "Hôm nay",
                    date = today.toEpochMillis(),
                ),
                transaction(
                    30_000,
                    TransactionType.EXPENSE,
                    Category.FOOD,
                    note = "Hôm qua",
                    date = today.minusDays(1).toEpochMillis(),
                ),
            )
        }

        val vm = TransactionsViewModel(FakeAuthRepository(), repository)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        val groups = vm.uiState.value.groups
        assertEquals(2, groups.size)
        assertEquals(today, groups[0].date)
        assertEquals(today.minusDays(1), groups[1].date)
        assertEquals(100_000L, groups[0].expense)
        assertTrue(groups[0].income == 0L)

        job.cancel()
    }

    private fun LocalDate.toEpochMillis(): Long =
        atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
}
