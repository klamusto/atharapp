package com.example.data

import android.app.Application
import android.content.Context
import android.util.Log
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import java.util.Calendar
import java.util.TimeZone
import java.io.File
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

/**
 * وجهات التنقّل داخل التطبيق.
 * [isRoot] تعني أنّها من وجهات الشريط السفلي (لا تتراكم في مكدّس الرجوع).
 */
enum class AtharTab(val isRoot: Boolean = false) {
    HOME(isRoot = true),
    QURAN(isRoot = true),
    AZKAR(isRoot = true),
    PRAYER_TIMES(isRoot = true),
    MORE(isRoot = true),
    HIJRI,
    QIBLA,
    TASBIH,
    AUDIOPLAYER,
    DOWNLOADS,
    FAVORITES,
    SETTINGS,
    ABOUT,
}

/** أذكار السبحة الإلكترونية. */
data class TasbihPhrase(val text: String, val target: Int, val virtue: String = "")

val TASBIH_PHRASES = listOf(
    TasbihPhrase("سُبْحَانَ اللهِ", 33, "غرست له نخلة في الجنة"),
    TasbihPhrase("الْحَمْدُ للهِ", 33, "تملأ الميزان"),
    TasbihPhrase("اللهُ أَكْبَرُ", 34, "تملأ ما بين السماء والأرض"),
    TasbihPhrase("لَا إِلَهَ إِلَّا اللهُ", 100, "أفضل الذكر"),
    TasbihPhrase("أَسْتَغْفِرُ اللهَ", 100, "جعل الله له من كل ضيق مخرجاً"),
    TasbihPhrase("لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللهِ", 100, "كنز من كنوز الجنة"),
    TasbihPhrase("اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ", 100, "صلى الله عليه بها عشراً"),
    TasbihPhrase("سُبْحَانَ اللهِ وَبِحَمْدِهِ", 100, "حُطّت خطاياه وإن كانت مثل زبد البحر"),
)

class AtharViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AtharRepository(application)

    // Vibrator
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    // 1. Tab Navigation State — مكدّس تنقّل حقيقي يدعم زر الرجوع في النظام
    private val _backStack = MutableStateFlow(listOf(AtharTab.HOME))
    val backStack: StateFlow<List<AtharTab>> = _backStack.asStateFlow()

    private val _currentTab = MutableStateFlow(AtharTab.HOME)
    val currentTab: StateFlow<AtharTab> = _currentTab.asStateFlow()

    /** الانتقال إلى وجهة. وجهات الجذر تُصفّر المكدّس، وغيرها تُضاف إليه. */
    fun navigateTo(tab: AtharTab) {
        val stack = _backStack.value
        if (stack.lastOrNull() == tab) return
        _backStack.value = when {
            tab.isRoot && tab == AtharTab.HOME -> listOf(AtharTab.HOME)
            tab.isRoot -> listOf(AtharTab.HOME, tab)
            else -> stack + tab
        }
        _currentTab.value = tab
    }

    /** رجوع خطوة. يعيد false إذا لم يعد هناك ما يُرجع إليه (يُغلق التطبيق). */
    fun navigateBack(): Boolean {
        val stack = _backStack.value
        if (stack.size <= 1) return false
        val newStack = stack.dropLast(1)
        _backStack.value = newStack
        _currentTab.value = newStack.last()
        return true
    }

    fun navigateHome() {
        _backStack.value = listOf(AtharTab.HOME)
        _currentTab.value = AtharTab.HOME
    }

    @Deprecated("استخدم navigateTo", ReplaceWith("navigateTo(tab)"))
    fun selectTab(tab: AtharTab) = navigateTo(tab)

    // 2. Settings States
    val isDarkMode: StateFlow<Boolean> = repository.getSettingFlow("dark_mode", "false")
        .map { it.toBoolean() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hijriOffset: StateFlow<Int> = repository.getSettingFlow("hijri_offset", "0")
        .map { it.toIntOrNull() ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val quranPageScale: StateFlow<Float> = repository.getSettingFlow("quran_scale", "1.0")
        .map { it.toFloatOrNull() ?: 1.0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val quranLastPage: StateFlow<Int> = repository.getSettingFlow("quran_last_page", "1")
        .map { it.toIntOrNull() ?: 1 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { repository.saveSetting("dark_mode", enabled.toString()) }
    }

    fun adjustHijriOffset(delta: Int) {
        viewModelScope.launch {
            val current = hijriOffset.value
            val newValue = current + delta
            repository.saveSetting("hijri_offset", newValue.toString())
        }
    }

    fun resetHijriOffset() {
        viewModelScope.launch { repository.saveSetting("hijri_offset", "0") }
    }

    fun saveQuranScale(scale: Float) {
        viewModelScope.launch { repository.saveSetting("quran_scale", scale.toString()) }
    }

    fun saveQuranLastPage(page: Int) {
        viewModelScope.launch { repository.saveSetting("quran_last_page", page.toString()) }
    }

    fun isNetworkAvailable(): Boolean = repository.isNetworkAvailable()

    // 3. Azkar States
    private val _allThikrs = MutableStateFlow<List<Thikr>>(emptyList())
    val allThikrs: StateFlow<List<Thikr>> = _allThikrs.asStateFlow()

    // Quran Outline State
    private val _quranOutline = MutableStateFlow<List<SurahOutlineItem>>(emptyList())
    val quranOutline: StateFlow<List<SurahOutlineItem>> = _quranOutline.asStateFlow()

    val favorites: StateFlow<List<Int>> = repository.getFavoritesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customCategoryOrder: StateFlow<List<String>> = repository.getCustomCategoryOrderFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Dynamic counts (ThikrId -> remaining count)
    private val _thikrCounts = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val thikrCounts: StateFlow<Map<Int, Int>> = _thikrCounts.asStateFlow()

    // Reordering mode status
    private val _isReorderingCategories = MutableStateFlow(false)
    val isReorderingCategories: StateFlow<Boolean> = _isReorderingCategories.asStateFlow()

    // Location and Prayer Times states
    val selectedCity = MutableStateFlow<PrayerTimesCalculator.CityInfo>(PrayerTimesCalculator.DEFAULT_CITIES[0]) // default to Mecca
    val isLocationFetched = MutableStateFlow(false)
    val fetchedLocationName = MutableStateFlow("مكة المكرمة")
    val currentLatitude = MutableStateFlow(21.3891)
    val currentLongitude = MutableStateFlow(39.8579)
    val prayerTimes = MutableStateFlow<PrayerTimesCalculator.PrayerTimes?>(null)
    
    // Online City Search States
    val searchResults = MutableStateFlow<List<PrayerTimesCalculator.CityInfo>>(emptyList())
    val isSearching = MutableStateFlow(false)
    
    // Athan Settings
    val isAthanEnabled = MutableStateFlow(true)
    val isAlertOnly = MutableStateFlow(false)
    val selectedAthanSound = MutableStateFlow("Muaiqly") // Muaiqly, Makkah, Madinah
    val athanVolume = MutableStateFlow(0.8f)

    // Azkar Notification Settings
    val isMorningNotificationEnabled = MutableStateFlow(true)
    val isEveningNotificationEnabled = MutableStateFlow(true)
    val isSleepNotificationEnabled = MutableStateFlow(true)
    val morningNotificationTime = MutableStateFlow("05:30")
    val eveningNotificationTime = MutableStateFlow("17:30")
    val sleepNotificationTime = MutableStateFlow("22:00")

    // ---------------------------------------------------------------------
    // السبحة الإلكترونية
    // ---------------------------------------------------------------------
    val tasbihPhraseIndex = MutableStateFlow(0)
    val tasbihCount = MutableStateFlow(0)
    val tasbihRounds = MutableStateFlow(0)
    val tasbihTotal = MutableStateFlow(0)
    val tasbihVibrate = MutableStateFlow(true)

    fun selectTasbihPhrase(index: Int) {
        if (index !in TASBIH_PHRASES.indices) return
        tasbihPhraseIndex.value = index
        tasbihCount.value = 0
        savePreference("tasbih_phrase", index.toString())
        persistTasbih()
    }

    fun incrementTasbih() {
        val target = TASBIH_PHRASES[tasbihPhraseIndex.value].target
        val next = tasbihCount.value + 1
        tasbihTotal.value = tasbihTotal.value + 1
        if (next >= target) {
            tasbihCount.value = 0
            tasbihRounds.value = tasbihRounds.value + 1
            if (tasbihVibrate.value) vibrateDevice(180)
        } else {
            tasbihCount.value = next
            if (tasbihVibrate.value) vibrateDevice(24)
        }
        persistTasbih()
    }

    fun resetTasbih() {
        tasbihCount.value = 0
        tasbihRounds.value = 0
        persistTasbih()
    }

    fun resetTasbihTotal() {
        tasbihTotal.value = 0
        persistTasbih()
    }

    fun setTasbihVibrate(enabled: Boolean) {
        tasbihVibrate.value = enabled
        savePreference("tasbih_vibrate", enabled)
    }

    private fun persistTasbih() {
        savePreference("tasbih_count", tasbihCount.value.toString())
        savePreference("tasbih_rounds", tasbihRounds.value.toString())
        savePreference("tasbih_total", tasbihTotal.value.toString())
    }

    // ---------------------------------------------------------------------
    // الصلاة القادمة
    // ---------------------------------------------------------------------
    data class NextPrayer(
        val name: String,
        val time24: String,
        val timeMs: Long,
        val isTomorrow: Boolean,
    )

    /** يحسب الصلاة القادمة بالاعتماد على المواقيت المحسوبة حالياً. */
    fun computeNextPrayer(nowMs: Long = System.currentTimeMillis()): NextPrayer? {
        val t = prayerTimes.value ?: return null
        val list = listOf(
            Triple("الفجر", t.fajr, t.rawFajrMs),
            Triple("الشروق", t.sunrise, t.rawSunriseMs),
            Triple("الظهر", t.dhuhr, t.rawDhuhrMs),
            Triple("العصر", t.asr, t.rawAsrMs),
            Triple("المغرب", t.maghrib, t.rawMaghribMs),
            Triple("العشاء", t.isha, t.rawIshaMs),
        )
        val upcoming = list.firstOrNull { it.third > nowMs }
        if (upcoming != null) {
            return NextPrayer(upcoming.first, upcoming.second, upcoming.third, false)
        }
        // كل مواقيت اليوم مضت ⇒ فجر الغد
        val first = list.first()
        return NextPrayer(first.first, first.second, first.third + 24L * 60L * 60L * 1000L, true)
    }

    /** اسم الصلاة الحالية (التي دخل وقتها ولم تنتهِ بعد). */
    fun currentPrayerName(nowMs: Long = System.currentTimeMillis()): String? {
        val t = prayerTimes.value ?: return null
        val list = listOf(
            "الفجر" to t.rawFajrMs,
            "الشروق" to t.rawSunriseMs,
            "الظهر" to t.rawDhuhrMs,
            "العصر" to t.rawAsrMs,
            "المغرب" to t.rawMaghribMs,
            "العشاء" to t.rawIshaMs,
        )
        var current: String? = null
        for ((name, ms) in list) {
            if (ms <= nowMs) current = name
        }
        return current
    }

    init {
        loadAzkar()
        loadQuranOutline()

        val context = application
        val prefs = context.getSharedPreferences("athar_settings_prefs", Context.MODE_PRIVATE)
        isAthanEnabled.value = prefs.getBoolean("athan_enabled", true)
        isAlertOnly.value = prefs.getBoolean("athan_alert_only", false)
        selectedAthanSound.value = prefs.getString("athan_sound", "Muaiqly") ?: "Muaiqly"
        athanVolume.value = prefs.getFloat("athan_volume", 0.8f)

        isMorningNotificationEnabled.value = prefs.getBoolean("morning_enabled", true)
        isEveningNotificationEnabled.value = prefs.getBoolean("evening_enabled", true)
        isSleepNotificationEnabled.value = prefs.getBoolean("sleep_enabled", true)
        morningNotificationTime.value = prefs.getString("morning_time", "05:30") ?: "05:30"
        eveningNotificationTime.value = prefs.getString("evening_time", "17:30") ?: "17:30"
        sleepNotificationTime.value = prefs.getString("sleep_time", "22:00") ?: "22:00"

        // السبحة
        tasbihPhraseIndex.value = (prefs.getString("tasbih_phrase", "0") ?: "0").toIntOrNull()
            ?.coerceIn(0, TASBIH_PHRASES.lastIndex) ?: 0
        tasbihCount.value = (prefs.getString("tasbih_count", "0") ?: "0").toIntOrNull() ?: 0
        tasbihRounds.value = (prefs.getString("tasbih_rounds", "0") ?: "0").toIntOrNull() ?: 0
        tasbihTotal.value = (prefs.getString("tasbih_total", "0") ?: "0").toIntOrNull() ?: 0
        tasbihVibrate.value = prefs.getBoolean("tasbih_vibrate", true)

        val isFetched = prefs.getBoolean("is_location_fetched", false)
        isLocationFetched.value = isFetched
        
        val savedCityName = prefs.getString("selected_city_name", "مكة المكرمة") ?: "مكة المكرمة"
        val savedLat = prefs.getFloat("selected_city_lat", 21.3891f).toDouble()
        val savedLon = prefs.getFloat("selected_city_lon", 39.8579f).toDouble()
        val savedFetchedName = prefs.getString("fetched_location_name", "موقعي الحالي") ?: "موقعي الحالي"

        fetchedLocationName.value = savedFetchedName
        currentLatitude.value = savedLat
        currentLongitude.value = savedLon
        selectedCity.value = PrayerTimesCalculator.CityInfo(savedCityName, "Mecca", savedLat, savedLon, TimeZone.getDefault().id)

        recalculatePrayerTimes()
        scheduleAzkarNotifications()
    }

    fun recalculatePrayerTimes() {
        val lat = currentLatitude.value
        val lon = currentLongitude.value
        val timeZone = TimeZone.getDefault()
        val offsetHours = timeZone.rawOffset / 3600000.0 + (if (timeZone.inDaylightTime(java.util.Date())) 1.0 else 0.0)
        
        val cal = Calendar.getInstance()
        val times = PrayerTimesCalculator.calculateTimes(
            latitude = lat,
            longitude = lon,
            timezoneOffset = offsetHours,
            calendar = cal,
            method = PrayerTimesCalculator.CalculationMethod.UMM_AL_QURA
        )
        prayerTimes.value = times
        scheduleAthanAlarms()
    }

    fun scheduleAthanAlarms() {
        val times = prayerTimes.value ?: return
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val prayerList = listOf(
            "الفجر" to times.rawFajrMs,
            "الظهر" to times.rawDhuhrMs,
            "العصر" to times.rawAsrMs,
            "المغرب" to times.rawMaghribMs,
            "العشاء" to times.rawIshaMs
        )

        val cityName = if (isLocationFetched.value) fetchedLocationName.value else selectedCity.value.nameAr

        val sharedPrefs = context.getSharedPreferences("athan_settings", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putBoolean("is_enabled", isAthanEnabled.value)
            .putBoolean("is_alert_only", isAlertOnly.value)
            .putString("selected_sound", selectedAthanSound.value)
            .putFloat("volume", athanVolume.value)
            .apply()

        val now = System.currentTimeMillis()

        prayerList.forEachIndexed { index, (name, timeMs) ->
            val intent = Intent(context, AthanReceiver::class.java).apply {
                putExtra(AthanService.EXTRA_PRAYER_NAME, name)
                putExtra(AthanService.EXTRA_CITY_NAME, cityName)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)

            if (timeMs > now && isAthanEnabled.value) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeMs, pendingIntent)
                }
            }
        }
    }

    fun scheduleAzkarNotifications() {
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val notificationConfigs = listOf(
            Triple("morning", morningNotificationTime.value, isMorningNotificationEnabled.value),
            Triple("evening", eveningNotificationTime.value, isEveningNotificationEnabled.value),
            Triple("sleep", sleepNotificationTime.value, isSleepNotificationEnabled.value)
        )

        notificationConfigs.forEachIndexed { index, (type, timeStr, isEnabled) ->
            val intent = Intent(context, AzkarNotificationReceiver::class.java).apply {
                putExtra(AzkarNotificationReceiver.EXTRA_AZKAR_TYPE, type)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                100 + index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)

            if (isEnabled) {
                val parts = timeStr.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toIntOrNull() ?: 0
                    val minute = parts[1].toIntOrNull() ?: 0

                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    if (calendar.timeInMillis < System.currentTimeMillis()) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                }
            }
        }
    }

    fun fetchDeviceLocation() {
        try {
            val locationManager = getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager == null) {
                recalculatePrayerTimes()
                return
            }

            val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            var location: Location? = null
            if (hasNetwork) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
            if (location == null && hasGps) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }

            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude
                currentLatitude.value = lat
                currentLongitude.value = lon
                isLocationFetched.value = true
                savePreference("is_location_fetched", true)
                savePreference("selected_city_lat", lat.toFloat())
                savePreference("selected_city_lon", lon.toFloat())
                fetchedLocationName.value = "موقعي الحالي"
                recalculatePrayerTimes()
                reverseGeocode(lat, lon)
            } else {
                val provider = if (hasNetwork) LocationManager.NETWORK_PROVIDER else LocationManager.GPS_PROVIDER
                locationManager.requestSingleUpdate(provider, object : LocationListener {
                    override fun onLocationChanged(loc: Location) {
                        val lat = loc.latitude
                        val lon = loc.longitude
                        currentLatitude.value = lat
                        currentLongitude.value = lon
                        isLocationFetched.value = true
                        savePreference("is_location_fetched", true)
                        savePreference("selected_city_lat", lat.toFloat())
                        savePreference("selected_city_lon", lon.toFloat())
                        fetchedLocationName.value = "موقعي الحالي"
                        recalculatePrayerTimes()
                        reverseGeocode(lat, lon)
                    }
                    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
                    override fun onProviderEnabled(p0: String) {}
                    override fun onProviderDisabled(p0: String) {}
                }, null)
            }
        } catch (e: SecurityException) {
            Log.e("AtharViewModel", "Location permissions not granted", e)
            recalculatePrayerTimes()
        } catch (e: Exception) {
            Log.e("AtharViewModel", "Failed to get location", e)
            recalculatePrayerTimes()
        }
    }

    private fun reverseGeocode(lat: Double, lon: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            var locationName = "موقعي الحالي"
            // 1. Try Android Geocoder first
            try {
                val geocoder = android.location.Geocoder(getApplication(), java.util.Locale("ar"))
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val address = addresses?.firstOrNull()
                if (address != null) {
                    val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: ""
                    val country = address.countryName ?: ""
                    locationName = if (city.isNotEmpty() && country.isNotEmpty()) "$city، $country" else if (city.isNotEmpty()) city else country
                }
            } catch (e: Exception) {
                Log.e("AtharViewModel", "Native Geocoder failed, trying online", e)
            }
            
            // 2. Fallback to online Nominatim reverse geocode if native returned default or failed
            if (locationName == "موقعي الحالي") {
                try {
                    val urlString = "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json&accept-language=ar"
                    val url = java.net.URL(urlString)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", "AtharApp/1.0")
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    if (connection.responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val obj = org.json.JSONObject(response)
                        val addressObj = obj.optJSONObject("address")
                        if (addressObj != null) {
                            val city = addressObj.optString("city", addressObj.optString("town", addressObj.optString("village", addressObj.optString("suburb", ""))))
                            val country = addressObj.optString("country", "")
                            locationName = if (city.isNotEmpty() && country.isNotEmpty()) "$city، $country" else if (city.isNotEmpty()) city else country
                        } else {
                            val disp = obj.optString("display_name", "")
                            if (disp.isNotEmpty()) {
                                val parts = disp.split(",")
                                locationName = parts.firstOrNull()?.trim() ?: "موقعي الحالي"
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AtharViewModel", "Online reverse geocoding failed", e)
                }
            }
            
            withContext(Dispatchers.Main) {
                fetchedLocationName.value = locationName
                savePreference("fetched_location_name", locationName)
            }
        }
    }

    fun searchCityOnline(query: String) {
        if (query.isBlank()) {
            searchResults.value = emptyList()
            return
        }
        isSearching.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val urlString = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=10&accept-language=ar"
                val url = java.net.URL(urlString)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "AtharApp/1.0")
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = org.json.JSONArray(response)
                    val list = mutableListOf<PrayerTimesCalculator.CityInfo>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val displayName = obj.getString("display_name")
                        val lat = obj.getDouble("lat")
                        val lon = obj.getDouble("lon")
                        
                        val nameParts = displayName.split(",")
                        val cityAr = nameParts.firstOrNull()?.trim() ?: "مدينة"
                        val countryAr = nameParts.lastOrNull()?.trim() ?: ""
                        val nameAr = if (countryAr.isNotEmpty() && nameParts.size > 1) "$cityAr، $countryAr" else cityAr
                        
                        list.add(PrayerTimesCalculator.CityInfo(
                            nameAr = nameAr,
                            nameEn = cityAr,
                            latitude = lat,
                            longitude = lon,
                            timezoneId = java.util.TimeZone.getDefault().id
                        ))
                    }
                    searchResults.value = list
                }
            } catch (e: Exception) {
                Log.e("AtharViewModel", "Search geocoding failed", e)
            } finally {
                isSearching.value = false
            }
        }
    }

    fun setAthanEnabled(enabled: Boolean) {
        isAthanEnabled.value = enabled
        savePreference("athan_enabled", enabled)
        recalculatePrayerTimes()
    }

    fun setAthanAlertOnly(enabled: Boolean) {
        isAlertOnly.value = enabled
        savePreference("athan_alert_only", enabled)
        recalculatePrayerTimes()
    }

    fun setAthanSound(sound: String) {
        selectedAthanSound.value = sound
        savePreference("athan_sound", sound)
        recalculatePrayerTimes()
    }

    fun setAthanVolume(vol: Float) {
        athanVolume.value = vol
        savePreference("athan_volume", vol)
        recalculatePrayerTimes()
    }

    fun setMorningEnabled(enabled: Boolean) {
        isMorningNotificationEnabled.value = enabled
        savePreference("morning_enabled", enabled)
        scheduleAzkarNotifications()
    }

    fun setEveningEnabled(enabled: Boolean) {
        isEveningNotificationEnabled.value = enabled
        savePreference("evening_enabled", enabled)
        scheduleAzkarNotifications()
    }

    fun setSleepEnabled(enabled: Boolean) {
        isSleepNotificationEnabled.value = enabled
        savePreference("sleep_enabled", enabled)
        scheduleAzkarNotifications()
    }

    fun setMorningTime(time: String) {
        morningNotificationTime.value = time
        savePreference("morning_time", time)
        scheduleAzkarNotifications()
    }

    fun setEveningTime(time: String) {
        eveningNotificationTime.value = time
        savePreference("evening_time", time)
        scheduleAzkarNotifications()
    }

    fun setSleepTime(time: String) {
        sleepNotificationTime.value = time
        savePreference("sleep_time", time)
        scheduleAzkarNotifications()
    }

    fun selectCity(city: PrayerTimesCalculator.CityInfo) {
        selectedCity.value = city
        currentLatitude.value = city.latitude
        currentLongitude.value = city.longitude
        isLocationFetched.value = false
        savePreference("selected_city_name", city.nameAr)
        savePreference("selected_city_lat", city.latitude.toFloat())
        savePreference("selected_city_lon", city.longitude.toFloat())
        savePreference("is_location_fetched", false)
        recalculatePrayerTimes()
    }

    private fun savePreference(key: String, value: Boolean) {
        getApplication<Application>().getSharedPreferences("athar_settings_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean(key, value).apply()
    }

    private fun savePreference(key: String, value: String) {
        getApplication<Application>().getSharedPreferences("athar_settings_prefs", Context.MODE_PRIVATE)
            .edit().putString(key, value).apply()
    }

    private fun savePreference(key: String, value: Float) {
        getApplication<Application>().getSharedPreferences("athar_settings_prefs", Context.MODE_PRIVATE)
            .edit().putFloat(key, value).apply()
    }

    private fun loadQuranOutline() {
        viewModelScope.launch {
            _quranOutline.value = repository.getQuranOutline()
        }
    }

    private fun loadAzkar() {
        viewModelScope.launch {
            // 1. Load local / cached immediately
            var azkarList = repository.getAzkar()
            _allThikrs.value = azkarList
            
            // Initialize counts
            val currentCounts = _thikrCounts.value
            val counts = azkarList.associate { item ->
                item.id to (currentCounts[item.id] ?: item.count)
            }
            _thikrCounts.value = counts

            if (azkarList.isNotEmpty() && _selectedCategory.value == null) {
                _selectedCategory.value = getCategories().firstOrNull()
            }

            // 2. Silently fetch online in the background and update
            val updated = repository.downloadAndUpdateAzkar()
            if (updated) {
                val updatedList = repository.getAzkar()
                _allThikrs.value = updatedList
                
                // Preserve user counts for unchanged IDs
                val freshCounts = _thikrCounts.value
                val updatedCounts = updatedList.associate { item ->
                    item.id to (freshCounts[item.id] ?: item.count)
                }
                _thikrCounts.value = updatedCounts
                
                if (updatedList.isNotEmpty() && (_selectedCategory.value == null || _selectedCategory.value !in getCategories())) {
                    _selectedCategory.value = getCategories().firstOrNull()
                }
            }
        }
    }

    fun getCategories(): List<String> {
        val originalCategories = _allThikrs.value.map { it.category }.distinct()
        val customOrder = customCategoryOrder.value
        if (customOrder.isEmpty()) return originalCategories

        // Sort based on customOrder, appending any missing categories at the end
        val sorted = originalCategories.filter { it in customOrder }.sortedBy { customOrder.indexOf(it) }
        val remaining = originalCategories.filter { it !in customOrder }
        return sorted + remaining
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setReorderingCategories(active: Boolean) {
        _isReorderingCategories.value = active
    }

    fun moveCategory(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentCategories = getCategories().toMutableList()
            if (fromIndex in currentCategories.indices && toIndex in currentCategories.indices) {
                val item = currentCategories.removeAt(fromIndex)
                currentCategories.add(toIndex, item)
                repository.saveCustomCategoryOrder(currentCategories)
            }
        }
    }

    fun resetCategoryOrder() {
        viewModelScope.launch {
            repository.clearCustomCategoryOrder()
        }
    }

    fun toggleFavorite(thikrId: Int) {
        viewModelScope.launch {
            if (favorites.value.contains(thikrId)) {
                repository.removeFavorite(thikrId)
            } else {
                repository.addFavorite(thikrId)
            }
        }
    }

    fun decrementThikrCount(thikrId: Int) {
        val currentCounts = _thikrCounts.value.toMutableMap()
        val currentCount = currentCounts[thikrId] ?: return
        if (currentCount > 0) {
            val nextCount = currentCount - 1
            currentCounts[thikrId] = nextCount
            _thikrCounts.value = currentCounts
            vibrateDevice(50) // Short vibrate on tap

            if (nextCount == 0) {
                vibrateDevice(200) // Long vibrate on completion
            }
        }
    }

    fun resetThikrCount(thikrId: Int) {
        val originalCount = _allThikrs.value.find { it.id == thikrId }?.count ?: return
        val currentCounts = _thikrCounts.value.toMutableMap()
        currentCounts[thikrId] = originalCount
        _thikrCounts.value = currentCounts
    }

    fun resetSelectedCategoryCounts() {
        val category = _selectedCategory.value ?: return
        val categoryThikrs = _allThikrs.value.filter { it.category == category }
        val currentCounts = _thikrCounts.value.toMutableMap()
        for (thikr in categoryThikrs) {
            currentCounts[thikr.id] = thikr.count
        }
        _thikrCounts.value = currentCounts
    }

    private fun vibrateDevice(ms: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(ms)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 4. Quran States & Downloader
    val downloadState: StateFlow<DownloadState> = repository.downloadState

    fun isQuranDownloaded(): Boolean = repository.isQuranDownloaded()

    fun triggerQuranDownload() {
        viewModelScope.launch {
            repository.downloadQuranPdf()
        }
    }

    fun deleteQuranPdf() {
        viewModelScope.launch {
            repository.deleteQuranPdf()
        }
    }

    // PDF Page rendering state
    private val _quranPageBitmap = MutableStateFlow<Bitmap?>(null)
    val quranPageBitmap: StateFlow<Bitmap?> = _quranPageBitmap.asStateFlow()

    private val _quranTotalPages = MutableStateFlow(604) // Standard Mushaf has 604 pages
    val quranTotalPages: StateFlow<Int> = _quranTotalPages.asStateFlow()

    private var currentPdfRenderer: PdfRenderer? = null
    private var currentParcelFd: ParcelFileDescriptor? = null

    private val pdfMutex = Mutex()

    suspend fun renderPageDirectly(pageIndex0: Int, screenWidthPx: Int): Bitmap? = pdfMutex.withLock {
        withContext(Dispatchers.IO) {
            if (!isQuranDownloaded()) return@withContext null
            try {
                if (currentPdfRenderer == null) {
                    val file = repository.quranFile
                    val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    currentParcelFd = fd
                    val renderer = PdfRenderer(fd)
                    currentPdfRenderer = renderer
                    _quranTotalPages.value = renderer.pageCount
                }
                val renderer = currentPdfRenderer ?: return@withContext null
                if (pageIndex0 in 0 until renderer.pageCount) {
                    val page = renderer.openPage(pageIndex0)
                    val targetWidth = if (screenWidthPx > 0) screenWidthPx else 1080
                    val scale = targetWidth.toFloat() / page.width.toFloat()
                    val targetHeight = (page.height * scale).toInt()
                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmap
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun renderQuranPage(pageIndex0: Int, screenWidthPx: Int) = pdfMutex.withLock {
        withContext(Dispatchers.IO) {
            if (!isQuranDownloaded()) return@withContext

            try {
                if (currentPdfRenderer == null) {
                    val file = repository.quranFile
                    val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    currentParcelFd = fd
                    val renderer = PdfRenderer(fd)
                    currentPdfRenderer = renderer
                    _quranTotalPages.value = renderer.pageCount
                }

                val renderer = currentPdfRenderer ?: return@withContext
                if (pageIndex0 in 0 until renderer.pageCount) {
                    val page = renderer.openPage(pageIndex0)

                    // High quality responsive scaling
                    val targetWidth = if (screenWidthPx > 0) screenWidthPx else 1080
                    val scale = targetWidth.toFloat() / page.width.toFloat()
                    val targetHeight = (page.height * scale).toInt()

                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    _quranPageBitmap.value = bitmap
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _quranPageBitmap.value = null
            }
        }
    }

    fun closePdfRenderer() {
        try {
            currentPdfRenderer?.close()
            currentParcelFd?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            currentPdfRenderer = null
            currentParcelFd = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        closePdfRenderer()
    }

    // 5. Hijri Date Generation Logic
    // We implement a complete, zero-dependency calculation for the Hijri Month calendar
    fun getHijriOccasions(): Map<Pair<Int, Int>, String> {
        return mapOf(
            Pair(1, 1) to "رأس السنة الهجرية الجديدة",
            Pair(1, 10) to "يوم عاشوراء المبارك",
            Pair(3, 12) to "المولد النبوي الشريف",
            Pair(7, 27) to "ليلة الإسراء والمعراج",
            Pair(9, 1) to "بداية شهر رمضان المبارك",
            Pair(10, 1) to "عيد الفطر السعيد",
            Pair(12, 9) to "يوم عرفة (وقفة العيد)",
            Pair(12, 10) to "عيد الأضحى المبارك"
        )
    }

    // Returns adjusted HijrahDate based on the current system date and offset
    fun getAdjustedHijrahDate(): HijrahDate {
        val offsetDays = hijriOffset.value
        val today = HijrahDate.now()
        return if (offsetDays == 0) {
            today
        } else {
            today.plus(offsetDays.toLong(), ChronoUnit.DAYS)
        }
    }
}
