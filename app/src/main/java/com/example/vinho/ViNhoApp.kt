package com.example.vinho

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.vinho.notification.Notifications
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ViNhoApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /**
     * WorkManager lấy WorkerFactory từ Hilt để tạo @HiltWorker (ReminderWorker)
     * với dependency được inject. Cần kèm việc xoá WorkManagerInitializer trong Manifest.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Offline-first: bật cache cục bộ bền vững cho Firestore.
        // Đọc/ghi vẫn chạy khi mất mạng; khi có mạng lại Firestore tự đẩy thay đổi lên.
        // Phải đặt TRƯỚC lần dùng Firestore đầu tiên (Application.onCreate là sớm nhất).
        Firebase.firestore.firestoreSettings = firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings { })
        }

        Notifications.createChannel(this)
    }
}
