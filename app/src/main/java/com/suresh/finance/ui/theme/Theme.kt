package com.suresh.finance.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B6E4F),
    secondary = Color(0xFF4A6358),
    tertiary = Color(0xFF3D6373),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD8B1),
    secondary = Color(0xFFB1CCBF),
    tertiary = Color(0xFFA5CCDF),
)

@Composable
fun FinanceTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    // Samsung One UI supports Material You, so follow the phone's wallpaper colours.
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}

/** Fixed colours for money in and out, so they read the same with any wallpaper. */
object MoneyColors {
    @Composable
    fun income(): Color = if (isSystemInDarkTheme()) Color(0xFF81C995) else Color(0xFF1E7B34)

    @Composable
    fun expense(): Color = if (isSystemInDarkTheme()) Color(0xFFF28B82) else Color(0xFFC5221F)

    @Composable
    fun warning(): Color = if (isSystemInDarkTheme()) Color(0xFFFDD663) else Color(0xFFB06000)
}
