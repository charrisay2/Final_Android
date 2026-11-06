package com.example.vinho.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Green40,
    onPrimary = Color.White,
    primaryContainer = Green90,
    onPrimaryContainer = Green10,
    secondary = Teal40,
    onSecondary = Color.White,
    secondaryContainer = NeutralVariant90,
    onSecondaryContainer = Teal10,
    tertiary = Sand40,
    onTertiary = Color.White,
    tertiaryContainer = Sand90,
    onTertiaryContainer = Sand10,
    error = Error40,
    onError = Color.White,
    errorContainer = Error90,
    onErrorContainer = Error10,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    surfaceContainer = Neutral95,
)

private val DarkColors = darkColorScheme(
    primary = Green80,
    onPrimary = Green20,
    primaryContainer = Green30,
    onPrimaryContainer = Green90,
    secondary = Teal80,
    onSecondary = Teal10,
    secondaryContainer = Teal30,
    onSecondaryContainer = Color(0xFFCDE9F5),
    tertiary = Sand80,
    onTertiary = Sand20,
    tertiaryContainer = Sand30,
    onTertiaryContainer = Sand90,
    error = Error80,
    onError = Error10,
    errorContainer = Error40,
    onErrorContainer = Error90,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant30,
    surfaceContainer = Neutral20,
)

/**
 * Theme của app. Không dùng dynamic color: bảng màu thương hiệu là một phần nhận diện,
 * và biểu đồ dùng đúng bộ màu này ở cả hai chế độ sáng/tối.
 */
@Composable
fun ViNhoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ViNhoTypography,
        content = content,
    )
}
