package com.example.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = M3VioletPrimary,
    onPrimary = M3VioletOnPrimary,
    primaryContainer = M3VioletContainer,
    onPrimaryContainer = M3VioletOnContainer,
    secondary = M3SecondaryLilac,
    onSecondary = Color.White,
    secondaryContainer = M3SecondaryContainer,
    onSecondaryContainer = M3OnSecondaryContainer,
    tertiary = M3TertiaryAmber,
    onTertiary = Color.White,
    tertiaryContainer = M3TertiaryContainer,
    onTertiaryContainer = M3OnTertiaryContainer,
    background = M3LightBackground,
    onBackground = M3LightTextPrimary,
    surface = M3LightSurface,
    onSurface = M3LightTextPrimary,
    surfaceVariant = M3LightSurfaceContainer,
    onSurfaceVariant = M3LightTextSecondary,
    surfaceContainerLowest = M3LightSurfaceContainerLowest,
    surfaceContainerLow = M3LightSurfaceContainerLow,
    surfaceContainer = M3LightSurfaceContainer,
    surfaceContainerHigh = M3LightSurfaceContainerHigh,
    surfaceContainerHighest = M3LightSurfaceContainerHighest,
    outline = M3LightOutline,
    outlineVariant = M3LightOutlineVariant,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColorScheme = darkColorScheme(
    primary = M3DarkPrimary,
    onPrimary = M3DarkOnPrimary,
    primaryContainer = M3DarkPrimaryContainer,
    onPrimaryContainer = M3DarkOnPrimaryContainer,
    secondary = Color(0xFFCBC2DB),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    background = M3DarkBackground,
    onBackground = M3DarkTextPrimary,
    surface = M3DarkSurface,
    onSurface = M3DarkTextPrimary,
    surfaceVariant = M3DarkSurfaceContainer,
    onSurfaceVariant = M3DarkTextSecondary,
    surfaceContainer = M3DarkSurfaceContainer,
    surfaceContainerHigh = M3DarkSurfaceContainerHigh,
    surfaceContainerHighest = M3DarkSurfaceContainerHighest,
    outline = M3DarkOutline,
    outlineVariant = M3DarkOutlineVariant,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun RepoLensTheme(
    darkTheme: Boolean = false, // Default to the primary light theme as explicitly requested
    dynamicColor: Boolean = false,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = RepoLensTheme(darkTheme, dynamicColor, content)
