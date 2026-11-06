package com.example.vinho.di

import android.content.Context
import androidx.credentials.CredentialManager
import com.example.vinho.data.auth.AuthRepository
import com.example.vinho.data.auth.FirebaseAuthRepository
import com.example.vinho.data.budget.BudgetRepository
import com.example.vinho.data.budget.FirestoreBudgetRepository
import com.example.vinho.data.settings.DataStoreSettingsRepository
import com.example.vinho.data.settings.SettingsRepository
import com.example.vinho.data.storage.FirebaseReceiptStorage
import com.example.vinho.data.storage.ReceiptStorage
import com.example.vinho.data.transaction.FirestoreTransactionRepository
import com.example.vinho.data.transaction.TransactionRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

/** Đánh dấu dispatcher dùng cho việc nặng (đọc/ghi Firestore, nén ảnh). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = Firebase.firestore

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = Firebase.storage

    @Provides
    @Singleton
    fun provideCredentialManager(@ApplicationContext context: Context): CredentialManager =
        CredentialManager.create(context)

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: FirestoreTransactionRepository): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: FirestoreBudgetRepository): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindReceiptStorage(impl: FirebaseReceiptStorage): ReceiptStorage

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository
}
