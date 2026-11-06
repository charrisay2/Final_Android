package com.example.vinho.core

import com.example.vinho.data.auth.SignInCancelledException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException

/**
 * Đổi exception thành câu hiển thị được cho người dùng.
 *
 * Ghi chú i18n: đây là thông báo kỹ thuật dự phòng (fallback) khi server trả lỗi lạ;
 * các câu UI chính đều lấy từ strings.xml. Muốn dịch cả phần này thì map sang mã lỗi
 * rồi resolve ở tầng UI.
 */
fun Throwable.toUserMessage(): String = when (this) {
    is SignInCancelledException -> "Đã huỷ đăng nhập"
    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Không có quyền truy cập dữ liệu (kiểm tra Firestore rules)"
        FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Không kết nối được Firestore, thử lại sau"
        else -> message ?: "Lỗi Firestore"
    }
    is IOException -> "Không có kết nối mạng"
    else -> message ?: javaClass.simpleName
}
