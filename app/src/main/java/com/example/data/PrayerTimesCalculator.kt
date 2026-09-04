package com.example.data

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

object PrayerTimesCalculator {

    enum class CalculationMethod {
        EGYPT, ISNA, MWL, MAKKAH, KARACHI, JAFARI, UMM_AL_QURA
    }

    enum class JuristicMethod {
        STANDARD, HANAFI
    }

    // Coordinates for default major cities if location is not available
    val DEFAULT_CITIES = listOf(
        CityInfo("مكة المكرمة", "Mecca", 21.3891, 39.8579, "Asia/Riyadh"),
        CityInfo("المدينة المنورة", "Medina", 24.4672, 39.6112, "Asia/Riyadh"),
        CityInfo("القدس الشريف", "Jerusalem", 31.7683, 35.2137, "Asia/Jerusalem"),
        CityInfo("القاهرة", "Cairo", 30.0444, 31.2357, "Africa/Cairo"),
        CityInfo("الرياض", "Riyadh", 24.7136, 46.6753, "Asia/Riyadh"),
        CityInfo("دبي", "Dubai", 25.2048, 55.2708, "Asia/Dubai"),
        CityInfo("عمان", "Amman", 31.9522, 35.9106, "Asia/Amman"),
        CityInfo("المنامة", "Manama", 26.2285, 50.5860, "Asia/Bahrain"),
        CityInfo("الكويت", "Kuwait City", 29.3759, 47.9774, "Asia/Kuwait"),
        CityInfo("الدوحة", "Doha", 25.2854, 51.5310, "Asia/Qatar"),
        CityInfo("لندن", "London", 51.5074, -0.1278, "Europe/London"),
        CityInfo("نيويورك", "New York", 40.7128, -74.0060, "America/New_York")
    )

    data class CityInfo(
        val nameAr: String,
        val nameEn: String,
        val latitude: Double,
        val longitude: Double,
        val timezoneId: String
    )

    data class PrayerTimes(
        val fajr: String,
        val sunrise: String,
        val dhuhr: String,
        val asr: String,
        val maghrib: String,
        val isha: String,
        val dateString: String,
        val rawFajrMs: Long,
        val rawSunriseMs: Long,
        val rawDhuhrMs: Long,
        val rawAsrMs: Long,
        val rawMaghribMs: Long,
        val rawIshaMs: Long
    )

    fun calculateTimes(
        latitude: Double,
        longitude: Double,
        timezoneOffset: Double, // in hours, e.g. +3.0
        calendar: Calendar = Calendar.getInstance(),
        method: CalculationMethod = CalculationMethod.UMM_AL_QURA,
        juristic: JuristicMethod = JuristicMethod.STANDARD
    ): PrayerTimes {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // 1. Calculate Julian Date
        val julianDate = getJulianDate(year, month, day)

        // 2. Sun's declination and equation of time
        val d = julianDate - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(r(g)) + 0.020 * sin(r(2 * g)))

        val e = 23.439 - 0.00000036 * d
        val sunDeclination = deg(asin(sin(r(e)) * sin(r(l))))
        val ra = deg(atan2(cos(r(e)) * sin(r(l)), cos(r(l)))) / 15.0
        val eqTime = (q/15.0) - fixHour(ra)

        // 3. Noon / Dhuhr
        val baseDhuhr = 12.0 - longitude / 15.0 + timezoneOffset - eqTime
        val dhuhrHour = fixHour(baseDhuhr)

        // 4. Fajr & Isha angles
        val (fajrAngle, ishaAngle, ishaIntervalMinutes) = when (method) {
            CalculationMethod.EGYPT -> Triple(19.5, 17.5, 0)
            CalculationMethod.ISNA -> Triple(15.0, 15.0, 0)
            CalculationMethod.MWL -> Triple(18.0, 17.0, 0)
            CalculationMethod.MAKKAH -> Triple(18.5, 90.0, 90) // Isha is 90 mins after Maghrib (120 in Ramadan)
            CalculationMethod.KARACHI -> Triple(18.0, 18.0, 0)
            CalculationMethod.UMM_AL_QURA -> Triple(18.5, 90.0, 90) // Isha 90 mins after Maghrib
            CalculationMethod.JAFARI -> Triple(16.0, 14.0, 0)
        }

