package com.example.vinho.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lên lịch nhắc nhở bằng WorkManager.
 *
 * WorkManager không hẹn giờ chính xác từng phút (đó là việc của AlarmManager): nó đảm bảo
 * "khoảng mỗi 24h một lần, chạy được kể cả khi app bị kill hoặc máy reboot". Vì vậy thời điểm
 * thực tế có thể lệch vài phút — hoàn toàn ổn với một lời nhắc.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /** Đặt lại lịch theo giờ người dùng chọn (ghi đè lịch cũ nếu có). */
    fun schedule(hour: Int, minute: Int) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis(hour, minute), TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build(),
            )
            .addTag(WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            // UPDATE: đổi giờ thì thay lịch cũ, không tạo thêm việc trùng
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        const val WORK_NAME = "daily_spending_reminder"

        /** Số ms từ bây giờ tới lần [hour]:[minute] kế tiếp. */
        fun initialDelayMillis(
            hour: Int,
            minute: Int,
            now: LocalDateTime = LocalDateTime.now(),
        ): Long {
            val target = LocalDateTime.of(now.toLocalDate(), LocalTime.of(hour, minute))
            val next = if (target.isAfter(now)) target else target.plusDays(1)
            return Duration.between(now, next).toMillis()
        }
    }
}
