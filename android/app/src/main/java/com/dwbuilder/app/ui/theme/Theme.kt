package com.dwbuilder.app.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Deepwoken builder palette — deep forest green on near-black, echoing the wiki's `--c-panel` look. */
private val DarkColors = darkColorScheme(
    primary = Color(0xFF86CF93),
    onPrimary = Color(0xFF00391D),
    primaryContainer = Color(0xFF1C5334),
    onPrimaryContainer = Color(0xFFA4F5B0),
    secondary = Color(0xFFB9CDA9),
    onSecondary = Color(0xFF25341C),
    secondaryContainer = Color(0xFF3B4B30),
    onSecondaryContainer = Color(0xFFD5EAC3),
    tertiary = Color(0xFFAACBEA),
    onTertiary = Color(0xFF0A3650),
    tertiaryContainer = Color(0xFF2F4D67),
    onTertiaryContainer = Color(0xFFC8E6FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    background = Color(0xFF10150F),
    onBackground = Color(0xFFE2E4DC),
    surface = Color(0xFF10150F),
    onSurface = Color(0xFFE2E4DC),
    surfaceVariant = Color(0xFF42483E),
    onSurfaceVariant = Color(0xFFC2C8BB),
    outline = Color(0xFF8C9386),
)

/**
 * Material 3 theme (Material You). Uses the system dynamic palette on Android 12+;
 * falls back to the builder's dark forest palette below API 31.
 */
@Composable
fun BuilderTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        DarkColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}