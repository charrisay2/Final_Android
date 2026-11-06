package com.example.vinho.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinho.data.auth.AuthRepository
import com.example.vinho.data.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Cổng vào: quyết định người dùng thấy màn Đăng nhập hay màn chính.
 * Chỉ quan tâm "đã đăng nhập hay chưa", không giữ dữ liệu chi tiêu.
 */
@HiltViewModel
class AuthGateViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    sealed interface State {
        /** Chưa biết — đang chờ Firebase trả lời (thường < 300ms). */
        data object Loading : State
        data object SignedOut : State
        data class SignedIn(val user: UserProfile) : State
    }

    val state: StateFlow<State> = authRepository.currentUser
        .map { user -> if (user == null) State.SignedOut else State.SignedIn(user) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = State.Loading,
        )
}
