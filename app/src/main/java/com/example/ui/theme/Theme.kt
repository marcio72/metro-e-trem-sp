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

private val DarkColorScheme = darkColorScheme(
  primary = MetroBlueLight,
  onPrimary = Color.White,
  primaryContainer = MetroBlue,
  onPrimaryContainer = Color.White,
  secondary = MetroAccent,
  onSecondary = Color.Black,
  secondaryContainer = Color(0xFF2C394B),
  onSecondaryContainer = Color.White,
  tertiary = Linha2Verde,
  background = SlateBackground,
  onBackground = Color(0xFFE6EDF3),
  surface = MetroSurfaceDark,
  onSurface = Color(0xFFE6EDF3),
  surfaceVariant = SlateCard,
  onSurfaceVariant = Color(0xFF94A3B8),
  outline = SlateBorder
)

private val LightColorScheme = lightColorScheme(
  primary = MetroBlue,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFD6E4F0),
  onPrimaryContainer = MetroBlue,
  secondary = Color(0xFFD97706),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFFEF3C7),
  onSecondaryContainer = Color(0xFF78350F),
  tertiary = Linha2Verde,
  background = LightBackground,
  onBackground = Color(0xFF0F172A),
  surface = LightSurface,
  onSurface = Color(0xFF0F172A),
  surfaceVariant = Color(0xFFF1F5F9),
  onSurfaceVariant = Color(0xFF475569),
  outline = LightCardBorder
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep consistent transport branding
  content: @Composable () -> Unit,
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
