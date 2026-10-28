package org.mealforge.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** MealForge brand palette: black base with gold, purple, pink, red and maroon accents. */
object Brand {
    val Gold = Color(0xFFF2C14E)
    val Purple = Color(0xFFB37FEB)
    val Pink = Color(0xFFFF7EB6)
    val Red = Color(0xFFFF6B6B)
    val Maroon = Color(0xFF7A1F35)
    val Black = Color(0xFF0B0709)
    val Surface = Color(0xFF1A1016)
    val SurfaceHigh = Color(0xFF261822)
    val SurfaceHighest = Color(0xFF33202E)
    val OnSurface = Color(0xFFF6EEF2)
    val OnSurfaceMuted = Color(0xFFCDB9C4)
    val Outline = Color(0xFF6E5464)
    val Success = Color(0xFF7BD88F)
}

private val colors = darkColorScheme(
    primary = Brand.Gold,
    onPrimary = Color(0xFF241A00),
    primaryContainer = Brand.Maroon,
    onPrimaryContainer = Color(0xFFFFE3EA),
    secondary = Brand.Pink,
    onSecondary = Color(0xFF3B0020),
    secondaryContainer = Color(0xFF4A2340),
    onSecondaryContainer = Color(0xFFFFD8EA),
    tertiary = Brand.Purple,
    onTertiary = Color(0xFF270046),
    tertiaryContainer = Color(0xFF3D2459),
    onTertiaryContainer = Color(0xFFEFDBFF),
    error = Brand.Red,
    onError = Color(0xFF3F0005),
    background = Brand.Black,
    onBackground = Brand.OnSurface,
    surface = Brand.Black,
    onSurface = Brand.OnSurface,
    surfaceVariant = Brand.SurfaceHigh,
    onSurfaceVariant = Brand.OnSurfaceMuted,
    surfaceContainerLowest = Brand.Black,
    surfaceContainerLow = Brand.Surface,
    surfaceContainer = Brand.Surface,
    surfaceContainerHigh = Brand.SurfaceHigh,
    surfaceContainerHighest = Brand.SurfaceHighest,
    outline = Brand.Outline,
    outlineVariant = Color(0xFF4A3542),
)

private val base = Typography()
private val typography = base.copy(
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Black, color = Brand.Gold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun MealForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
