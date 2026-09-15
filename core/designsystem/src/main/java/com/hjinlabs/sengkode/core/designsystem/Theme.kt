package com.hjinlabs.sengkode.core.designsystem

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

/**
 * SENGKODE brand palette. Teal primary (ocean/harbor), a warm red accent
 * (a nod to the Singapore flag), neutral surfaces. Dark and light schemes
 * are both first-class; dynamic color is opt-in via [dynamicColor] on
 * Android 12+.
 */
private val SkLightColors = lightColorScheme(
    primary = Color(0xFF00695C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9CF1E1),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF4A635D),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFB3252B),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFBFDFA),
    onBackground = Color(0xFF171D1B),
    surface = Color(0xFFFBFDFA),
    onSurface = Color(0xFF171D1B),
    surfaceVariant = Color(0xFFDBE5E0),
    onSurfaceVariant = Color(0xFF3F4946),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

private val SkDarkColors = darkColorScheme(
    primary = Color(0xFF80D5C5),
    onPrimary = Color(0xFF003730),
    primaryContainer = Color(0xFF005046),
    onPrimaryContainer = Color(0xFF9CF1E1),
    secondary = Color(0xFFB1CCC4),
    onSecondary = Color(0xFF1D352F),
    tertiary = Color(0xFFFFB4A9),
    onTertiary = Color(0xFF5F150E),
    background = Color(0xFF0F1513),
    onBackground = Color(0xFFDEE4E1),
    surface = Color(0xFF0F1513),
    onSurface = Color(0xFFDEE4E1),
    surfaceVariant = Color(0xFF3F4946),
    onSurfaceVariant = Color(0xFFBEC9C4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun SengkodeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> SkDarkColors
        else -> SkLightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
