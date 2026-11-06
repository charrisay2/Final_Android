package com.example.vinho

import android.content.Context
import com.example.vinho.data.auth.AuthRepository
import com.example.vinho.data.budget.BudgetRepository
import com.example.vinho.data.model.Budget
import com.example.vinho.data.model.Category
import com.example.vinho.data.model.Transaction
import com.example.vinho.data.model.TransactionDraft
import com.example.vinho.data.model.TransactionType
import com.example.vinho.data.model.UserProfile
import com.example.vinho.core.toLocalDate
import com.example.vinho.data.transaction.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.atomic.AtomicInteger

val testUser = UserProfile(
    uid = "uid-test",
    displayName = "Nguyễn Phú Quốc",
    email = "quoc@example.com",
    photoUrl = null,
)

/** Auth giả: chỉ cần phát đúng một user cố định. */
class FakeAuthRepository(
    initialUser: UserProfile? = testUser,
) : AuthRepository {

    private val state = MutableStateFlow(initialUser)

    override val currentUser: Flow<UserProfile?> = state

    override suspend fun signInWithGoogle(activityContext: Context): UserProfile =
        state.value ?: error("Không có user")

    override suspend fun signOut() {
        state.value = null
    }
}

/** Repository giao dịch giả: giữ dữ liệu trong bộ nhớ và ghi lại lời gọi để assert. */
class FakeTransactionRepository : TransactionRepository {

    val transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val added = mutableListOf<TransactionDraft>()
    val updated = mutableListOf<Pair<String, TransactionDraft>>()
    val deleted = mutableListOf<String>()

    /** Đặt giá trị để giả lập lỗi mạng. */
    var failWith: Throwable? = null

    override fun observeMonth(uid: String, month: YearMonth): Flow<List<Transaction>> =
        transactions.map { list ->
            list.filter { YearMonth.from(it.date.toLocalDate()) == month }
        }

    override suspend fun get(uid: String, id: String): Transaction? =
        transactions.value.firstOrNull { it.id == id }

    override suspend fun add(uid: String, draft: TransactionDraft): String {
        failWith?.let { throw it }
        added += draft
        return "new-id"
    }

    override suspend fun update(uid: String, id: String, draft: TransactionDraft) {
        failWith?.let { throw it }
        updated += id to draft
    }

    override suspend fun delete(uid: String, id: String) {
        failWith?.let { throw it }
        deleted += id
    }
}

class FakeBudgetRepository : BudgetRepository {

    val budgets = MutableStateFlow<List<Budget>>(emptyList())
    val upserted = mutableListOf<Budget>()
    val deleted = mutableListOf<Pair<YearMonth, Category>>()

    override fun observeMonth(uid: String, month: YearMonth): Flow<List<Budget>> =
        budgets.map { list -> list.filter { it.month == "${month.year}-${month.monthValue.toString().padStart(2, '0')}" } }

    override suspend fun upsert(uid: String, budget: Budget) {
        upserted += budget
    }

    override suspend fun delete(uid: String, month: YearMonth, category: Category) {
        deleted += month to category
    }
}

private val ids = AtomicInteger(0)

/** Tạo giao dịch nhanh trong test. Mặc định là hôm nay (thuộc tháng hiện tại). */
fun transaction(
    amount: Long,
    type: TransactionType,
    category: Category,
    note: String = "",
    date: Long = LocalDate.now().toEpochMillis(),
    receiptUrl: String? = null,
): Transaction = Transaction(
    id = "tx-${ids.incrementAndGet()}",
    amount = amount,
    type = type,
    category = category,
    note = note,
    date = date,
    receiptUrl = receiptUrl,
)

private fun LocalDate.toEpochMillis(): Long =
    atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
