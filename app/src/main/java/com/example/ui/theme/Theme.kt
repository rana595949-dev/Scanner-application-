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

private val DarkColorScheme =
  darkColorScheme(
    primary = NaturalDarkPrimary,
    secondary = NaturalDarkSecondary,
    tertiary = NaturalDarkTertiary,
    background = NaturalDarkBg,
    surface = NaturalDarkSurface,
    surfaceVariant = NaturalDarkContainer,
    outline = NaturalDarkBorder,
    onPrimary = NaturalDarkBg,
    onSecondary = NaturalDarkText,
    onBackground = NaturalDarkText,
    onSurface = NaturalDarkText,
    onSurfaceVariant = NaturalDarkTertiary
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NaturalLightPrimary,
    secondary = NaturalLightSecondary,
    tertiary = NaturalLightTertiary,
    background = NaturalLightBg,
    surface = NaturalLightSurface,
    surfaceVariant = NaturalLightContainer,
    outline = NaturalLightBorder,
    onPrimary = Color.White,
    onSecondary = NaturalLightText,
    onBackground = NaturalLightText,
    onSurface = NaturalLightText,
    onSurfaceVariant = NaturalLightTertiary
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+ (setting default as false to preserve custom brand colors)
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
