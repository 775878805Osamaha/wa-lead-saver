package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// WA Lead Saver - Professional Dark Identity Palette
// ==========================================

// 1. Core Surfaces & Backgrounds (Warm Black #0a0d0b, Card #121713, Border #1f271f)
val ColorScreenBackground = Color(0xFF0A0D0B)   // #0a0d0b
val ColorHeaderStart = Color(0xFF0F2D22)        // #0f2d22
val ColorHeaderEnd = Color(0xFF0A1D16)          // #0a1d16
val ColorCardBackground = Color(0xFF121713)     // #121713
val ColorCardBorder = Color(0xFF1F271F)         // #1f271f

// 2. Typography
val ColorTextPrimary = Color(0xFFF0F5F2)        // #f0f5f2
val ColorTextSecondary = Color(0xFF8B958F)      // #8b958f
val ColorTextMuted = Color(0xFF7A8580)          // #7a8580
val ColorTextSubtle = Color(0xFF6E786F)         // #6e786f

// 3. Brand WhatsApp Green (#2ee671 and #16a34a)
val ColorBrandGreen = Color(0xFF2EE671)         // #2ee671
val ColorBrandGreenDark = Color(0xFF16A34A)     // #16a34a
val ColorBrandGreenTranslucent = Color(0x242EE671) // rgba(46, 230, 113, 0.14)
val ColorBrandGreenGlow = Color(0x282EE671)     // subtle green drop shadow
val ColorNotificationGlowSubtle = Color(0x0F2EE671) // 6% alpha subtle glow

// 4. Accent & Neutral Tints
val ColorAccentBlue = Color(0xFF7FA6E6)         // #7fa6e6
val ColorAccentBlueTranslucent = Color(0x245A8CE6) // rgba(90, 140, 230, 0.14)

val ColorAlertAmber = Color(0xFFE6A032)         // #e6a032
val ColorAlertAmberTranslucent = Color(0x24E6A032) // rgba(230, 160, 50, 0.14)

val ColorDangerRed = Color(0xFFE05252)          // #e05252
val ColorDangerRedTranslucent = Color(0x24E05252) // rgba(224, 82, 82, 0.14)

// 5. Gradients
val HeaderGradient = Brush.verticalGradient(
    colors = listOf(ColorHeaderStart, ColorHeaderEnd)
)

val BrandGreenGradient = Brush.horizontalGradient(
    colors = listOf(ColorBrandGreenDark, ColorBrandGreen)
)

val AccuracyProgressGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF16A34A), Color(0xFF2EE671))
)

val BrandGreenGradientVertical = Brush.verticalGradient(
    colors = listOf(ColorBrandGreenDark, ColorBrandGreen)
)

// ==========================================
// Compatibility Aliases for Existing Components
// ==========================================
val CrmHeader = ColorHeaderStart
val CrmActiveEmerald = ColorBrandGreen
val CrmBackground = ColorScreenBackground
val CrmCardSurface = ColorCardBackground
val CrmCardElevated = ColorCardBackground
val CrmTextPrimary = ColorTextPrimary
val CrmTextSecondary = ColorTextSecondary
val CrmBorder = ColorCardBorder
val CrmBorderSubtle = ColorCardBorder
val CrmPaleEmerald = ColorBrandGreenTranslucent
val CrmPaleBlue = ColorAccentBlueTranslucent
val CrmPaleAmber = ColorAlertAmberTranslucent
val CrmPaleRed = ColorDangerRedTranslucent

val CrmPrimary = ColorBrandGreen
val CrmPrimaryGlow = ColorBrandGreenTranslucent
val CrmSelectedNavBg = ColorBrandGreenTranslucent
val CrmTextMuted = ColorTextMuted
val CrmSuccess = ColorBrandGreen
val CrmSuccessBg = ColorBrandGreenTranslucent
val CrmWarning = ColorAlertAmber
val CrmWarningBg = ColorAlertAmberTranslucent
val CrmDanger = ColorDangerRed
val CrmDangerBg = ColorDangerRedTranslucent
val CrmDangerCardBg = ColorCardBackground
val CrmDangerBorder = ColorDangerRed
val CrmInfoBlue = ColorAccentBlue
val CrmInfoBlueBg = ColorAccentBlueTranslucent
val CrmPurple = ColorAccentBlue
val CrmPurpleBg = ColorAccentBlueTranslucent
val CrmSlate = ColorTextSecondary
val CrmSlateBg = ColorCardBackground

val DashboardContentBg = ColorScreenBackground
val DashboardHeaderBg = ColorHeaderStart
val DashboardButtonPrimary = ColorBrandGreen
val DashboardSuccess = ColorBrandGreen
val DashboardWarning = ColorAlertAmber
val DashboardTextPrimary = ColorTextPrimary
val DashboardTextSecondary = ColorTextSecondary
val DashboardCardBg = ColorCardBackground
val DashboardCardBorder = ColorCardBorder
val DashboardLightGreenBg = ColorBrandGreenTranslucent
val DashboardLightGreenBorder = ColorCardBorder
val DashboardLightRedBg = ColorDangerRedTranslucent
val DashboardLightRedBorder = ColorDangerRed
val DashboardDanger = ColorDangerRed

val DarkEmeraldBg = ColorScreenBackground
val DarkEmeraldSurface = ColorCardBackground
val DarkEmeraldElevated = ColorCardBackground
val DarkEmeraldBorder = ColorCardBorder
val DarkEmeraldBorderLight = ColorCardBorder
val DarkEmeraldHeader = ColorHeaderStart

val WhatsAppGreen = ColorBrandGreen
val WhatsAppGreenLight = ColorBrandGreen
val WhatsAppGreenGlow = ColorBrandGreenTranslucent
val WhatsAppTeal = ColorBrandGreenDark
val WhatsAppDarkTeal = ColorHeaderStart
val WhatsAppTealAccent = ColorBrandGreen

val TextLightPrimary = ColorTextPrimary
val TextLightSecondary = ColorTextSecondary
val TextLightMuted = ColorTextMuted
val TextEmeraldAccent = ColorBrandGreen

val StatusActiveGreen = ColorBrandGreen
val StatusActiveDot = ColorBrandGreen
val StatusWarningAmber = ColorAlertAmber
val StatusWarningAmberBg = ColorAlertAmberTranslucent
val RemoveRed = ColorDangerRed
val RemoveRedLight = ColorDangerRedTranslucent
val RemoveRedDark = ColorDangerRed
val InfoBlue = ColorAccentBlue
val InfoBlueBg = ColorAccentBlueTranslucent
val PurpleAccent = ColorAccentBlue
val PurpleAccentBg = ColorAccentBlueTranslucent

val DarkTealHeader = ColorHeaderStart
val DarkTealHeaderDarker = ColorHeaderEnd
val WhatsAppTealLight = ColorBrandGreen
val AppBackground = ColorScreenBackground
val CardBackground = ColorCardBackground
val CardBorder = ColorCardBorder
val TextPrimary = ColorTextPrimary
val TextSecondary = ColorTextSecondary
val TextMuted = ColorTextMuted
val StatusInactiveGray = ColorTextMuted
