package com.example.vinho.data.transaction

import com.example.vinho.core.monthKey
import com.example.vinho.core.toLocalDate
import com.example.vinho.data.model.Category
import com.example.vinho.data.model.Transaction
import com.example.vinho.data.model.TransactionDraft
import com.example.vinho.data.model.TransactionType
import com.example.vinho.data.storage.ReceiptStorage
import com.example.vinho.di.IoDispatcher
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.YearMonth
import java.util.Date
import javax.inject.Inject

interface TransactionRepository {
    /** Mọi giao dịch trong tháng, mới nhất trước. Flow phát lại mỗi khi Firestore thay đổi. */
    fun observeMonth(uid: String, month: YearMonth): Flow<List<Transaction>>

    suspend fun get(uid: String, id: String): Transaction?

    /** Trả về id của document vừa tạo. */
    suspend fun add(uid: String, draft: TransactionDraft): String

    suspend fun update(uid: String, id: String, draft: TransactionDraft)

    suspend fun delete(uid: String, id: String)
}

/** Mới nhất trước; cùng ngày thì ưu tiên bản ghi mới tạo. */
val TransactionNewestFirst: Comparator<Transaction> =
    compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAt }

/**
 * Cài đặt bằng Cloud Firestore.
 *
 * Vì sao query chỉ `whereEqualTo("month", ...)` rồi sort trong bộ nhớ: query vừa where vừa
 * orderBy trên hai field khác nhau sẽ cần composite index (phải tạo tay trên Console).
 * Mỗi tháng chỉ vài chục giao dịch nên sort tại máy là đủ nhanh và không phụ thuộc index.
 */
class FirestoreTransactionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val receipts: ReceiptStorage,
    @IoDispatcher private val io: CoroutineDispatcher,
) : TransactionRepository {

    private fun collection(uid: String) =
        firestore.collection(USERS).document(uid).collection(TRANSACTIONS)

    override fun observeMonth(uid: String, month: YearMonth): Flow<List<Transaction>> =
        collection(uid)
            .whereEqualTo("month", month.monthKey())
            .snapshots()
            .map { snapshot ->
                snapshot.documents
                    .mapNotNull { it.toTransaction() }
                    .sortedWith(TransactionNewestFirst)
            }
            .flowOn(io)

    override suspend fun get(uid: String, id: String): Transaction? =
        collection(uid).document(id).get().await().toTransaction()

    override suspend fun add(uid: String, draft: TransactionDraft): String {
        val receiptUrl = draft.receiptUri?.let { receipts.upload(uid, it) }
        val document = collection(uid).document()
        document.set(
            draft.toMap(receiptUrl) + mapOf("createdAt" to FieldValue.serverTimestamp()),
        ).await()
        return document.id
    }

    override suspend fun update(uid: String, id: String, draft: TransactionDraft) {
        val receiptUrl = if (draft.receiptUri != null) {
            receipts.upload(uid, draft.receiptUri)
        } else {
            draft.receiptUrl
        }
        collection(uid).document(id)
            .set(
                draft.toMap(receiptUrl) + mapOf("updatedAt" to FieldValue.serverTimestamp()),
                SetOptions.merge(),
            )
            .await()
    }

    override suspend fun delete(uid: String, id: String) {
        val document = collection(uid).document(id)
        val receiptUrl = document.get().await().getString("receiptUrl")
        document.delete().await()
        // Ảnh hoá đơn không còn ai dùng -> xoá luôn cho sạch Storage
        receiptUrl?.let { receipts.delete(it) }
    }

    /**
     * Trường `month` được ghi kèm mỗi giao dịch để query theo tháng chỉ cần 1 điều kiện.
     * Client không tự đặt được field này khi sửa vì toMap() luôn tính lại từ `date`.
     */
    private fun TransactionDraft.toMap(receiptUrl: String?): Map<String, Any?> = mapOf(
        "amount" to amount,
        "type" to type.name,
        "category" to category.name,
        "note" to note,
        "date" to Timestamp(Date(date)),
        "month" to YearMonth.from(date.toLocalDate()).monthKey(),
        "receiptUrl" to receiptUrl,
    )

    private fun DocumentSnapshot.toTransaction(): Transaction? {
        if (!exists()) return null
        val type = TransactionType.entries.firstOrNull { it.name == getString("type") }
            ?: TransactionType.EXPENSE
        return Transaction(
            id = id,
            amount = getLong("amount") ?: 0L,
            type = type,
            category = Category.fromName(getString("category")),
            note = getString("note").orEmpty(),
            date = getTimestamp("date")?.toDate()?.time ?: 0L,
            receiptUrl = getString("receiptUrl"),
            createdAt = getTimestamp("createdAt")?.toDate()?.time ?: 0L,
        )
    }

    private companion object {
        const val USERS = "users"
        const val TRANSACTIONS = "transactions"
    }
}
