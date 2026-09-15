package com.example.routy.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.routy.core.preferences.domain.Appearance

object RoutySpacing {
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val section = 32.dp
}

val Lagoon = Color(0xFF007E80)
val Ink = Color(0xFF142C35)

@Composable
fun RoutyTheme(
    appearance: Appearance,
    content: @Composable () -> Unit,
) {
    val dark = appearance == Appearance.Dark || (appearance == Appearance.System && isSystemInDarkTheme())
    val colors =
        if (dark) {
            darkColorScheme(
                primary = Color(0xFF79DBD1),
                onPrimary = Color(0xFF003735),
                background = Color(0xFF101D22),
                surface = Color(0xFF17292F),
                surfaceContainer = Color(0xFF21353B),
                onSurface = Color(0xFFE4F1F1),
                primaryContainer = Color(0xFF254D4D),
                secondaryContainer = Color(0xFF254D4D),
                onSecondaryContainer = Color(0xFFAEF3E9),
                onPrimaryContainer = Color(0xFFAEF3E9),
            )
        } else {
            lightColorScheme(
                primary = Lagoon,
                onPrimary = Color.White,
                background = Color(0xFFF3F6F5),
                surface = Color.White,
                surfaceContainer = Color(0xFFEAF1EF),
                onSurface = Ink,
                primaryContainer = Color(0xFFD6F3EB),
                secondaryContainer = Color(0xFFD6F3EB),
                onSecondaryContainer = Color(0xFF084B49),
                onPrimaryContainer = Color(0xFF084B49),
                secondary = Color(0xFF546E76),
                outlineVariant = Color(0xFFDCE5E2),
            )
        }
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
                headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 39.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp),
                headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold),
                titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
                bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
                labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
            ),
        content = content,
    )
}
