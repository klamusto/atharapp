package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/* ---------------------------------------------------------------------------
 *  أثر — نظام الألوان
 *  لوحة ألوان "الزمرد والذهب" مصممة خصيصاً للتطبيق مع دعم كامل للوضعين
 *  الفاتح والداكن، وتباين مقروء في كل الحالات.
 * ------------------------------------------------------------------------- */

// ---- Light scheme tokens ----
val GreenPrimaryLight = Color(0xFF116B4C)
val GreenOnPrimaryLight = Color(0xFFFFFFFF)
val GreenContainerLight = Color(0xFFA8F2CD)
val GreenOnContainerLight = Color(0xFF002114)

val SageSecondaryLight = Color(0xFF4B635A)
val SageOnSecondaryLight = Color(0xFFFFFFFF)
val SageContainerLight = Color(0xFFCEE9DC)
val SageOnContainerLight = Color(0xFF072019)

val GoldTertiaryLight = Color(0xFF7C5E10)
val GoldOnTertiaryLight = Color(0xFFFFFFFF)
val GoldContainerLight = Color(0xFFFFE08A)
val GoldOnContainerLight = Color(0xFF261A00)

val BackgroundLight = Color(0xFFF5FAF6)
val OnBackgroundLight = Color(0xFF161D19)
val SurfaceLight = Color(0xFFF5FAF6)
val OnSurfaceLight = Color(0xFF161D19)
val SurfaceVariantLight = Color(0xFFDBE5DE)
val OnSurfaceVariantLight = Color(0xFF404944)
val OutlineLight = Color(0xFF707974)
val OutlineVariantLight = Color(0xFFC0C9C2)

val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFEFF5F0)
val SurfaceContainerLight = Color(0xFFE9F0EB)
val SurfaceContainerHighLight = Color(0xFFE3EAE5)
val SurfaceContainerHighestLight = Color(0xFFDDE4DF)

// ---- Dark scheme tokens ----
val GreenPrimaryDark = Color(0xFF6ADCAB)
val GreenOnPrimaryDark = Color(0xFF003824)
val GreenContainerDark = Color(0xFF005236)
val GreenOnContainerDark = Color(0xFF89F8C6)

val SageSecondaryDark = Color(0xFFB2CCC0)
val SageOnSecondaryDark = Color(0xFF1D352C)
val SageContainerDark = Color(0xFF334C42)
val SageOnContainerDark = Color(0xFFCEE9DC)

val GoldTertiaryDark = Color(0xFFEFC65C)
val GoldOnTertiaryDark = Color(0xFF412D00)
val GoldContainerDark = Color(0xFF5E4400)
val GoldOnContainerDark = Color(0xFFFFE08A)

val BackgroundDark = Color(0xFF0D1411)
val OnBackgroundDark = Color(0xFFDDE5DF)
val SurfaceDark = Color(0xFF0D1411)
val OnSurfaceDark = Color(0xFFDDE5DF)
val SurfaceVariantDark = Color(0xFF3F4943)
val OnSurfaceVariantDark = Color(0xFFBFC9C2)
val OutlineDark = Color(0xFF89938D)
val OutlineVariantDark = Color(0xFF3F4943)

val SurfaceContainerLowestDark = Color(0xFF070F0C)
val SurfaceContainerLowDark = Color(0xFF151C19)
val SurfaceContainerDark = Color(0xFF19211D)
val SurfaceContainerHighDark = Color(0xFF232B27)
val SurfaceContainerHighestDark = Color(0xFF2E3632)

// ---- Shared ----
val ErrorLight = Color(0xFFBA1A1A)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)
val ErrorDark = Color(0xFFFFB4AB)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

/**
 * ألوان إضافية خارج نطاق Material 3 يستعملها التطبيق (تدرجات، ذهبي، ورق المصحف…)
 */
@Immutable
data class AtharExtraColors(
    val gold: Color,
    val goldSoft: Color,
    val heroStart: Color,
    val heroMid: Color,
    val heroEnd: Color,
    val onHero: Color,
    val onHeroMuted: Color,
    val success: Color,
    val info: Color,
    val warning: Color,
    val paper: Color,
    val onPaper: Color,
    val paperAccent: Color,
    val shimmer: Color,
    val isDark: Boolean,
)

val LightExtraColors = AtharExtraColors(
    gold = Color(0xFFB8912F),
    goldSoft = Color(0xFFF3E2B4),
    heroStart = Color(0xFF0B5138),
    heroMid = Color(0xFF10714D),
    heroEnd = Color(0xFF13835A),
    onHero = Color(0xFFFFFFFF),
    onHeroMuted = Color(0xCCFFFFFF),
    success = Color(0xFF2E7D48),
    info = Color(0xFF2A6C9B),
    warning = Color(0xFFB2661A),
    paper = Color(0xFFFCF8EF),
    onPaper = Color(0xFF1E1B12),
    paperAccent = Color(0xFFE8DCBF),
    shimmer = Color(0x22000000),
    isDark = false,
)

val DarkExtraColors = AtharExtraColors(
    gold = Color(0xFFE9C56B),
    goldSoft = Color(0xFF4A3C13),
    heroStart = Color(0xFF04120C),
    heroMid = Color(0xFF0A3526),
    heroEnd = Color(0xFF0F5238),
    onHero = Color(0xFFEFF6F1),
    onHeroMuted = Color(0xB3EFF6F1),
    success = Color(0xFF7ED9A2),
    info = Color(0xFF8CC6F0),
    warning = Color(0xFFF0B074),
    paper = Color(0xFF141A16),
    onPaper = Color(0xFFE6EDE8),
    paperAccent = Color(0xFF2A342E),
    shimmer = Color(0x22FFFFFF),
    isDark = true,
)

val LocalAtharColors = staticCompositionLocalOf { LightExtraColors }
