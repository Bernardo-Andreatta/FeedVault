package com.bernardo.feedvault.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── FeedVault "darkroom" palette ──────────────────────────────────────────────
// A space lit like a photo darkroom: deep ink, a single warm brass safelight as
// the accent, desaturated steel-blue as its cool counterpart. One warm rose for
// "favorite", one signal red for destructive. No violet anywhere — every Material
// role is set so the default purple/teal never leaks through.
private val Ink = Color(0xFF0E1116)
private val Slate = Color(0xFF1A2027)
private val SlateHi = Color(0xFF232C35)
private val Brass = Color(0xFFC8A24B)
private val BrassDim = Color(0xFF3A3115)
private val BrassText = Color(0xFFEBD9A6)
private val Steel = Color(0xFF7C97A6)
private val SteelContainer = Color(0xFF26333C)
private val SteelText = Color(0xFFCFE0EA)
private val Bone = Color(0xFFECE7DC)
private val Muted = Color(0xFF9FB0BC)
private val Outline = Color(0xFF3A454F)
private val OutlineDim = Color(0xFF2A333B)
private val Signal = Color(0xFFD9594C)
private val SignalDim = Color(0xFF3A1A17)
private val SignalText = Color(0xFFF2B3AC)

/** Warm rose reserved exclusively for the "favorite" state (distinct from the brass accent). */
val FavoriteRose = Color(0xFFE0617A)

private val DarkColorScheme = darkColorScheme(
    primary = Brass,
    onPrimary = Ink,
    primaryContainer = BrassDim,
    onPrimaryContainer = BrassText,
    inversePrimary = BrassDim,
    secondary = Steel,
    onSecondary = Ink,
    secondaryContainer = SteelContainer,
    onSecondaryContainer = SteelText,
    tertiary = Steel,
    onTertiary = Ink,
    tertiaryContainer = SteelContainer,
    onTertiaryContainer = SteelText,
    background = Ink,
    onBackground = Bone,
    surface = Slate,
    onSurface = Bone,
    surfaceVariant = SlateHi,
    onSurfaceVariant = Muted,
    surfaceTint = Brass,
    inverseSurface = Bone,
    inverseOnSurface = Ink,
    outline = Outline,
    outlineVariant = OutlineDim,
    error = Signal,
    onError = Ink,
    errorContainer = SignalDim,
    onErrorContainer = SignalText,
    scrim = Color(0xFF000000)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8A6D24),
    onPrimary = Color(0xFFFFFBF2),
    primaryContainer = Color(0xFFF1E4BE),
    onPrimaryContainer = Color(0xFF2A2207),
    inversePrimary = Color(0xFFC8A24B),
    secondary = Color(0xFF3E5867),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE6EC),
    onSecondaryContainer = Color(0xFF1B2730),
    tertiary = Color(0xFF3E5867),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDCE6EC),
    onTertiaryContainer = Color(0xFF1B2730),
    background = Color(0xFFF5F1E8),
    onBackground = Color(0xFF1A2027),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A2027),
    surfaceVariant = Color(0xFFEAE4D6),
    onSurfaceVariant = Color(0xFF55606A),
    surfaceTint = Color(0xFF8A6D24),
    outline = Color(0xFFB7B0A0),
    outlineVariant = Color(0xFFD8D2C4),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

// ── Typography ────────────────────────────────────────────────────────────────
// System sans with deliberate weight + tight tracking for titles; monospace
// reserved for data (counts, sizes, dates) so numbers read like instrument readouts.
private val mono = FontFamily.Monospace

private val FeedVaultTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = mono, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp
    ),
    labelSmall = TextStyle(
        fontFamily = mono, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.8.sp
    )
)

@Composable
fun FeedVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FeedVaultTypography,
        content = content
    )
}
