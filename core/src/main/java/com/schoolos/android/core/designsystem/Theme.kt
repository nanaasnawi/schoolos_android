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
val BaseNeonBlue       = Color(0xFF3B82F6) // Apple System Blue
val BaseNeonBlueLight  = Color(0xFF60A5FA)
val BaseNeonBlueDark   = Color(0xFF2563EB)
val BaseNeonBlueBg     = Color(0xFF1E293B)

val NeonBlue: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF3B82F6) else Color(0xFF2563EB)
val NeonBlueLight = BaseNeonBlueLight
val NeonBlueDark = BaseNeonBlueDark
val NeonBlueBg = BaseNeonBlueBg

// Role subtle accents
val StudentPrimary = Color(0xFF6366F1) // Indigo
val StudentLight = Color(0xFF818CF8)
val StudentContainer: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1B4B) else Color(0xFFEEF2FF)
val StudentNeon: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF818CF8) else Color(0xFF4F46E5)

val TeacherPrimary = Color(0xFF10B981) // Crisp Emerald
val TeacherLight = Color(0xFF34D399)
val TeacherContainer: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF064E3B) else Color(0xFFECFDF5)
val TeacherNeon: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF10B981) else Color(0xFF059669)

val TeacherBrandBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5)
val TeacherBrandBorder: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFA7F3D0)
val TeacherBrandText: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF34D399) else Color(0xFF047857)

val ParentPrimary = Color(0xFFF43F5E) // Calm Rose
val ParentLight = Color(0xFFFB7185)
val ParentContainer: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF881337) else Color(0xFFFFF1F2)
val ParentNeon: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFF43F5E) else Color(0xFFE11D48)

// Minimal semantic accents
val AccentElectricBlue: Color @Composable get() = NeonBlue
val AccentNeonGreen: Color    @Composable get() = TeacherNeon
val AccentNeonAmber: Color    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFF59E0B) else Color(0xFFD97706)
val AccentNeonCoral: Color    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFEF4444) else Color(0xFFDC2626)
val AccentNeonPurple: Color   @Composable get() = StudentNeon
val AccentNeonCyan: Color     @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF06B6D4) else Color(0xFF0891B2)
val AccentNeonPink: Color     @Composable get() = ParentNeon

// Semantic Colors
val NeonSuccess: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF10B981) else Color(0xFF059669)
val NeonWarning: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFF59E0B) else Color(0xFFD97706)
val NeonError: Color   @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFEF4444) else Color(0xFFDC2626)
val NeonInfo: Color    @Composable get() = NeonBlue

// Semantic backgrounds
val SuccessBg: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF064E3B) else Color(0xFFECFDF5)
val WarningBg: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF451A03) else Color(0xFFFFFBEB)
val ErrorBg: Color   @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF4C0519) else Color(0xFFFEF2F2)
val InfoBg: Color    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E293B) else Color(0xFFEFF6FF)

// Aliases
val PrimaryBlue: Color      @Composable get() = NeonBlue
val PrimaryBlueLight: Color @Composable get() = NeonBlueLight
val SoftBlueBg: Color       @Composable get() = InfoBg

val AccentPurple: Color  @Composable get() = StudentNeon
val AccentEmerald: Color @Composable get() = NeonSuccess
val AccentAmber: Color   @Composable get() = NeonWarning
val AccentRose: Color    @Composable get() = NeonError
val AccentBlue: Color    @Composable get() = NeonBlue
val AccentOrange: Color  @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFF97316) else Color(0xFFEA580C)

val SuccessGreen: Color @Composable get() = NeonSuccess
val WarningAmber: Color @Composable get() = NeonWarning
val ErrorRed: Color     @Composable get() = NeonError
val InfoBlue: Color     @Composable get() = NeonInfo

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
    primary              = Color(0xFF2563EB), // Refined Apple/Linear System Blue
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFEFF6FF), // Soft Blue 50
    onPrimaryContainer   = Color(0xFF1D4ED8), // Deep Blue 700
    secondary            = StudentPrimary,    // Color(0xFF6366F1)
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFEEF2FF), // Soft Indigo 50
    onSecondaryContainer = Color(0xFF4338CA), // Deep Indigo 700
    tertiary             = TeacherPrimary,    // Color(0xFF10B981)
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFFECFDF5), // Soft Emerald 50
    onTertiaryContainer  = Color(0xFF047857), // Deep Emerald 700
    background           = Color(0xFFF8FAFC), // Crisp Slate 50
    onBackground         = Color(0xFF0F172A), // Slate 900
    surface              = Color(0xFFFFFFFF), // Pure white
    onSurface            = Color(0xFF0F172A),
    surfaceVariant       = Color(0xFFF1F5F9), // Slate 100
    onSurfaceVariant     = Color(0xFF475569), // Slate 600
    outline              = Color(0xFFCBD5E1), // Slate 300
    outlineVariant       = Color(0xFFE2E8F0), // Slate 200
    error                = Color(0xFFEF4444),
    onError              = Color.White,
    errorContainer       = Color(0xFFFEF2F2), // Soft Red 50
    onErrorContainer     = Color(0xFFB91C1C), // Deep Red 700
    scrim                = Color(0x66000000),
)

// DARK (True OLED Pitch Black)
private val DarkColorScheme = darkColorScheme(
    primary              = Color(0xFF3B82F6),
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFF1E293B),
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
