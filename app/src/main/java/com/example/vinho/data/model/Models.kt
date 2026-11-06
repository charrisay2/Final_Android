package com.example.vinho.data.model

import androidx.annotation.StringRes
import com.example.vinho.R

/** Loại giao dịch. Lưu trên Firestore dưới dạng chuỗi "INCOME" / "EXPENSE". */
enum class TransactionType { INCOME, EXPENSE }

/**
 * Danh mục cố định của app.
 *
 * Vì sao dùng enum thay vì collection trên Firestore: danh mục là hằng số của sản phẩm,
 * nằm trong code thì không tốn 1 lượt đọc mạng, hiển thị được cả khi offline, và đổi nhãn
 * chỉ cần sửa strings.xml (đa ngôn ngữ).
 *
 * `labelRes` trỏ tới string resource để hỗ trợ vi + en; `type` quyết định danh mục này
 * thuộc form Thêm khoản thu hay khoản chi.
 */
enum class Category(
    @StringRes val labelRes: Int,
    val emoji: String,
    val type: TransactionType,
) {
    FOOD(R.string.cat_food, "\uD83C\uDF5C", TransactionType.EXPENSE),
    TRANSPORT(R.string.cat_transport, "\uD83D\uDE8C", TransactionType.EXPENSE),
    SHOPPING(R.string.cat_shopping, "\uD83D\uDECD", TransactionType.EXPENSE),
    BILLS(R.string.cat_bills, "\uD83E\uDDFE", TransactionType.EXPENSE),
    ENTERTAINMENT(R.string.cat_entertainment, "\uD83C\uDFAE", TransactionType.EXPENSE),
    HEALTH(R.string.cat_health, "\uD83D\uDC8A", TransactionType.EXPENSE),
    EDUCATION(R.string.cat_education, "\uD83D\uDCDA", TransactionType.EXPENSE),
    HOUSING(R.string.cat_housing, "\uD83C\uDFE0", TransactionType.EXPENSE),
    OTHER_EXPENSE(R.string.cat_other_expense, "\u2728", TransactionType.EXPENSE),
    SALARY(R.string.cat_salary, "\uD83D\uDCBC", TransactionType.INCOME),
    BONUS(R.string.cat_bonus, "\uD83C\uDF81", TransactionType.INCOME),
    OTHER_INCOME(R.string.cat_other_income, "\uD83D\uDCB0", TransactionType.INCOME);

    companion object {
        fun of(type: TransactionType): List<Category> = entries.filter { it.type == type }

        /** Đọc lại từ Firestore; dữ liệu lạ (bị xoá danh mục) thì quy về "khác". */
        fun fromName(name: String?): Category = when (name) {
            null -> OTHER_EXPENSE
            else -> entries.firstOrNull { it.name == name }
                ?: if (name == "INCOME") OTHER_INCOME else OTHER_EXPENSE
        }
    }
}

/** Một giao dịch đã lưu. Domain model thuần Kotlin — không có kiểu Firebase ở đây. */
data class Transaction(
    val id: String = "",
    val amount: Long = 0L,
    val type: TransactionType = TransactionType.EXPENSE,
    val category: Category = Category.OTHER_EXPENSE,
    val note: String = "",
    /** Thời điểm (ms) của ngày giao dịch, luôn là 00:00 giờ máy. */
    val date: Long = 0L,
    val receiptUrl: String? = null,
    val createdAt: Long = 0L,
) {
    /** Số tiền có dấu, dùng cho chỗ cần hiển thị +/- */
    val signedAmount: Long get() = if (type == TransactionType.INCOME) amount else -amount
}

/** Dữ liệu form gửi xuống repository. Tách khỏi [Transaction] vì chưa có id/createdAt. */
data class TransactionDraft(
    val amount: Long,
    val type: TransactionType,
    val category: Category,
    val note: String = "",
    val date: Long,
    /** Ảnh mới người dùng vừa chọn, dạng chuỗi URI (chưa upload). */
    val receiptUri: String? = null,
    /** Ảnh đã có trên Storage (khi sửa giao dịch cũ). */
    val receiptUrl: String? = null,
)

/** Ngân sách của một danh mục trong một tháng. */
data class Budget(
    val category: Category,
    val limit: Long,
    /** "2026-11" */
    val month: String,
)

/** Người dùng đang đăng nhập (rút gọn từ FirebaseUser). */
data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?,
) {
    val initial: String get() = displayName.trim().take(1).uppercase().ifBlank { "?" }
}
