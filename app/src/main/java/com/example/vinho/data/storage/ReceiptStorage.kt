package com.example.vinho.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.example.vinho.di.IoDispatcher
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

/**
 * Nơi duy nhất nói chuyện với Firebase Storage.
 * Tách interface để ViewModel/test không cần Firebase.
 */
interface ReceiptStorage {
    /** Nén ảnh rồi upload, trả về URL tải xuống. [uri] là chuỗi URI do Photo Picker trả về. */
    suspend fun upload(uid: String, uri: String): String

    /** Xoá ảnh trên Storage theo URL (bỏ qua nếu không tìm thấy). */
    suspend fun delete(url: String)
}

class FirebaseReceiptStorage @Inject constructor(
    private val storage: FirebaseStorage,
    @param:ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ReceiptStorage {

    override suspend fun upload(uid: String, uri: String): String {
        // Nén trên IO vì decode bitmap rất nặng, không được chạy trên main thread
        val bytes = withContext(io) {
            ReceiptImageCompressor.compress(context, Uri.parse(uri))
        }

        val ref = storage.reference.child("users/$uid/receipts/${UUID.randomUUID()}.jpg")
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()
        ref.putBytes(bytes, metadata).await()
        return ref.downloadUrl.await().toString()
    }

    override suspend fun delete(url: String) {
        runCatching { storage.getReferenceFromUrl(url).delete().await() }
    }
}

/**
 * Nén ảnh hoá đơn trước khi upload: hoá đơn chỉ cần đọc được chữ, không cần ảnh 12MP.
 * Quy trình: đọc kích thước -> decode bản thu nhỏ (inSampleSize) -> xoay theo EXIF -> nén JPEG.
 */
object ReceiptImageCompressor {

    fun compress(
        context: Context,
        uri: Uri,
        maxSize: Int = 1600,
        quality: Int = 80,
    ): ByteArray {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val srcW = bounds.outWidth
        val srcH = bounds.outHeight
        require(srcW > 0 && srcH > 0) { "Không đọc được ảnh đã chọn" }

        var sample = 1
        while (max(srcW, srcH) / (sample * 2) >= maxSize) sample *= 2

        var bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Không decode được ảnh")

        val rotation = resolver.openInputStream(uri)?.use { input ->
            when (ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        val scale = min(1f, maxSize.toFloat() / max(bitmap.width, bitmap.height))
        val matrix = Matrix().apply {
            if (scale < 1f) postScale(scale, scale)
            if (rotation != 0f) postRotate(rotation)
        }
        if (!matrix.isIdentity) {
            val transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (transformed !== bitmap) bitmap.recycle()
            bitmap = transformed
        }

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        bitmap.recycle()
        return out.toByteArray()
    }
}
