package com.example.ui.util

import com.example.ui.components.toArabicDigits
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.Locale

/* ---------------------------------------------------------------------------
 *  أدوات التاريخ الهجري والميلادي بالعربية (بدون اعتماد على لغة الجهاز)
 * ------------------------------------------------------------------------- */

val HIJRI_MONTHS = listOf(
    "محرّم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
    "رجب", "شعبان", "رمضان", "شوّال", "ذو القعدة", "ذو الحجة",
)

val GREGORIAN_MONTHS = listOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر",
)

val WEEK_DAYS = listOf("الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")

fun hijriMonthName(month: Int): String =
    HIJRI_MONTHS.getOrElse(month - 1) { "" }

fun gregorianMonthName(month: Int): String =
    GREGORIAN_MONTHS.getOrElse(month - 1) { "" }

/** يوم الأسبوع بالعربية لتاريخ ميلادي. */
fun weekDayName(date: LocalDate): String =
    WEEK_DAYS.getOrElse(date.dayOfWeek.value - 1) { "" }

/** التاريخ الهجري الحالي بعد تطبيق تعديل المستخدم (± أيام). */
fun adjustedHijri(offsetDays: Int, base: LocalDate = LocalDate.now()): HijrahDate {
    val shifted = base.plusDays(offsetDays.toLong())
    return HijrahDate.from(shifted)
}

fun hijriDay(date: HijrahDate): Int = date.get(ChronoField.DAY_OF_MONTH)
fun hijriMonth(date: HijrahDate): Int = date.get(ChronoField.MONTH_OF_YEAR)
fun hijriYear(date: HijrahDate): Int = date.get(ChronoField.YEAR)

/** مثال: «١٢ رمضان ١٤٤٧ هـ» */
fun formatHijriLong(date: HijrahDate): String {
    val d = hijriDay(date)
    val m = hijriMonth(date)
    val y = hijriYear(date)
    return "${toArabicDigits(d)} ${hijriMonthName(m)} ${toArabicDigits(y)} هـ"
}

/** مثال: «الأربعاء ٤ سبتمبر ٢٠٢٥ م» */
fun formatGregorianLong(date: LocalDate = LocalDate.now()): String {
    return "${weekDayName(date)} ${toArabicDigits(date.dayOfMonth)} ${gregorianMonthName(date.monthValue)} ${toArabicDigits(date.year)} م"
}

/** عدد أيام الشهر الهجري المُعطى. */
fun hijriMonthLength(year: Int, month: Int): Int {
    return try {
        val first = HijrahDate.of(year, month, 1)
        first.lengthOfMonth()
    } catch (e: Exception) {
        30
    }
}

/** أول أيام الشهر الهجري كتاريخ ميلادي (لحساب موضعه في الشبكة). */
fun hijriMonthStartWeekDayIndex(year: Int, month: Int): Int {
    return try {
        val first = HijrahDate.of(year, month, 1)
        val gregorian = LocalDate.from(first)
        // نبدأ الأسبوع بالسبت كما هو معتاد في التقويم العربي
        when (gregorian.dayOfWeek.value) {
            6 -> 0 // السبت
            7 -> 1 // الأحد
            1 -> 2 // الاثنين
            2 -> 3
            3 -> 4
            4 -> 5
            else -> 6 // الجمعة
        }
    } catch (e: Exception) {
        0
    }
}

fun hijriToGregorian(year: Int, month: Int, day: Int): LocalDate? {
    return try {
        LocalDate.from(HijrahDate.of(year, month, day))
    } catch (e: Exception) {
        null
    }
}

fun daysBetween(from: LocalDate, to: LocalDate): Long = ChronoUnit.DAYS.between(from, to)

/** المناسبات الإسلامية (شهر, يوم) → الاسم. */
val ISLAMIC_OCCASIONS: Map<Pair<Int, Int>, String> = mapOf(
    (1 to 1) to "رأس السنة الهجرية",
    (1 to 10) to "يوم عاشوراء",
    (3 to 12) to "المولد النبوي الشريف",
    (7 to 27) to "الإسراء والمعراج",
    (8 to 15) to "ليلة النصف من شعبان",
    (9 to 1) to "أول رمضان المبارك",
    (9 to 27) to "ليلة القدر (المرجّحة)",
    (10 to 1) to "عيد الفطر المبارك",
    (12 to 9) to "يوم عرفة",
    (12 to 10) to "عيد الأضحى المبارك",
)

/** تنسيق مدة زمنية بالمللي ثانية إلى «٠٢:١٥:٣٣». */
fun formatCountdown(millis: Long): String {
    val total = (millis / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return toArabicDigits(String.format(Locale.US, "%02d:%02d:%02d", h, m, s))
}

/** تنسيق مدة بصيغة نصية مختصرة: «٣ س ١٢ د». */
fun formatDurationShort(millis: Long): String {
    val total = (millis / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    return when {
        h > 0 -> "${toArabicDigits(h)} س ${toArabicDigits(m)} د"
        m > 0 -> "${toArabicDigits(m)} دقيقة"
        else -> "أقل من دقيقة"
    }
}
