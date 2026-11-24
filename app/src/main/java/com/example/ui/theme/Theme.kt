package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val NightCoffeeDarkColorScheme = darkColorScheme(
    primary = CaramelGold,
    onPrimary = DarkEspressoInk,
    primaryContainer = CaramelContainer,
    onPrimaryContainer = WarmCream,
    secondary = ToastedLatte,
    onSecondary = DarkEspressoInk,
    secondaryContainer = RoastedBeanElevated,
    onSecondaryContainer = WarmCream,
    tertiary = FamilyHoney,
    onTertiary = DarkEspressoInk,
    tertiaryContainer = FamilyBadgeContainer,
    onTertiaryContainer = FamilyHoney,
    background = EspressoBlack,
    onBackground = WarmCream,
    surface = DeepRoastSurface,
    onSurface = WarmCream,
    surfaceVariant = MochaCardSurface,
    onSurfaceVariant = ToastedLatte,
    surfaceContainerLowest = EspressoBlack,
    surfaceContainerLow = DeepRoastSurface,
    surfaceContainer = MochaCardSurface,
    surfaceContainerHigh = RoastedBeanElevated,
    surfaceContainerHighest = RoastedBeanElevated,
    outline = CocoaBorder,
    outlineVariant = CocoaBorder,
    error = ClosedRed,
    onError = DarkEspressoInk,
    errorContainer = ClosedRedContainer,
    onErrorContainer = ClosedRed
)

val NightCoffeeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun NightCoffeeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NightCoffeeDarkColorScheme,
        typography = NightCoffeeTypography,
        shapes = NightCoffeeShapes,
        content = content
    )
}
