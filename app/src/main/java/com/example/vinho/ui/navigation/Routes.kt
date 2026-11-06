package com.example.vinho.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Route type-safe của Navigation Compose: mỗi đích là một @Serializable.
 * Truyền tham số bằng property của data class thay vì nối chuỗi "screen/{id}".
 */

/** Hiển thị logo trong lúc chờ Firebase trả về trạng thái đăng nhập. */
@Serializable
object SplashRoute

@Serializable
object LoginRoute

/** Vỏ chứa 5 tab (Tổng quan / Giao dịch / Ngân sách / Thống kê / Cài đặt). */
@Serializable
object MainRoute

/** null = thêm mới, có id = sửa giao dịch đã có. */
@Serializable
data class TransactionFormRoute(val transactionId: String? = null)

// --- Các tab nằm trong NavHost con của MainRoute ---

@Serializable
object OverviewRoute

@Serializable
object TransactionsRoute

@Serializable
object BudgetRoute

@Serializable
object StatsRoute

@Serializable
object SettingsRoute
