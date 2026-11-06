package com.example.vinho.ui.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.vinho.core.parseAmount
import com.example.vinho.core.toLocalDate
import com.example.vinho.core.toUserMessage
import com.example.vinho.core.formatNumber
import com.example.vinho.data.auth.AuthRepository
import com.example.vinho.data.model.Category
import com.example.vinho.data.model.TransactionDraft
import com.example.vinho.data.model.TransactionType
import com.example.vinho.data.transaction.TransactionRepository
import com.example.vinho.ui.navigation.TransactionFormRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TransactionFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingReceipt: Boolean = false,
    val type: TransactionType = TransactionType.EXPENSE,
    /** Chuỗi đã định dạng ("1.250.000") để hiển thị; parse khi lưu. */
    val amountText: String = "",
    val category: Category? = null,
    val date: Long = LocalDate.now().toEpochMillis(),
    val note: String = "",
    val receiptUri: String? = null,
    val receiptUrl: String? = null,
    val amountError: Boolean = false,
    val categoryError: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
) {
    val amount: Long? get() = parseAmount(amountText)
    val canSave: Boolean get() = !isSaving && !isLoading
}

/**
 * ViewModel dùng chung cho Thêm và Sửa.
 *
 * id lấy từ route type-safe: null = thêm mới, có id = tải giao dịch cũ rồi cập nhật.
 * Validate ở đây (tầng app) trước khi gọi repository; Firestore rules vẫn kiểm tra lại
 * vì không ai chặn được client tự gọi API.
 */
@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val transactionId: String? = runCatching {
        savedStateHandle.toRoute<TransactionFormRoute>().transactionId
    }.getOrNull()

    private val _uiState = MutableStateFlow(TransactionFormUiState(isEdit = transactionId != null))
    val uiState: StateFlow<TransactionFormUiState> = _uiState.asStateFlow()

    init {
        if (transactionId != null) loadTransaction(transactionId)
    }

    private fun loadTransaction(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val uid = currentUid()
                val transaction = transactionRepository.get(uid, id)
                    ?: error("Không tìm thấy giao dịch")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        type = transaction.type,
                        amountText = formatNumber(transaction.amount),
                        category = transaction.category,
                        date = transaction.date,
                        note = transaction.note,
                        receiptUrl = transaction.receiptUrl,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.toUserMessage()) }
            }
        }
    }

    /** Người dùng gõ số: bỏ mọi ký tự không phải số rồi định dạng lại. */
    fun onAmountChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(12)
        val formatted = digits.toLongOrNull()?.let { formatNumber(it) } ?: ""
        _uiState.update { it.copy(amountText = formatted, amountError = false) }
    }

    fun onTypeChange(type: TransactionType) {
        _uiState.update { state ->
            val categoryStillValid = state.category?.type == type
            state.copy(
                type = type,
                category = if (categoryStillValid) state.category else null,
                categoryError = false,
            )
        }
    }

    fun onCategoryChange(category: Category) {
        _uiState.update { it.copy(category = category, categoryError = false) }
    }

    fun onDateChange(date: Long) {
        _uiState.update { it.copy(date = date.toLocalDate().toEpochMillis()) }
    }

    fun onNoteChange(value: String) {
        _uiState.update { it.copy(note = value) }
    }

    fun onReceiptPicked(uri: String?) {
        _uiState.update { it.copy(receiptUri = uri) }
    }

    fun onReceiptRemoved() {
        _uiState.update { it.copy(receiptUri = null, receiptUrl = null) }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return

        val amount = state.amount
        val amountInvalid = amount == null || amount <= 0L
        val categoryInvalid = state.category == null
        if (amountInvalid || categoryInvalid) {
            _uiState.update { it.copy(amountError = amountInvalid, categoryError = categoryInvalid) }
            return
        }

        val draft = TransactionDraft(
            amount = amount!!,
            type = state.type,
            category = state.category!!,
            note = state.note.trim(),
            date = state.date,
            receiptUri = state.receiptUri,
            receiptUrl = state.receiptUrl,
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSaving = true, error = null, isUploadingReceipt = it.receiptUri != null)
            }
            try {
                val uid = currentUid()
                if (transactionId == null) {
                    transactionRepository.add(uid, draft)
                } else {
                    transactionRepository.update(uid, transactionId, draft)
                }
                _uiState.update { it.copy(isSaving = false, isUploadingReceipt = false, saved = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, isUploadingReceipt = false, error = e.toUserMessage())
                }
            }
        }
    }

    fun delete() {
        val id = transactionId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                transactionRepository.delete(currentUid(), id)
                _uiState.update { it.copy(isSaving = false, deleted = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.toUserMessage()) }
            }
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }

    private suspend fun currentUid(): String =
        authRepository.currentUser.first()?.uid ?: error("Chưa đăng nhập")
}
