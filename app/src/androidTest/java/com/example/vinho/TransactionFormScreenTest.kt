package com.example.vinho

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vinho.data.auth.AuthRepository
import com.example.vinho.data.model.Transaction
import com.example.vinho.data.model.TransactionDraft
import com.example.vinho.data.model.UserProfile
import com.example.vinho.data.transaction.TransactionRepository
import com.example.vinho.ui.form.TransactionFormScreen
import com.example.vinho.ui.form.TransactionFormViewModel
import com.example.vinho.ui.theme.ViNhoTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.YearMonth

/**
 * Compose UI test cho form Thêm giao dịch.
 *
 * Dùng ViewModel thật + repository giả nên test đi qua đúng luồng UI → ViewModel → repository
 * mà không cần Firebase. Chữ trên màn hình lấy từ resources nên test chạy được ở cả vi và en.
 */
@RunWith(AndroidJUnit4::class)
class TransactionFormScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val transactions = UiTransactionRepository()

    private fun setContent() {
        val viewModel = TransactionFormViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = UiAuthRepository(),
            transactionRepository = transactions,
        )
        composeRule.setContent {
            ViNhoTheme {
                TransactionFormScreen(onDone = {}, viewModel = viewModel)
            }
        }
    }

    @Test
    fun savingEmptyForm_showsAmountError() {
        setContent()
        val errorText = composeRule.activity.getString(R.string.error_amount_required)

        composeRule.onNodeWithTag("form_save").performClick()

        composeRule.onNodeWithText(errorText).assertIsDisplayed()
        assertTrue(transactions.added.isEmpty())
    }

    @Test
    fun savingValidForm_sendsDraftToRepository() {
        setContent()

        composeRule.onNodeWithTag("form_amount").performTextInput("150000")
        composeRule.onNodeWithTag("category_FOOD").performClick()
        composeRule.onNodeWithTag("form_save").performClick()

        composeRule.waitUntil(timeoutMillis = 3_000) { transactions.added.isNotEmpty() }
        assertEquals(150_000L, transactions.added.first().amount)
    }
}

private class UiAuthRepository : AuthRepository {
    private val user = UserProfile("uid-ui", "Người dùng test", "test@example.com", null)
    override val currentUser: Flow<UserProfile?> = flowOf(user)
    override suspend fun signInWithGoogle(activityContext: Context): UserProfile = user
    override suspend fun signOut() = Unit
}

private class UiTransactionRepository : TransactionRepository {
    val added = mutableListOf<TransactionDraft>()

    override fun observeMonth(uid: String, month: YearMonth): Flow<List<Transaction>> = flowOf(emptyList())

    override suspend fun get(uid: String, id: String): Transaction? = null

    override suspend fun add(uid: String, draft: TransactionDraft): String {
        added += draft
        return "ui-id"
    }

    override suspend fun update(uid: String, id: String, draft: TransactionDraft) = Unit

    override suspend fun delete(uid: String, id: String) = Unit
}
