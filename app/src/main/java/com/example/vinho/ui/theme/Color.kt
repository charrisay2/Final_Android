package com.example.vinho.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.vinho.data.model.Category

// Bảng màu thương hiệu "Ví Nhỏ": xanh ví (primary) + vàng đồng xu (tertiary)
val Green10 = Color(0xFF00201A)
val Green20 = Color(0xFF00382C)
val Green30 = Color(0xFF005142)
val Green40 = Color(0xFF1E6F5C)
val Green80 = Color(0xFF8CD5BE)
val Green90 = Color(0xFFA8F2DC)

val Sand10 = Color(0xFF2A1F00)
val Sand20 = Color(0xFF453100)
val Sand30 = Color(0xFF624700)
val Sand40 = Color(0xFF7F5F00)
val Sand80 = Color(0xFFF3BF48)
val Sand90 = Color(0xFFFFDF9C)

val Teal10 = Color(0xFF001F26)
val Teal30 = Color(0xFF1F4B57)
val Teal40 = Color(0xFF3D6373)
val Teal80 = Color(0xFFA4CDDD)

val Neutral10 = Color(0xFF191C1A)
val Neutral20 = Color(0xFF2E312F)
val Neutral90 = Color(0xFFE1E3E0)
val Neutral95 = Color(0xFFEFF1EE)
val Neutral99 = Color(0xFFFBFDF9)
val NeutralVariant30 = Color(0xFF3F4946)
val NeutralVariant50 = Color(0xFF6F7975)
val NeutralVariant80 = Color(0xFFBEC9C4)
val NeutralVariant90 = Color(0xFFDAE5E0)

val Error10 = Color(0xFF410002)
val Error40 = Color(0xFFBA1A1A)
val Error80 = Color(0xFFFFB4AB)
val Error90 = Color(0xFFFFDAD6)

/** Màu riêng cho từng danh mục — dùng cho biểu đồ và badge. */
object CategoryColors {
    private val map: Map<Category, Color> = mapOf(
        Category.FOOD to Color(0xFFE8833A),
        Category.TRANSPORT to Color(0xFF4E8CD9),
        Category.SHOPPING to Color(0xFFC86BB0),
        Category.BILLS to Color(0xFF6E7F8D),
        Category.ENTERTAINMENT to Color(0xFF8E6BD6),
        Category.HEALTH to Color(0xFFE2605F),
        Category.EDUCATION to Color(0xFF2E9E8F),
        Category.HOUSING to Color(0xFFB08A3E),
        Category.OTHER_EXPENSE to Color(0xFF7A8B99),
        Category.SALARY to Color(0xFF2F9E63),
        Category.BONUS to Color(0xFF3FA97F),
        Category.OTHER_INCOME to Color(0xFF5AA88C),
    )

    fun of(category: Category): Color = map[category] ?: Color(0xFF7A8B99)
}
