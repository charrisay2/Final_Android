package com.example.vinho.ui

import androidx.lifecycle.SavedStateHandle
import com.example.vinho.FakeAuthRepository
import com.example.vinho.FakeTransactionRepository
import com.example.vinho.data.model.Category
import com.example.vinho.data.model.TransactionType
import com.example.vinho.ui.form.TransactionFormViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionFormViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: FakeTransactionRepository = FakeTransactionRepository(),
    ) = TransactionFormViewModel(
        savedStateHandle = SavedStateHandle(),
        authRepository = FakeAuthRepository(),
        transactionRepository = repository,
    )

    @Test
    fun `lưu khi chưa nhập số tiền thì báo lỗi và không gọi repository`() = runTest {
        val repository = FakeTransactionRepository()
        val vm = viewModel(repository)
        vm.onCategoryChange(Category.FOOD)

        vm.save()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.amountError)
        assertFalse(vm.uiState.value.categoryError)
        assertTrue(repository.added.isEmpty())
        assertFalse(vm.uiState.value.saved)
    }

    @Test
    fun `lưu khi chưa chọn danh mục thì báo lỗi danh mục`() = runTest {
        val repository = FakeTransactionRepository()
        val vm = viewModel(repository)

        vm.onAmountChange("150000")
        vm.save()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.amountError)
        assertTrue(vm.uiState.value.categoryError)
        assertTrue(repository.added.isEmpty())
    }

    @Test
    fun `lưu hợp lệ thì gửi đúng dữ liệu xuống repository`() = runTest {
        val repository = FakeTransactionRepository()
        val vm = viewModel(repository)

        vm.onAmountChange("150000")
        vm.onCategoryChange(Category.FOOD)
        vm.onNoteChange("  Ăn trưa  ")
        vm.save()
        advanceUntilIdle()

        assertEquals(1, repository.added.size)
        val draft = repository.added.first()
        assertEquals(150_000L, draft.amount)
        assertEquals(TransactionType.EXPENSE, draft.type)
        assertEquals(Category.FOOD, draft.category)
        assertEquals("Ăn trưa", draft.note)
        assertTrue(vm.uiState.value.saved)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `đổi loại giao dịch thì bỏ danh mục không còn phù hợp`() = runTest {
        val vm = viewModel()

        vm.onCategoryChange(Category.FOOD)
        assertEquals(Category.FOOD, vm.uiState.value.category)

        vm.onTypeChange(TransactionType.INCOME)
        assertNull(vm.uiState.value.category)
        assertEquals(TransactionType.INCOME, vm.uiState.value.type)

        vm.onCategoryChange(Category.SALARY)
        vm.onTypeChange(TransactionType.EXPENSE)
        assertNull(vm.uiState.value.category)
    }

    @Test
    fun `gõ số tiền được định dạng hàng nghìn và bỏ ký tự lạ`() = runTest {
        val vm = viewModel()

        vm.onAmountChange("1a2b3 4567")
        assertEquals(1_234_567L, vm.uiState.value.amount)
        // Chuỗi hiển thị chỉ còn chữ số và dấu phân cách nghìn
        assertTrue(vm.uiState.value.amountText.filter { it.isDigit() } == "1234567")
    }
}
