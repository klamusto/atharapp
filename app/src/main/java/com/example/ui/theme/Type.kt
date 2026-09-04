package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * خط شهرزاد الجديد — يُستعمل لنصوص القرآن والأذكار (خط نسخ عربي أصيل).
 */
val ScheherazadeFamily = FontFamily(Font(R.font.scheherazade_new, FontWeight.Normal))

/** خط واجهة المستخدم: خط النظام (يدعم العربية بشكل ممتاز على أندرويد). */
val UiFontFamily = FontFamily.Default

private fun ar(
    size: Int,
    line: Int,
    weight: FontWeight = FontWeight.Normal,
    family: FontFamily = UiFontFamily,
    letterSpacing: Double = 0.0,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = letterSpacing.sp,
    textDirection = TextDirection.Content,
)

val AtharTypography = Typography(
    displayLarge = ar(48, 58, FontWeight.Bold),
    displayMedium = ar(40, 50, FontWeight.Bold),
    displaySmall = ar(34, 44, FontWeight.Bold),

    headlineLarge = ar(30, 40, FontWeight.Bold),
    headlineMedium = ar(26, 36, FontWeight.Bold),
    headlineSmall = ar(22, 30, FontWeight.Bold),

    titleLarge = ar(20, 28, FontWeight.Bold),
    titleMedium = ar(17, 24, FontWeight.SemiBold),
    titleSmall = ar(15, 22, FontWeight.SemiBold),

    bodyLarge = ar(16, 26),
    bodyMedium = ar(14, 22),
    bodySmall = ar(12, 18),

    labelLarge = ar(14, 20, FontWeight.SemiBold),
    labelMedium = ar(12, 16, FontWeight.Medium),
    labelSmall = ar(11, 14, FontWeight.Medium),
)

/** نمط النص القرآني — يُستعمل مع أحجام ديناميكية. */
fun quranTextStyle(fontSize: Float) = TextStyle(
    fontFamily = ScheherazadeFamily,
    fontWeight = FontWeight.Normal,
    fontSize = fontSize.sp,
    lineHeight = (fontSize * 2.0f).sp,
    textDirection = TextDirection.Rtl,
)

/** نمط نص الأذكار. */
fun thikrTextStyle(fontSize: Float = 19f) = TextStyle(
    fontFamily = ScheherazadeFamily,
    fontWeight = FontWeight.Normal,
    fontSize = fontSize.sp,
    lineHeight = (fontSize * 1.9f).sp,
    textDirection = TextDirection.Rtl,
)