        // Calculate time for Sunrise
        val sunriseHour = dhuhrHour - hourAngle(0.833, latitude, sunDeclination)
        
        // Calculate time for Fajr
        val fajrHour = dhuhrHour - hourAngle(fajrAngle, latitude, sunDeclination)

        // Calculate time for Asr
        val asrFactor = if (juristic == JuristicMethod.HANAFI) 2.0 else 1.0
        val asrAngleDeg = deg(atan(1.0 / (asrFactor + tan(r(abs(latitude - sunDeclination))))))
        val asrHour = dhuhrHour + hourAngle(90.0 - asrAngleDeg, latitude, sunDeclination)

        // Calculate time for Maghrib / Sunset
        val maghribHour = dhuhrHour + hourAngle(0.833, latitude, sunDeclination)

        // Calculate time for Isha
        val ishaHour = if (ishaIntervalMinutes > 0) {
            maghribHour + (ishaIntervalMinutes / 60.0)
        } else {
            dhuhrHour + hourAngle(ishaAngle, latitude, sunDeclination)
        }

        // Format times
        val calBase = (calendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val rawFajr = calBase.timeInMillis + (fajrHour * 3600000).toLong()
        val rawSunrise = calBase.timeInMillis + (sunriseHour * 3600000).toLong()
        val rawDhuhr = calBase.timeInMillis + (dhuhrHour * 3600000).toLong()
        val rawAsr = calBase.timeInMillis + (asrHour * 3600000).toLong()
        val rawMaghrib = calBase.timeInMillis + (maghribHour * 3600000).toLong()
        val rawIsha = calBase.timeInMillis + (ishaHour * 3600000).toLong()

        val dateStr = String.format("%04d-%02d-%02d", year, month, day)

        return PrayerTimes(
            fajr = formatHour(fajrHour),
            sunrise = formatHour(sunriseHour),
            dhuhr = formatHour(dhuhrHour),
            asr = formatHour(asrHour),
            maghrib = formatHour(maghribHour),
            isha = formatHour(ishaHour),
            dateString = dateStr,
            rawFajrMs = rawFajr,
            rawSunriseMs = rawSunrise,
            rawDhuhrMs = rawDhuhr,
            rawAsrMs = rawAsr,
            rawMaghribMs = rawMaghrib,
            rawIshaMs = rawIsha
        )
    }

    private fun getJulianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun r(deg: Double): Double = deg * PI / 180.0
    private fun deg(rad: Double): Double = rad * 180.0 / PI

    private fun fixAngle(deg: Double): Double {
        var a = deg % 360.0
        if (a < 0) a += 360.0
        return a
    }

    private fun fixHour(hour: Double): Double {
        var h = hour % 24.0
        if (h < 0) h += 24.0
        return h
    }

    private fun hourAngle(angle: Double, latitude: Double, sunDeclination: Double): Double {
        val latRad = r(latitude)
        val declRad = r(sunDeclination)
        val angleRad = r(angle)
        
        // cos(HA) = (sin(-angle) - sin(lat) * sin(decl)) / (cos(lat) * cos(decl))
        val numerator = -sin(angleRad) - sin(latRad) * sin(declRad)
        val denominator = cos(latRad) * cos(declRad)
        val cosHA = numerator / denominator

        if (cosHA < -1.0 || cosHA > 1.0) {
            // Fallback in case of extreme latitudes
            return 6.0
        }
        return deg(acos(cosHA)) / 15.0
    }

    private fun formatHour(hour: Double): String {
        val h = fixHour(hour)
        val totalMinutes = floor(h * 60.0 + 0.5).toInt()
        val hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        return String.format("%02d:%02d", hours, minutes)
    }
}
