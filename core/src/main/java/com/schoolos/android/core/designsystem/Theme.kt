package com.schoolos.android.core.designsystem

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ─── Apple & Linear Ultra-Minimalist Design System — Color Tokens ───────────

// Refined Accents (Subtle & Disciplined, Not Neon)
val NeonBlue       = Color(0xFF3B82F6) // Apple System Blue
val NeonBlueLight  = Color(0xFF60A5FA)
val NeonBlueDark   = Color(0xFF2563EB)
val NeonBlueBg     = Color(0xFF1E293B)

// Role subtle accents
val StudentPrimary    = Color(0xFF6366F1) // Indigo
val StudentLight      = Color(0xFF818CF8)
val StudentContainer  = Color(0xFF1E1B4B)
val StudentNeon       = Color(0xFF818CF8)

val TeacherPrimary    = Color(0xFF10B981) // Crisp Emerald
val TeacherLight      = Color(0xFF34D399)
val TeacherContainer  = Color(0xFF064E3B)
val TeacherNeon       = Color(0xFF10B981)

val ParentPrimary     = Color(0xFFF43F5E) // Calm Rose
val ParentLight       = Color(0xFFFB7185)
val ParentContainer   = Color(0xFF881337)
val ParentNeon        = Color(0xFFF43F5E)

// Minimal semantic accents
val AccentElectricBlue  = Color(0xFF3B82F6)
val AccentNeonGreen     = Color(0xFF10B981)
val AccentNeonAmber     = Color(0xFFF59E0B)
val AccentNeonCoral     = Color(0xFFEF4444)
val AccentNeonPurple    = Color(0xFF818CF8)
val AccentNeonCyan      = Color(0xFF06B6D4)
val AccentNeonPink      = Color(0xFFEC4899)

// Semantic Colors
val NeonSuccess  = Color(0xFF10B981)
val NeonWarning  = Color(0xFFF59E0B)
val NeonError    = Color(0xFFEF4444)
val NeonInfo     = Color(0xFF3B82F6)

// Semantic backgrounds
val SuccessBg   = Color(0xFF064E3B)
val WarningBg   = Color(0xFF451A03)
val ErrorBg     = Color(0xFF4C0519)
val InfoBg      = Color(0xFF1E293B)

// Aliases
val PrimaryBlue       = NeonBlue
val PrimaryBlueLight  = NeonBlueLight
val SoftBlueBg        = NeonBlueBg

val AccentPurple      = StudentNeon
val AccentEmerald     = NeonSuccess
val AccentAmber       = NeonWarning
val AccentRose        = NeonError
val AccentBlue        = AccentElectricBlue
val AccentOrange      = Color(0xFFF97316)

val SuccessGreen = NeonSuccess
val WarningAmber = NeonWarning
val ErrorRed     = NeonError
val InfoBlue     = NeonInfo

// ─── Reactive Theme Color Tokens (Matte Obsidian & Zinc Hierarchy) ──────────
val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFF4F4F5) else Color(0xFF0F172A)

val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFA1A1AA) else Color(0xFF475569)

val TextTertiary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF71717A) else Color(0xFF64748B)

val TextDisabled: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF52525B) else Color(0xFF94A3B8)

// Solid Page Background — PURE PITCH BLACK (True OLED Black #000000)
val CosmicBlack: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF000000) else Color(0xFFF8FAFC)

// Solid Container Background — Deep Obsidian Container
val CosmicDark: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF0A0A0B) else Color(0xFFF1F5F9)

// SOLID Card Background — Refined Matte Obsidian Surface (#111113)
val CosmicNavy: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF111113) else Color(0xFFFFFFFF)

// Solid Surface Background
val CosmicSurface: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF111113) else Color(0xFFFFFFFF)

val CosmicSurface2: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF18181B) else Color(0xFFF1F5F9)

val CosmicSurface3: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF27272A) else Color(0xFFE2E8F0)

// Hairline Clean Borders (Subtle, Non-Distracting)
val GlassBorder: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1F1F23) else Color(0xFFE2E8F0)

val GlassBorder2: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF27272A) else Color(0xFFCBD5E1)

