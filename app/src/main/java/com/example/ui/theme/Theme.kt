package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val EarthWatchDarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = NavyDeepest,
    primaryContainer = TealContainer,
    onPrimaryContainer = TealBright,
    secondary = InfoCyan,
    onSecondary = NavyDeepest,
    secondaryContainer = InfoCyanContainer,
    onSecondaryContainer = PureWhite,
    tertiary = RiskMediumAmber,
    onTertiary = NavyDeepest,
    tertiaryContainer = RiskMediumContainer,
    onTertiaryContainer = RiskMediumAmber,
    background = NavyDeepest,
    onBackground = OffWhite,
    surface = NavySurface,
    onSurface = OffWhite,
    surfaceVariant = NavyCard,
    onSurfaceVariant = SlateLight,
    outline = NavyBorder,
    outlineVariant = NavyElevated,
    error = RiskHighRed,
    onError = PureWhite,
    errorContainer = RiskHighContainer,
    onErrorContainer = ColorErrorText
)

private val ColorErrorText get() = androidx.compose.ui.graphics.Color(0xFFFCA5A5)

val EarthWatchShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun EarthWatchTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EarthWatchDarkColorScheme,
        typography = EarthWatchTypography,
        shapes = EarthWatchShapes,
        content = content
    )
}
