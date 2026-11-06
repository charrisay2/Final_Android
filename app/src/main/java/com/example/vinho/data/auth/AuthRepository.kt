package com.example.vinho.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.vinho.R
import com.example.vinho.data.model.UserProfile
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** Nguồn duy nhất cho trạng thái đăng nhập. ViewModel không biết Firebase tồn tại. */
interface AuthRepository {

    /** Phát user hiện tại mỗi khi trạng thái đăng nhập đổi; null = chưa đăng nhập. */
    val currentUser: Flow<UserProfile?>

    /** Mở bottom sheet chọn tài khoản Google rồi đăng nhập Firebase. */
    suspend fun signInWithGoogle(activityContext: Context): UserProfile

    suspend fun signOut()
}

class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val credentialManager: CredentialManager,
    @param:ApplicationContext private val appContext: Context,
) : AuthRepository {

    override val currentUser: Flow<UserProfile?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            trySend(
                user?.let {
                    UserProfile(
                        uid = it.uid,
                        displayName = it.displayName ?: it.email?.substringBefore('@') ?: "Bạn",
                        email = it.email.orEmpty(),
                        photoUrl = it.photoUrl?.toString(),
                    )
                }
            )
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override suspend fun signInWithGoogle(activityContext: Context): UserProfile {
        // Web client ID do plugin google-services sinh ra từ google-services.json.
        // Thiếu chuỗi này = chưa bật Google Sign-In trong Firebase Console.
        val webClientId = appContext.getString(R.string.default_web_client_id)

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()

        val credential = try {
            credentialManager.getCredential(activityContext, request).credential
        } catch (e: GetCredentialCancellationException) {
            throw SignInCancelledException(e)
        }

        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw IllegalStateException("Loại credential không mong đợi: ${credential.type}")
        }

        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(firebaseCredential).await()
        val user = result.user ?: throw IllegalStateException("Firebase không trả về user")

        // Ghi hồ sơ tối thiểu để Firestore rules có dữ liệu người dùng.
        // merge() để không ghi đè các trường khác nếu document đã tồn tại.
        runCatching {
            firestore.collection(USERS).document(user.uid)
                .set(
                    mapOf(
                        "displayName" to (user.displayName ?: ""),
                        "email" to (user.email ?: ""),
                        "photoUrl" to user.photoUrl?.toString(),
                    ),
                    SetOptions.merge(),
                )
                .await()
        }.onFailure { Log.w(TAG, "Không ghi được hồ sơ người dùng", it) }

        return UserProfile(
            uid = user.uid,
            displayName = user.displayName ?: user.email?.substringBefore('@') ?: "Bạn",
            email = user.email.orEmpty(),
            photoUrl = user.photoUrl?.toString(),
        )
    }

    override suspend fun signOut() {
        auth.signOut()
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: ClearCredentialException) {
            // Không chặn đăng xuất chỉ vì Credential Manager lỗi
            Log.w(TAG, "clearCredentialState thất bại", e)
        }
    }

    private companion object {
        const val TAG = "AuthRepository"
        const val USERS = "users"
    }
}

/** Người dùng bấm "Huỷ" ở bottom sheet — không phải lỗi, UI hiển thị nhẹ nhàng. */
class SignInCancelledException(cause: Throwable) : Exception(cause)
