package com.example.parent_teacher_engagement.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.parent_teacher_engagement.utils.ThemeManager
import com.example.parent_teacher_engagement.utils.SettingsManager
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun ParentteacherengagementTheme(
    darkTheme: Boolean = ThemeManager.isDarkMode,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // Create a custom Typography that uses the selected font and text size
    val customTypography = Typography.copy(
        displayLarge = Typography.displayLarge.copy(
            fontFamily = SettingsManager.getFontFamily(),
            fontSize = SettingsManager.getScaledFontSize(57).sp
        ),
        displayMedium = Typography.displayMedium.copy(
            fontFamily = SettingsManager.getFontFamily(),
            fontSize = SettingsManager.getScaledFontSize(45).sp
        ),
        displaySmall = Typography.displaySmall.copy(
            fontFamily = SettingsManager.getFontFamily(),
            fontSize = SettingsManager.getScaledFontSize(36).sp
        ),
        headlineLarge = Typography.headlineLarge.copy(
            fontFamily = SettingsManager.getFontFamily(),
            fontSize = SettingsManager.getScaledFontSize(32).sp
        ),
        headlineMedium = Typography.headlineMedium.copy(
            fontFamily = SettingsManager.getFontFamily(),
            fontSize = SettingsManager.getScaledFontSize(28).sp
        ),
        headlineSmall = Typography.headlineSmall.copy(
            fontFamily = SettingsManager.getFontFamily(),
            fontSize = SettingsManager.getScaledFontSize(24).sp
        ),
        titleLarge = Typography.titleLarge.copy(
            fontFamily = SettingsManager.getFontFamily(),
            fontSize = SettingsManager.getScaledFontSize(22).sp
        ),
        titleMedium = Typography.titleMedium.copy(
            fontFamily = SettingsManager.getFontFamily(),
            fontSize = SettingsManager.getScaledFontSize(16).sp
        ),
        titleSmall = Typography.titleSmall.copy(
            fontFamily = SettingsManager.getFontFamily(),
            fontSize = SettingsManager.getScaledFontSize(14).sp
        ),
        bodyLarge = Typography.bodyLarge.copy(
            fontFamily = SettingsManager.getFontFamily(),
            fontSize = SettingsManager.getScaledFontSize(16).sp
        ),
        bodyMedium = Typography.bodyMedium.copy(
            fontFamily = SettingsManager.getFontFamily(),
            fontSize = SettingsManager.getScaledFontSize(14).sp
        ),
        bodySmall = Typography.bodySmall.copy(
            fontFamily = SettingsManager.getFontFamily(),
            fontSize = SettingsManager.getScaledFontSize(12).sp
        ),
        labelLarge = Typography.labelLarge.copy(
            fontFamily = SettingsManager.getFontFamily(),
            fontSize = SettingsManager.getScaledFontSize(14).sp
        ),
        labelMedium = Typography.labelMedium.copy(
            fontFamily = SettingsManager.getFontFamily(),
            fontSize = SettingsManager.getScaledFontSize(12).sp
        ),
        labelSmall = Typography.labelSmall.copy(
            fontFamily = SettingsManager.getFontFamily(),
            fontSize = SettingsManager.getScaledFontSize(11).sp
        )
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = customTypography,
        content = content
    )
}