package com.example.vinho.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinho.core.toUserMessage
import com.example.vinho.data.auth.AuthRepository
import com.example.vinho.data.model.UserProfile
import com.example.vinho.data.settings.ReminderSettings
import com.example.vinho.data.settings.SettingsRepository
import com.example.vinho.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val user: UserProfile? = null,
    val reminder: ReminderSettings = ReminderSettings(),
    val isSigningOut: Boolean = false,
    val error: String? = null,
)

/**
 * ViewModel màn Cài đặt: hồ sơ người dùng + nhắc nhở hằng ngày + đăng xuất.
 *
 * Khi bật/tắt hoặc đổi giờ nhắc, ViewModel vừa lưu vào DataStore vừa gọi ReminderScheduler:
 * DataStore là "sự thật" của cài đặt, WorkManager chỉ là bộ hẹn giờ thực thi nó.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> =
        combine(authRepository.currentUser, settingsRepository.reminder) { user, reminder ->
            SettingsUiState(user = user, reminder = reminder)
        }
            .catch { error -> emit(SettingsUiState(error = error.toUserMessage())) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SettingsUiState(),
            )

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setReminderEnabled(enabled)
                val current = uiState.value.reminder
                if (enabled) reminderScheduler.schedule(current.hour, current.minute)
                else reminderScheduler.cancel()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Lỗi ghi DataStore rất hiếm; không làm sập UI
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setReminderTime(hour, minute)
            if (uiState.value.reminder.enabled) {
                reminderScheduler.schedule(hour, minute)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Đăng xuất cục bộ vẫn thành công dù clearCredentialState lỗi
            }
        }
    }
}
