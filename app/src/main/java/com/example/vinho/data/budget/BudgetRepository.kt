package com.example.vinho.data.budget

import com.example.vinho.core.monthKey
import com.example.vinho.data.model.Budget
import com.example.vinho.data.model.Category
import com.example.vinho.di.IoDispatcher
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.YearMonth
import javax.inject.Inject

interface BudgetRepository {
    fun observeMonth(uid: String, month: YearMonth): Flow<List<Budget>>

    /** Thêm hoặc cập nhật giới hạn của một danh mục trong tháng. */
    suspend fun upsert(uid: String, budget: Budget)

    suspend fun delete(uid: String, month: YearMonth, category: Category)
}

/**
 * Mỗi ngân sách là 1 document có id = "<tháng>_<danh mục>", ví dụ "2026-11_FOOD".
 * Nhờ vậy "thêm" và "sửa" dùng chung một lệnh set(): ghi đè đúng document, không tạo trùng.
 */
class FirestoreBudgetRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val io: CoroutineDispatcher,
) : BudgetRepository {

    private fun collection(uid: String) =
        firestore.collection(USERS).document(uid).collection(BUDGETS)

    private fun documentId(month: YearMonth, category: Category) = "${month.monthKey()}_${category.name}"

    override fun observeMonth(uid: String, month: YearMonth): Flow<List<Budget>> =
        collection(uid)
            .whereEqualTo("month", month.monthKey())
            .snapshots()
            .map { snapshot -> snapshot.documents.mapNotNull { it.toBudget() } }
            .flowOn(io)

    override suspend fun upsert(uid: String, budget: Budget) {
        val month = YearMonth.parse(budget.month)
        collection(uid).document(documentId(month, budget.category)).set(
            mapOf(
                "category" to budget.category.name,
                "limit" to budget.limit,
                "month" to budget.month,
            ),
        ).await()
    }

    override suspend fun delete(uid: String, month: YearMonth, category: Category) {
        collection(uid).document(documentId(month, category)).delete().await()
    }

    private fun DocumentSnapshot.toBudget(): Budget? {
        if (!exists()) return null
        val category = Category.entries.firstOrNull { it.name == getString("category") } ?: return null
        return Budget(
            category = category,
            limit = getLong("limit") ?: 0L,
            month = getString("month").orEmpty(),
        )
    }

    private companion object {
        const val USERS = "users"
        const val BUDGETS = "budgets"
    }
}
