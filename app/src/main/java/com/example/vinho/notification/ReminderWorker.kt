package com.example.vinho.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.vinho.data.settings.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Worker chạy mỗi ngày một lần để nhắc ghi chi tiêu.
 *
 * Vì sao đọc lại settings trong worker thay vì tin vào lúc lên lịch: người dùng có thể
 * tắt nhắc nhở sau khi lịch đã được tạo; kiểm tra lại ở đây tránh thông báo "ma".
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settings: SettingsRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        val reminder = settings.reminder.first()
        if (!reminder.enabled) return Result.success()
        Notifications.showDailyReminder(applicationContext)
        Result.success()
    }.getOrElse {
        // Lỗi tạm thời (ví dụ máy đang bận) -> để WorkManager thử lại sau
        Result.retry()
    }
}
