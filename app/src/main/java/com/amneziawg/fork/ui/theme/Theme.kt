package com.amneziawg.fork.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.setStatusBarColor
import androidx.core.view.setNavigationBarColor

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CAF50),
    primaryContainer = Color(0xFF388E3C),
    secondary = Color(0xFF81C784),
    secondaryContainer = Color(0xFF2E7D32),
    tertiary = Color(0xFF64B5F6),
    tertiaryContainer = Color(0xFF1565C0),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C),
    background = Color(0xFF121212),
    error = Color(0xFFEF5350),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onSurface = Color.White,
    outline = Color(0xFF444444)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),
    primaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFF388E3C),
    secondaryContainer = Color(0xFFB9F6CA),
    tertiary = Color(0xFF1565C0),
    tertiaryContainer = Color(0xFFBBDEFB),
    surface = Color.White,
    surfaceVariant = Color(0xFFF5F5F5),
    background = Color(0xFFFAFAFA),
    error = Color(0xFFC62828),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onSurface = Color.Black,
    outline = Color(0xFFBDBDBD)
)

@Composable
fun AmneziaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = androidx.compose.ui.platform.LocalView.current
    val window = (view.context as? android.app.Activity)?.window

    SideEffect {
        window?.let {
            WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = !darkTheme
            it.statusBarColor = colorScheme.background.toArgb()
            it.navigationBarColor = colorScheme.background.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

object Typography {
    // Using Material3 default typography
}