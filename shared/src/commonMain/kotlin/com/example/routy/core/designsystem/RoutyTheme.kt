package com.example.routy.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routy.core.preferences.domain.Appearance
import com.example.routy.core.preferences.domain.ColorTheme

object RoutySpacing {
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val section = 32.dp
}

data class RoutyPalette(
    val route: Color,
    val routeOutline: Color,
    val selected: Color,
    val routeColors: List<Color>,
    val searchSheet: Color,
)

val LocalRoutyPalette =
    staticCompositionLocalOf {
        RoutyPalette(
            route = Color(0xFF2563EB),
            routeOutline = Color.White,
            selected = Color(0xFFFFB800),
            routeColors = routeColors,
            searchSheet = Color(0xFFF8FAFC),
        )
    }

@Composable
fun RoutyTheme(
    appearance: Appearance,
    colorTheme: ColorTheme,
    content: @Composable () -> Unit,
) {
    val dark = appearance == Appearance.Dark || (appearance == Appearance.System && isSystemInDarkTheme())
    val colors = colorScheme(colorTheme, dark)
    val palette =
        RoutyPalette(
            route = colors.primary,
            routeOutline = colors.surface,
            selected = colors.tertiary,
            routeColors = routeColors,
            searchSheet = if (dark) colors.surface else Color(0xFFF8FAFC),
        )
    CompositionLocalProvider(LocalRoutyPalette provides palette) {
        MaterialTheme(
            colorScheme = colors,
            shapes =
                Shapes(
                    small = RoundedCornerShape(12.dp),
                    medium = RoundedCornerShape(20.dp),
                    large = RoundedCornerShape(28.dp),
                ),
            typography =
                Typography(
                    headlineLarge =
                        TextStyle(
                            fontSize = 32.sp,
                            lineHeight = 39.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.7).sp,
                        ),
                    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold),
                    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
                    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
                    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
                ),
            content = content,
        )
    }
}

private val routeColors =
    listOf(
        Color(0xFF2563EB),
        Color(0xFFEF4444),
        Color(0xFF16A34A),
        Color(0xFFF59E0B),
        Color(0xFF7C3AED),
        Color(0xFF06B6D4),
        Color(0xFFEC4899),
        Color(0xFF64748B),
    )

private fun colorScheme(
    theme: ColorTheme,
    dark: Boolean,
): ColorScheme =
    when (theme) {
        ColorTheme.Ocean ->
            if (dark) {
                darkScheme(
                    primary = Color(0xFF60A5FA),
                    accent = Color(0xFF22D3EE),
                    background = Color(0xFF091426),
                    surface = Color(0xFF12213A),
                )
            } else {
                lightScheme(primary = Color(0xFF2563EB), accent = Color(0xFF06B6D4), background = Color(0xFFF6F9FF), surface = Color.White)
            }
        ColorTheme.Violet ->
            if (dark) {
                darkScheme(
                    primary = Color(0xFFA78BFA),
                    accent = Color(0xFF22D3EE),
                    background = Color(0xFF17102B),
                    surface = Color(0xFF251B40),
                )
            } else {
                lightScheme(primary = Color(0xFF6D4AFF), accent = Color(0xFF8B5CF6), background = Color(0xFFFAF8FF), surface = Color.White)
            }
        ColorTheme.Mint ->
            if (dark) {
                darkScheme(
                    primary = Color(0xFF2DD4BF),
                    accent = Color(0xFF5EEAD4),
                    background = Color(0xFF071D1B),
                    surface = Color(0xFF102B28),
                )
            } else {
                lightScheme(primary = Color(0xFF008C82), accent = Color(0xFF14B8A6), background = Color(0xFFF4FBF9), surface = Color.White)
            }
        ColorTheme.Mono ->
            if (dark) {
                darkScheme(
                    primary = Color(0xFFF5F5F5),
                    accent = Color(0xFF3B82F6),
                    background = Color(0xFF0B0B0C),
                    surface = Color(0xFF171719),
                )
            } else {
                lightScheme(primary = Color(0xFF18181B), accent = Color(0xFF3B82F6), background = Color(0xFFF7F7F8), surface = Color.White)
            }
    }

private fun lightScheme(
    primary: Color,
    accent: Color,
    background: Color,
    surface: Color,
): ColorScheme =
    lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primary.copy(alpha = 0.14f),
        onPrimaryContainer = primary,
        secondary = accent,
        tertiary = Color(0xFFFFB800),
        background = background,
        surface = surface,
        surfaceContainer = Color(0xFFF0F3F7),
        onSurface = Color(0xFF172033),
        onSurfaceVariant = Color(0xFF64748B),
        outlineVariant = Color(0xFFE1E7EF),
        error = Color(0xFFEF4444),
    )

private fun darkScheme(
    primary: Color,
    accent: Color,
    background: Color,
    surface: Color,
): ColorScheme =
    darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF07121B),
        primaryContainer = primary.copy(alpha = 0.22f),
        onPrimaryContainer = primary,
        secondary = accent,
        tertiary = Color(0xFFFFB800),
        background = background,
        surface = surface,
        surfaceContainer = Color(0xFF243044),
        onSurface = Color(0xFFF2F5F8),
        onSurfaceVariant = Color(0xFFB0BBCB),
        outlineVariant = Color(0xFF3A4658),
        error = Color(0xFFFF6B6B),
    )
