package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
  primary = BluePrimary,
  onPrimary = Color.White,
  primaryContainer = BluePrimaryContainer,
  onPrimaryContainer = BlueOnPrimaryContainer,
  secondary = TextSecondary,
  onSecondary = Color.White,
  secondaryContainer = SurfaceSubtle,
  onSecondaryContainer = TextPrimary,
  tertiary = BluePrimaryDark,
  onTertiary = Color.White,
  background = BackgroundLight,
  onBackground = TextPrimary,
  surface = SurfaceWhite,
  onSurface = TextPrimary,
  surfaceVariant = SurfaceSubtle,
  onSurfaceVariant = TextSecondary,
  outline = BorderLight,
  outlineVariant = DividerLight,
  error = StockOutText,
  onError = Color.White,
  errorContainer = StockOutBg,
  onErrorContainer = StockOutText,
)

private val DarkColorScheme = darkColorScheme(
  primary = BlueDarkPrimary,
  onPrimary = Color(0xFF0A192F),
  primaryContainer = BlueDarkContainer,
  onPrimaryContainer = Color(0xFFDBEAFE),
  secondary = TextSecondaryDark,
  onSecondary = BackgroundDark,
  secondaryContainer = SurfaceVariantDark,
  onSecondaryContainer = TextPrimaryDark,
  background = BackgroundDark,
  onBackground = TextPrimaryDark,
  surface = SurfaceDark,
  onSurface = TextPrimaryDark,
  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = TextSecondaryDark,
  outline = BorderDark,
  outlineVariant = SurfaceVariantDark,
  error = Color(0xFFFCA5A5),
  onError = Color(0xFF7F1D1D),
  errorContainer = Color(0xFF7F1D1D),
  onErrorContainer = Color(0xFFFEE2E2),
)

val AppShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(20.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun PoojaElectronicsTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = AppTypography,
    shapes = AppShapes,
    content = content,
  )
}
