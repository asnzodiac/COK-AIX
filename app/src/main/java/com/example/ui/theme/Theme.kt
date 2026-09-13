package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AviationDarkColorScheme = darkColorScheme(
  primary = AviationCrimson,
  onPrimary = Color.White,
  primaryContainer = AviationCrimsonDark,
  onPrimaryContainer = Color.White,
  secondary = AviationAmber,
  onSecondary = Color.Black,
  secondaryContainer = Color(0xFF332010),
  onSecondaryContainer = AviationAmber,
  tertiary = AviationTeal,
  onTertiary = Color.Black,
  background = BoardBackground,
  onBackground = BoardTextPrimary,
  surface = BoardSurface,
  onSurface = BoardTextPrimary,
  surfaceVariant = BoardCard,
  onSurfaceVariant = BoardTextMuted,
  outline = BoardBorder,
  error = AviationRed,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = AviationDarkColorScheme,
    typography = Typography,
    content = content
  )
}
