package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = OrangePrimary,
    secondary = TurquoiseSecondary,
    tertiary = DesertSandTertiary,
    background = DarkBackground,
    surface = DarkBackground,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onBackground = Slate800,
    onSurface = Slate800
)

private val LightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    secondary = TurquoiseSecondary,
    tertiary = DesertSandTertiary,
    background = OffWhiteBackground,
    surface = OffWhiteBackground,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onBackground = Slate800,
    onSurface = Slate800
)

@Composable
fun LifeAssistantTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disabling dynamic colors to strictly enforce Immersive UI theme
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