val GlassOverlay: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0x80000000) else Color(0x0A000000)

val Slate950: Color @Composable get() = TextPrimary
val Slate900: Color @Composable get() = TextPrimary
val Slate800: Color @Composable get() = TextSecondary
val Slate700: Color @Composable get() = TextSecondary
val Slate600: Color @Composable get() = TextTertiary
val Slate500: Color @Composable get() = TextTertiary
val Slate400: Color @Composable get() = TextDisabled
val Slate200: Color @Composable get() = GlassBorder2
val Slate100: Color @Composable get() = GlassBorder
val Slate50: Color  @Composable get() = CosmicDark

// ─── Standard Layout Spacing Tokens (Consistent Mobile Design System) ──────
val ScreenHorizontalPadding = 16.dp
val ScreenCompactPadding    = 12.dp
val CardInnerPadding        = 16.dp
val StandardCardRadius      = 16.dp
val StandardHeaderRadius    = 20.dp

// ─── Typography ─────────────────────────────────────────────────────────────
val SchoolOsTypography = Typography(
    displayLarge   = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Black,     fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Black,     fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall   = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,     fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,     fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,     fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,  fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,  fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge      = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,    fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,    fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall      = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,    fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,      fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,  fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,    fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

// ─── Color Schemes ──────────────────────────────────────────────────────────

// LIGHT (default theme for modern educational app)
private val LightColorScheme = lightColorScheme(
    primary              = NeonBlue,
    onPrimary            = Color.White,
    primaryContainer     = NeonBlueBg,
    onPrimaryContainer   = NeonBlue,
    secondary            = StudentNeon,
    onSecondary          = Color.White,
    secondaryContainer   = StudentContainer,
    onSecondaryContainer = StudentNeon,
    tertiary             = TeacherNeon,
    onTertiary           = Color.White,
    tertiaryContainer    = TeacherContainer,
    onTertiaryContainer  = TeacherNeon,
    background           = Color(0xFFF1F5F9), // Solid porcelain slate background
    onBackground         = Color(0xFF0F172A), // TextPrimary
    surface              = Color(0xFFFFFFFF), // Solid pure white card surface
    onSurface            = Color(0xFF0F172A),
    surfaceVariant       = Color(0xFFE2E8F0),
    onSurfaceVariant     = Color(0xFF334155),
    outline              = Color(0xFFCBD5E1),
    outlineVariant       = Color(0xFFE2E8F0),
    error                = NeonError,
    onError              = Color.White,
    errorContainer       = ErrorBg,
    onErrorContainer     = NeonError,
    scrim                = Color(0x66000000),
)

// DARK (True OLED Pitch Black)
private val DarkColorScheme = darkColorScheme(
    primary              = Color(0xFF3B82F6),
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFF141416),
    onPrimaryContainer   = Color(0xFF93C5FD),
    secondary            = Color(0xFF8B5CF6),
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFF2E1065),
    onSecondaryContainer = Color(0xFFDDD6FE),
    tertiary             = Color(0xFF10B981),
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFF064E3B),
    onTertiaryContainer  = Color(0xFFA7F3D0),
    background           = Color(0xFF000000), // Pure Pitch Black
    onBackground         = Color(0xFFFFFFFF), // Pure white / bright text
    surface              = Color(0xFF101010), // Solid pitch black card background
    onSurface            = Color(0xFFF8FAFC),
    surfaceVariant       = Color(0xFF18181A), // Secondary dark surface
    onSurfaceVariant     = Color(0xFFA1A1AA), // Secondary text color in dark mode
    outline              = Color(0xFF27272A), // Dark mode border
    outlineVariant       = Color(0xFF1F1F23),
    error                = Color(0xFFEF4444),
    onError              = Color.White,
    errorContainer       = Color(0xFF7F1D1D),
    onErrorContainer     = Color(0xFFFECACA),
    scrim                = Color(0xEE000000),
)

// ─── Theme Entry Point ───────────────────────────────────────────────────────
@Composable
fun SchoolOsTheme(
    darkTheme: Boolean = false, // Default to LIGHT mode for educational UX!
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = SchoolOsTypography,
            content     = content,
        )
    }
}
