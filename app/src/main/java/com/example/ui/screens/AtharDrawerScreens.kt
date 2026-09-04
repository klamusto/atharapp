package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.selection.selectable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.R
import com.example.data.*
import com.example.features.quran.domain.*
import com.example.features.quran.presentation.SurahPlaybackManager
import com.example.features.quran.presentation.RepeatMode
import com.example.features.quran.presentation.Reciter
import com.example.features.quran.presentation.RECITERS_LIST
import com.example.features.quran.presentation.QuranAudioService
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IslamicGreenPrimary
import com.example.ui.theme.IslamicGreenSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.*

// ==========================================
// 1. PRAYER TIMES SCREEN
// ==========================================
@Composable
fun PrayerTimesScreen(viewModel: AtharViewModel) {
    val context = LocalContext.current
    val prayerTimes by viewModel.prayerTimes.collectAsState()
    val isLocationFetched by viewModel.isLocationFetched.collectAsState()
    val fetchedLocationName by viewModel.fetchedLocationName.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()
    
    var showCityDialog by remember { mutableStateOf(false) }
    var countdownText by remember { mutableStateOf("جاري الحساب...") }
    var nextPrayerName by remember { mutableStateOf("") }
    
    val cityName = if (isLocationFetched) fetchedLocationName else selectedCity.nameAr

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.fetchDeviceLocation()
        }
    }

    // Trigger permission request upon first launch
    LaunchedEffect(Unit) {
        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (!hasFine && !hasCoarse) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else if (!isLocationFetched) {
            viewModel.fetchDeviceLocation()
        }
    }

    // Timer effect for countdown
    LaunchedEffect(prayerTimes) {
        while (true) {
            val times = prayerTimes
            if (times != null) {
                val now = System.currentTimeMillis()
                val prayerList = listOf(
                    "الفجر" to times.rawFajrMs,
                    "الشروق" to times.rawSunriseMs,
                    "الظهر" to times.rawDhuhrMs,
                    "العصر" to times.rawAsrMs,
                    "المغرب" to times.rawMaghribMs,
                    "العشاء" to times.rawIshaMs
                )
                
                // Find next prayer
                val next = prayerList.firstOrNull { it.second > now } ?: Pair("الفجر", times.rawFajrMs + 24 * 3600000)
                nextPrayerName = next.first
                
                val diff = next.second - now
                val hours = (diff / 3600000) % 24
                val minutes = (diff / 60000) % 60
                val seconds = (diff / 1000) % 60
                countdownText = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            }
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Location Selector Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.LocationOn, contentDescription = "الموقع", tint = IslamicGreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(cityName, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (isLocationFetched) "تم التحديد عبر GPS" else "مدينة مخصصة",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Row {
                    IconButton(onClick = {
                        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (hasFine || hasCoarse) {
                            viewModel.fetchDeviceLocation()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "تحديد موقعي", tint = IslamicGreenPrimary)
                    }
                    IconButton(onClick = { showCityDialog = true }) {
                        Icon(Icons.Default.Search, contentDescription = "ابحث عن مدينة", tint = IslamicGreenPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Countdown Immersive Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(IslamicGreenPrimary, Color(0xFF072416))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text("المتبقي لأذان $nextPrayerName", color = GoldAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = countdownText,
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("بتوقيت $cityName", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("مواقيت الصلاة اليوم", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
        Spacer(modifier = Modifier.height(12.dp))

        // Grid of Prayer Times
        prayerTimes?.let { times ->
            val nowStr = SimpleDateFormat("HH:mm", Locale.US).format(Date())
            
            val itemsList = listOf(
                PrayerTimeRowItem("الفجر", times.fajr, Icons.Default.Brightness3, isCurrentPrayer("الفجر", times, nowStr)),
                PrayerTimeRowItem("الشروق", times.sunrise, Icons.Default.WbTwilight, false),
                PrayerTimeRowItem("الظهر", times.dhuhr, Icons.Default.WbSunny, isCurrentPrayer("الظهر", times, nowStr)),
                PrayerTimeRowItem("العصر", times.asr, Icons.Default.AccessTime, isCurrentPrayer("العصر", times, nowStr)),
                PrayerTimeRowItem("المغرب", times.maghrib, Icons.Default.WbCloudy, isCurrentPrayer("المغرب", times, nowStr)),
                PrayerTimeRowItem("العشاء", times.isha, Icons.Default.NightsStay, isCurrentPrayer("العشاء", times, nowStr))
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsList.forEach { item ->
                    PrayerTimeCard(item)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // City Selection Dialog with Online Search (Geocoding)
    if (showCityDialog) {
        val searchResults by viewModel.searchResults.collectAsState()
        val isSearching by viewModel.isSearching.collectAsState()
        var searchQuery by remember { mutableStateOf("") }
        
        // Trigger search on query change
        LaunchedEffect(searchQuery) {
            delay(500) // Debounce search
            if (searchQuery.isNotEmpty()) {
                viewModel.searchCityOnline(searchQuery)
            }
        }

        Dialog(onDismissRequest = { showCityDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "البحث عن مدينة (عبر الإنترنت)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGreenPrimary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("اكتب المدينة (الجزائر، France Paris...)", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("city_search_input"),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "بحث", tint = IslamicGreenPrimary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color.Gray)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IslamicGreenPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Results list
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 250.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(color = IslamicGreenPrimary)
                        } else if (searchQuery.isEmpty()) {
                            Text(
                                "اكتب اسم أي مدينة في العالم للبحث عنها",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        } else if (searchResults.isEmpty()) {
                            Text(
                                "لا توجد نتائج، حاول كتابة الاسم بشكل صحيح (مثال: الجزائر تلمسان أو France Paris)",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(searchResults) { city ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.selectCity(city)
                                                showCityDialog = false
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = IslamicGreenPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = city.nameAr,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Right
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(
                        onClick = { showCityDialog = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("إغلاق", color = IslamicGreenPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

data class PrayerTimeRowItem(
    val name: String,
    val time: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isActive: Boolean
)

@Composable
fun PrayerTimeCard(item: PrayerTimeRowItem) {
    val cardColor = if (item.isActive) IslamicGreenPrimary else MaterialTheme.colorScheme.surface
    val textColor = if (item.isActive) Color.White else MaterialTheme.colorScheme.onSurface
    val iconTint = if (item.isActive) GoldAccent else IslamicGreenPrimary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (item.isActive) 2.dp else 0.dp,
                color = if (item.isActive) GoldAccent else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isActive) 8.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(item.icon, contentDescription = item.name, tint = iconTint, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(item.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatTimeTo12Hour(item.time),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

fun isCurrentPrayer(name: String, times: PrayerTimesCalculator.PrayerTimes, now: String): Boolean {
    // Basic detection of active prayer based on hour interval
    try {
        val parser = SimpleDateFormat("HH:mm", Locale.US)
        val timeNow = parser.parse(now) ?: return false
        val tFajr = parser.parse(times.fajr) ?: return false
        val tSunrise = parser.parse(times.sunrise) ?: return false
        val tDhuhr = parser.parse(times.dhuhr) ?: return false
        val tAsr = parser.parse(times.asr) ?: return false
        val tMaghrib = parser.parse(times.maghrib) ?: return false
        val tIsha = parser.parse(times.isha) ?: return false

        return when (name) {
            "الفجر" -> timeNow.after(tFajr) && timeNow.before(tSunrise)
            "الظهر" -> timeNow.after(tDhuhr) && timeNow.before(tAsr)
            "العصر" -> timeNow.after(tAsr) && timeNow.before(tMaghrib)
            "المغرب" -> timeNow.after(tMaghrib) && timeNow.before(tIsha)
            "العشاء" -> timeNow.after(tIsha) || timeNow.before(tFajr)
            else -> false
        }
    } catch (e: Exception) {
        return false
    }
}

fun formatTimeTo12Hour(time24: String): String {
    try {
        val parts = time24.split(":")
        if (parts.size == 2) {
            val h = parts[0].toInt()
            val m = parts[1]
            val suffix = if (h >= 12) "م" else "ص"
            val h12 = if (h % 12 == 0) 12 else h % 12
            return String.format("%02d:%s %s", h12, m, suffix)
        }
    } catch (e: Exception) {}
    return time24
}

// ==========================================
// 2. QIBLA SCREEN (COMPASS)
// ==========================================
@Composable
fun QiblaScreen(viewModel: AtharViewModel) {
    val context = LocalContext.current
    val currentLat by viewModel.currentLatitude.collectAsState()
    val currentLon by viewModel.currentLongitude.collectAsState()
    val isLocationFetched by viewModel.isLocationFetched.collectAsState()
    
    var azimuth by remember { mutableStateOf(0f) }
    var qiblaAngle by remember { mutableStateOf(135f) } // default angle for mecca from north in ME
    var hasSensors by remember { mutableStateOf(true) }
    var manualRotation by remember { mutableStateOf(0f) }
    var centerPoint by remember { mutableStateOf(Offset.Zero) }

    // Mathematically calculate precise Qibla bearing
    // Mecca: Lat 21.4225, Lon 39.8262
    LaunchedEffect(currentLat, currentLon) {
        val phi = currentLat * Math.PI / 180.0
        val lambda = currentLon * Math.PI / 180.0
        val phiM = 21.4225 * Math.PI / 180.0
        val lambdaM = 39.8262 * Math.PI / 180.0
        val deltaLambda = lambdaM - lambda
        
        val numerator = Math.sin(deltaLambda)
        val denominator = Math.cos(phi) * Math.tan(phiM) - Math.sin(phi) * Math.cos(deltaLambda)
        val bearingRad = Math.atan2(numerator, denominator)
        var bearingDeg = bearingRad * 180.0 / Math.PI
        if (bearingDeg < 0) bearingDeg += 360.0
        qiblaAngle = bearingDeg.toFloat()
    }

    // Register Sensors for Real Rotation
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        var accelerometer: Sensor? = null
        var magnetometer: Sensor? = null
        
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        }

        hasSensors = (accelerometer != null && magnetometer != null)

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasGeomagnetic = false

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, gravity, 0, event.values.size)
                    hasGravity = true
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, geomagnetic, 0, event.values.size)
                    hasGeomagnetic = true
                }

                if (hasGravity && hasGeomagnetic) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(r, orientation)
                        val azimuthRad = orientation[0]
                        var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                        azimuthDeg = (azimuthDeg + 360) % 360
                        azimuth = azimuthDeg
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (hasSensors && sensorManager != null) {
            sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(sensorListener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            if (hasSensors) {
                sensorManager?.unregisterListener(sensorListener)
            }
        }
    }

    val effectiveAzimuth = if (hasSensors) azimuth else manualRotation

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("بوصلة اتجاه القبلة", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = IslamicGreenPrimary)
        
        Text(
            text = if (hasSensors) "قم بوضع الهاتف مستوياً لتحديد أدق لاتجاه الكعبة المشرفة" else "جهازك لا يدعم بوصلة المستشعرات التلقائية، يمكنك توجيه البوصلة يدوياً باللمس",
            fontSize = 12.sp,
            color = if (hasSensors) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) else GoldAccent,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Compass graphic container
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .border(4.dp, if (hasSensors) IslamicGreenPrimary else GoldAccent, CircleShape)
                .onGloballyPositioned { layoutCoordinates ->
                    val size = layoutCoordinates.size
                    centerPoint = Offset(size.width / 2f, size.height / 2f)
                }
                .pointerInput(hasSensors) {
                    if (!hasSensors) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val position = change.position
                            val angleRad = Math.atan2((position.y - centerPoint.y).toDouble(), (position.x - centerPoint.x).toDouble())
                            var angleDeg = Math.toDegrees(angleRad).toFloat()
                            angleDeg = (angleDeg + 360) % 360
                            manualRotation = angleDeg
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Rotating Compass Card (rotates in opposite direction of azimuth)
            val rotateAnim by animateFloatAsState(targetValue = -effectiveAzimuth, label = "compass_rotation")
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotateAnim),
                contentAlignment = Alignment.Center
            ) {
                // North-East-West-South Marks
                Text("N", modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp), fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 18.sp)
                Text("S", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp), fontWeight = FontWeight.Bold, color = IslamicGreenPrimary, fontSize = 16.sp)
                Text("E", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp), fontWeight = FontWeight.Bold, color = IslamicGreenPrimary, fontSize = 16.sp)
                Text("W", modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp), fontWeight = FontWeight.Bold, color = IslamicGreenPrimary, fontSize = 16.sp)
                
                // Fine compass ticks drawing (using simple canvas or line representations)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2
                    val center = Offset(size.width / 2f, size.height / 2f)
                    // Draw degrees marks
                    for (angle in 0 until 360 step 30) {
                        val angleRad = angle * Math.PI / 180.0
                        val startX = center.x + (radius - 15) * Math.cos(angleRad).toFloat()
                        val startY = center.y + (radius - 15) * Math.sin(angleRad).toFloat()
                        val endX = center.x + radius * Math.cos(angleRad).toFloat()
                        val endY = center.y + radius * Math.sin(angleRad).toFloat()
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }

                // Golden Qibla Angle Indicator line/logo in the compass card!
                // Rotates based on Mecca bearing (qiblaAngle)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(qiblaAngle),
                    contentAlignment = Alignment.Center
                ) {
                    // Golden Kaaba Indicator
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 35.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.LocationCity,
                            contentDescription = "الكعبة المشرفة",
                            tint = GoldAccent,
                            modifier = Modifier.size(36.dp)
                        )
                        Text("القبلة", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Fixed phone pointer/needle (stays static pointing up)
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Elegant Central Gold Pin
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(GoldAccent)
                )
                
                // Static arrow pointing straight up to indicate phone direction
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(80.dp)
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                        .background(GoldAccent)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Warning or Guidance card if sensors are missing
        if (!hasSensors) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "تنبيه المستشعرات",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "جهازك لا يحتوي على مستشعر المغناطيسية (Magnetometer) لتدوير البوصلة تلقائياً. يمكنك لمس وتدوير البوصلة بإصبعك لمطابقة اتجاه الشمال ومؤشر القبلة يدوياً.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Right
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Qibla Degrees card
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "اتجاه القبلة: ${qiblaAngle.toInt()}° من الشمال",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = IslamicGreenPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (hasSensors) "انحراف الهاتف الحالي: ${effectiveAzimuth.toInt()}°" else "توجيه البوصلة اليدوي الحالي: ${effectiveAzimuth.toInt()}°",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ==========================================
// 3. TASBIH/SEBHA SCREEN
// ==========================================
@Composable
fun TasbihScreen(viewModel: AtharViewModel) {
    val context = LocalContext.current
    var count by remember { mutableStateOf(0) }
    var totalCount by remember { mutableStateOf(0) }
    var selectedDhikrIndex by remember { mutableStateOf(0) }
    
    val dhikrs = listOf(
        "سبحان الله",
        "الحمد لله",
        "لا إله إلا الله",
        "الله أكبر",
        "أستغفر الله العظيم",
        "سبحان الله وبحمده، سبحان الله العظيم"
    )

    // Trigger haptic vibration feedback
    fun triggerVibration() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("السبحة الإلكترونية", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = IslamicGreenPrimary)
        Spacer(modifier = Modifier.height(16.dp))

        // Dhikr selection dropdown/button
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    selectedDhikrIndex = (selectedDhikrIndex + 1) % dhikrs.size
                    count = 0 // Reset sub-counter on dhikr switch
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "التالي", tint = IslamicGreenPrimary, modifier = Modifier.size(16.dp).rotate(180f))
                Text(
                    text = dhikrs[selectedDhikrIndex],
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = IslamicGreenPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "السابق", tint = IslamicGreenPrimary, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Interactive Circular Bead
        val scale by animateFloatAsState(targetValue = if (count > 0) 1.05f else 1f, animationSpec = spring(stiffness = Spring.StiffnessMedium), label = "pulse")
        
        Box(
            modifier = Modifier
                .size(240.dp)
                .rotate(count * 5f) // rotating animation feedback!
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(IslamicGreenPrimary, Color(0xFF072416))
                    )
                )
                .clickable {
                    count++
                    totalCount++
                    triggerVibration()
                },
            contentAlignment = Alignment.Center
        ) {
            // Radial counter ticks
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = size / 2.0f
                val radius = size.minDimension / 2
                // Draw dynamic rings based on progress (up to 33)
                val sweep = (count % 33) / 33.0f
                drawCircle(
                    color = GoldAccent.copy(alpha = 0.3f),
                    radius = radius - 15,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6.dp.toPx())
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = count.toString(), color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                Text(text = "دورات: ${totalCount / 33}", color = GoldAccent, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action Buttons Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    count = 0
                    totalCount = 0
                    triggerVibration()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "تصفير")
                Spacer(modifier = Modifier.width(4.dp))
                Text("تصفير العداد")
            }

            Button(
                onClick = {
                    count = 0
                    triggerVibration()
                },
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary)
            ) {
                Text("دورة جديدة (33)")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("إجمالي التسبيحات اليوم: $totalCount", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
    }
}

// ==========================================
// 4. SPOTIFY STYLE AUDIO PLAYER SCREEN
// ==========================================
@Composable
fun AudioPlayerScreen(viewModel: AtharViewModel) {
    val context = LocalContext.current
    val currentSurah by SurahPlaybackManager.currentSurah.collectAsState()
    val isPlaying by SurahPlaybackManager.isPlaying.collectAsState()
    val isBuffering by SurahPlaybackManager.isBuffering.collectAsState()
    val currentDuration by SurahPlaybackManager.currentDuration.collectAsState()
    val currentPosition by SurahPlaybackManager.currentPosition.collectAsState()
    
    val currentReciter by SurahPlaybackManager.currentReciter.collectAsState()
    val playbackSpeed by SurahPlaybackManager.playbackSpeed.collectAsState()
    val repeatMode by SurahPlaybackManager.repeatMode.collectAsState()
    val isShuffled by SurahPlaybackManager.isShuffled.collectAsState()
    val downloadProgress by SurahPlaybackManager.downloadProgress.collectAsState()
    val downloadedSurahs by SurahPlaybackManager.downloadedSurahs.collectAsState()
    
    var showReciterDialog by remember { mutableStateOf(false) }
    
    // Local SharedPreferences for Favorite Surahs
    val prefs = remember { context.getSharedPreferences("athar_audio_prefs", Context.MODE_PRIVATE) }
    var favoriteSurahs by remember { 
        mutableStateOf(
            prefs.getStringSet("favorite_surah_numbers", emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        ) 
    }
    
    val isCurrentSurahFavorite = currentSurah?.number?.let { favoriteSurahs.contains(it) } ?: false
    val isCurrentSurahDownloaded = currentSurah?.number?.let { downloadedSurahs.contains(it) } ?: false
    val currentSurahDownloadPct = currentSurah?.number?.let { downloadProgress[it] }

    val formattedElapsed = formatMillis(currentPosition)
    val formattedTotal = formatMillis(currentDuration)
    val progress = if (currentDuration > 0) currentPosition.toFloat() / currentDuration.toFloat() else 0f

    // Infinite spinning transition for the Artwork Disk
    val infiniteTransition = rememberInfiniteTransition(label = "emblem_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "emblem_spin_anim"
    )
    val currentRotationAngle = if (isPlaying) spinAngle else 0f

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F3220), // deep emerald green
                        Color(0xFF081C12), // dark velvet
                        Color(0xFF0F1110)  // charcoal dark
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Player Header / Reciter Selector Trigger
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("القرآن الكريم صوتي", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldAccent, letterSpacing = 1.sp)
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Card(
                onClick = { showReciterDialog = true },
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = "القارئ", tint = GoldAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "الشيخ ${currentReciter.nameArabic}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "تغيير القارئ", tint = GoldAccent)
                }
            }
        }

        // Beautiful Graphic Card Artwork (Spinning vinyl style Surah Emblem)
        Box(
            modifier = Modifier
                .size(260.dp)
                .shadow(24.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1E5236), Color(0xFF071910))
                    )
                )
                .border(6.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                .rotate(currentRotationAngle),
            contentAlignment = Alignment.Center
        ) {
            // Rotating Calligraphy lines and mandala
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .border(2.dp, GoldAccent.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = currentSurah?.name ?: "القرآن الكريم",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentSurah?.englishName ?: "Holy Quran",
                        color = GoldAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Dynamic Title, Favorites and Download Row
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Heart Favorite button
                IconButton(
                    onClick = {
                        currentSurah?.number?.let { num ->
                            val updated = if (favoriteSurahs.contains(num)) favoriteSurahs - num else favoriteSurahs + num
                            favoriteSurahs = updated
                            prefs.edit().putStringSet("favorite_surah_numbers", updated.map { it.toString() }.toSet()).apply()
                            Toast.makeText(
                                context, 
                                if (updated.contains(num)) "تمت الإضافة للمفضلة" else "تمت الإزالة من المفضلة", 
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = currentSurah != null
                ) {
                    Icon(
                        imageVector = if (isCurrentSurahFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "المفضلة",
                        tint = if (isCurrentSurahFavorite) Color.Red else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Core Surah Details
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = currentSurah?.name ?: "لم يتم اختيار سورة بعد",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (currentSurah != null) "سورة رقم ${currentSurah?.number} • ${currentSurah?.revelationType}" else "اضغط تشغيل للبدء أو تصفح السور",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Offline Download status button
                IconButton(
                    onClick = {
                        currentSurah?.number?.let { num ->
                            if (isCurrentSurahDownloaded) {
                                SurahPlaybackManager.deleteDownload(context, num, currentReciter.id)
                                Toast.makeText(context, "تم حذف الملف المحمل", Toast.LENGTH_SHORT).show()
                            } else {
                                SurahPlaybackManager.startDownload(context, num, currentReciter.id, coroutineScope)
                                Toast.makeText(context, "بدأ تحميل السورة للاستماع دون إنترنت...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = currentSurah != null
                ) {
                    if (currentSurahDownloadPct != null) {
                        if (currentSurahDownloadPct == -1f) {
                            Icon(Icons.Default.Error, contentDescription = "خطأ في التحميل", tint = Color.Red)
                        } else {
                            CircularProgressIndicator(
                                progress = { currentSurahDownloadPct ?: 0f },
                                modifier = Modifier.size(24.dp),
                                color = GoldAccent,
                                strokeWidth = 2.dp,
                            )
                        }
                    } else if (isCurrentSurahDownloaded) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "محملة", tint = IslamicGreenPrimary, modifier = Modifier.size(28.dp))
                    } else {
                        Icon(Icons.Default.CloudDownload, contentDescription = "تحميل", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        // Seeker Bar Block
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = progress,
                onValueChange = { newProgress ->
                    if (currentDuration > 0) {
                        val seekTarget = (newProgress * currentDuration).toLong()
                        val intent = Intent(context, QuranAudioService::class.java).apply {
                            action = QuranAudioService.ACTION_SEEK_TO
                            putExtra(QuranAudioService.EXTRA_SEEK_POSITION, seekTarget)
                        }
                        context.startService(intent)
                    }
                },
                colors = SliderDefaults.colors(
                    activeTrackColor = GoldAccent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                    thumbColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formattedElapsed, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                Text(formattedTotal, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            }
        }

        // Spotify-style Player Media Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle Button
            IconButton(
                onClick = {
                    SurahPlaybackManager.isShuffled.value = !isShuffled
                }
            ) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "عشوائي",
                    tint = if (isShuffled) GoldAccent else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Backward Skip
            IconButton(
                onClick = {
                    val intent = Intent(context, QuranAudioService::class.java).apply {
                        action = QuranAudioService.ACTION_PREV_SURAH
                    }
                    context.startService(intent)
                },
                enabled = currentSurah != null
            ) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "السابق",
                    tint = if (currentSurah != null) Color.White else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(36.dp)
                )
            }

            // Big Central Play/Pause Circular Action
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable {
                        if (currentSurah == null) {
                            // Default play surah 1
                            val intent = Intent(context, QuranAudioService::class.java).apply {
                                action = QuranAudioService.ACTION_PLAY_SURAH
                                putExtra(QuranAudioService.EXTRA_SURAH_NUMBER, 1)
                            }
                            context.startService(intent)
                        } else {
                            val intent = Intent(context, QuranAudioService::class.java).apply {
                                action = if (isPlaying) QuranAudioService.ACTION_PAUSE else QuranAudioService.ACTION_RESUME
                            }
                            context.startService(intent)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isBuffering) {
                    CircularProgressIndicator(color = IslamicGreenPrimary, modifier = Modifier.size(32.dp))
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "تشغيل/إيقاف",
                        tint = Color(0xFF081C12),
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            // Forward Skip
            IconButton(
                onClick = {
                    val intent = Intent(context, QuranAudioService::class.java).apply {
                        action = QuranAudioService.ACTION_NEXT_SURAH
                    }
                    context.startService(intent)
                },
                enabled = currentSurah != null
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "التالي",
                    tint = if (currentSurah != null) Color.White else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(36.dp)
                )
            }

            // Repeat Mode Button
            IconButton(
                onClick = {
                    val nextMode = when (repeatMode) {
                        RepeatMode.OFF -> RepeatMode.ALL
                        RepeatMode.ALL -> RepeatMode.ONE
                        RepeatMode.ONE -> RepeatMode.OFF
                    }
                    SurahPlaybackManager.repeatMode.value = nextMode
                }
            ) {
                Icon(
                    imageVector = when (repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    },
                    contentDescription = "تكرار",
                    tint = if (repeatMode != RepeatMode.OFF) GoldAccent else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Bottom speed controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val speedLabels = listOf("0.75x", "1.0x", "1.25x", "1.5x", "2.0x")
            val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
            val currentSpeedIndex = speeds.indexOf(playbackSpeed).coerceAtLeast(1)
            
            TextButton(
                onClick = {
                    val nextIndex = (currentSpeedIndex + 1) % speeds.size
                    SurahPlaybackManager.playbackSpeed.value = speeds[nextIndex]
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.8f))
            ) {
                Icon(Icons.Default.Speed, contentDescription = "السرعة", tint = GoldAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("السرعة: ${speedLabels[currentSpeedIndex]}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            
            // Full Stop Button
            IconButton(
                onClick = {
                    val intent = Intent(context, QuranAudioService::class.java).apply {
                        action = QuranAudioService.ACTION_STOP
                    }
                    context.startService(intent)
                },
                enabled = currentSurah != null
            ) {
                Icon(Icons.Default.Stop, contentDescription = "إيقاف بالكامل", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
            }
        }
    }

    // Reciters Selector Bottom Sheet style Dialog
    if (showReciterDialog) {
        Dialog(onDismissRequest = { showReciterDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "اختر القارئ الافتراضي",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGreenPrimary,
                        modifier = Modifier.padding(bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )
                    
                    LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                        items(RECITERS_LIST) { reciter ->
                            val isSelected = currentReciter.id == reciter.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) IslamicGreenPrimary.copy(alpha = 0.08f) else Color.Transparent)
                                    .clickable {
                                        SurahPlaybackManager.saveReciter(context, reciter)
                                        showReciterDialog = false
                                        Toast.makeText(context, "القارئ الحالي: ${reciter.nameArabic}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Circular Initial Badge instead of complex images
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) IslamicGreenPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = reciter.nameArabic.take(1),
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column {
                                        Text(text = reciter.nameArabic, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isSelected) IslamicGreenPrimary else MaterialTheme.colorScheme.onSurface)
                                        Text(text = reciter.nameEnglish, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                }
                                
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "محدد", tint = IslamicGreenPrimary, modifier = Modifier.size(24.dp))
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(
                        onClick = { showReciterDialog = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("إلغاء", color = IslamicGreenPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun formatMillis(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / 60000) % 60
    val hrs = (ms / 3600000)
    return if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, min, sec)
    } else {
        String.format("%02d:%02d", min, sec)
    }
}

// ==========================================
// 5. DOWNLOADS SCREEN
// ==========================================
@Composable
fun DownloadsScreen(viewModel: AtharViewModel) {
    val context = LocalContext.current
    val downloadedSurahs by SurahPlaybackManager.downloadedSurahs.collectAsState()
    val currentReciter by SurahPlaybackManager.currentReciter.collectAsState()
    val allSurahs by viewModel.quranOutline.collectAsState()

    // Trigger update downloads list at open
    LaunchedEffect(currentReciter) {
        SurahPlaybackManager.updateDownloadedList(context, currentReciter.id)
    }

    val downloadedList = allSurahs.filter { downloadedSurahs.contains(it.order) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("الملفات المحملة للتشغيل دون إنترنت", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = IslamicGreenPrimary)
        Text("القارئ الحالي: الشيخ ${currentReciter.nameArabic}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        
        Spacer(modifier = Modifier.height(16.dp))

        if (downloadedList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("لا توجد سور محملة حالياً دون إنترنت", color = Color.Gray, fontSize = 16.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(downloadedList) { surah ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(IslamicGreenPrimary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(surah.order.toString(), fontWeight = FontWeight.Bold, color = IslamicGreenPrimary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(surah.surahName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("الصفحة: ${surah.pageNumber}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                            
                            Row {
                                IconButton(
                                    onClick = {
                                        val intent = Intent(context, QuranAudioService::class.java).apply {
                                            action = QuranAudioService.ACTION_PLAY_SURAH
                                            putExtra(QuranAudioService.EXTRA_SURAH_NUMBER, surah.order)
                                        }
                                        context.startService(intent)
                                        viewModel.selectTab(AtharTab.AUDIOPLAYER)
                                    }
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "تشغيل", tint = IslamicGreenPrimary)
                                }
                                
                                IconButton(
                                    onClick = {
                                        SurahPlaybackManager.deleteDownload(context, surah.order, currentReciter.id)
                                        Toast.makeText(context, "تم حذف التحميل للملف", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. FAVORITES SCREEN
// ==========================================
@Composable
fun FavoritesScreen(viewModel: AtharViewModel) {
    val favorites by viewModel.favorites.collectAsState()
    val allThikrs by viewModel.allThikrs.collectAsState()
    
    val favoriteThikrs = allThikrs.filter { favorites.contains(it.id) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("الأذكار المفضلة", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = IslamicGreenPrimary)
        Spacer(modifier = Modifier.height(16.dp))

        if (favoriteThikrs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("قائمتك المفضلة فارغة حالياً", color = Color.Gray, fontSize = 16.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(favoriteThikrs) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = item.text,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "التكرار المطلوب: ${item.count}",
                                    fontSize = 12.sp,
                                    color = IslamicGreenPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                IconButton(onClick = { viewModel.toggleFavorite(item.id) }) {
                                    Icon(Icons.Default.Favorite, contentDescription = "إزالة من المفضلة", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. SETTINGS SCREEN
// ==========================================
@Composable
fun SettingsScreen(viewModel: AtharViewModel) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val hijriOffset by viewModel.hijriOffset.collectAsState()
    
    val isAthanEnabled by viewModel.isAthanEnabled.collectAsState()
    val isAlertOnly by viewModel.isAlertOnly.collectAsState()
    val selectedAthanSound by viewModel.selectedAthanSound.collectAsState()
    val athanVolume by viewModel.athanVolume.collectAsState()

    val isMorningEnabled by viewModel.isMorningNotificationEnabled.collectAsState()
    val isEveningEnabled by viewModel.isEveningNotificationEnabled.collectAsState()
    val isSleepEnabled by viewModel.isSleepNotificationEnabled.collectAsState()
    
    val morningTime by viewModel.morningNotificationTime.collectAsState()
    val eveningTime by viewModel.eveningNotificationTime.collectAsState()
    val sleepTime by viewModel.sleepNotificationTime.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("الإعدادات والخيارات", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = IslamicGreenPrimary)
        Spacer(modifier = Modifier.height(16.dp))

        // Section 1: Themes & Look
        Text("المظهر العام", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("الوضع الداكن النشط", fontSize = 16.sp)
                Switch(checked = isDarkMode, onCheckedChange = { viewModel.setDarkMode(it) })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 2: Athan Settings
        Text("إشعارات وأذان الصلاة", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تفعيل تنبيهات الأذان", fontSize = 16.sp)
                    Switch(checked = isAthanEnabled, onCheckedChange = { viewModel.setAthanEnabled(it) })
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("صوت التنبيه فقط (دون أذان كامل)", fontSize = 16.sp)
                    Switch(checked = isAlertOnly, onCheckedChange = { viewModel.setAthanAlertOnly(it) })
                }

                if (!isAlertOnly) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Text("اختر صوت المؤذن للأذان:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val sounds = listOf("Muaiqly" to "الشيخ ماهر المعيقلي", "Makkah" to "أذان الحرم المكي الشريف", "Madinah" to "أذان الحرم المدني الشريف")
                    sounds.forEach { (id, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedAthanSound == id,
                                    onClick = { viewModel.setAthanSound(id) }
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedAthanSound == id, onClick = { viewModel.setAthanSound(id) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, fontSize = 14.sp)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text("درجة صوت الأذان والتنبيهات:", fontSize = 14.sp)
                Slider(
                    value = athanVolume,
                    onValueChange = { viewModel.setAthanVolume(it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(activeTrackColor = IslamicGreenPrimary)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 3: Azkar Daily Schedules
        Text("مواعيد تنبيهات الأذكار اليومية", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Morning
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("أذكار الصباح", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("موعد التنبيه: $morningTime", fontSize = 12.sp, color = Color.Gray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            // Cycle preset times for simplified testing
                            val times = listOf("05:00", "05:30", "06:00", "06:30", "07:00")
                            val nextIndex = (times.indexOf(morningTime) + 1) % times.size
                            viewModel.setMorningTime(times[nextIndex])
                        }) {
                            Icon(Icons.Default.AccessTime, contentDescription = "تغيير الوقت")
                        }
                        Switch(checked = isMorningEnabled, onCheckedChange = { viewModel.setMorningEnabled(it) })
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Evening
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("أذكار المساء", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("موعد التنبيه: $eveningTime", fontSize = 12.sp, color = Color.Gray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val times = listOf("16:30", "17:00", "17:30", "18:00", "18:30")
                            val nextIndex = (times.indexOf(eveningTime) + 1) % times.size
                            viewModel.setEveningTime(times[nextIndex])
                        }) {
                            Icon(Icons.Default.AccessTime, contentDescription = "تغيير الوقت")
                        }
                        Switch(checked = isEveningEnabled, onCheckedChange = { viewModel.setEveningEnabled(it) })
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Sleep
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("أذكار النوم", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("موعد التنبيه: $sleepTime", fontSize = 12.sp, color = Color.Gray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val times = listOf("21:00", "21:30", "22:00", "22:30", "23:00")
                            val nextIndex = (times.indexOf(sleepTime) + 1) % times.size
                            viewModel.setSleepTime(times[nextIndex])
                        }) {
                            Icon(Icons.Default.AccessTime, contentDescription = "تغيير الوقت")
                        }
                        Switch(checked = isSleepEnabled, onCheckedChange = { viewModel.setSleepEnabled(it) })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 4: Hijri calendar tuning
        Text("ضبط التقويم الهجري", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("تعديل التاريخ الهجري الحسابي بيوم أو أكثر ليتوافق مع الرؤية الشرعية:", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { viewModel.adjustHijriOffset(-1) }, colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary)) {
                        Text("-1 يوم")
                    }
                    Text(text = "التعديل: $hijriOffset يوم", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Button(onClick = { viewModel.adjustHijriOffset(1) }, colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary)) {
                        Text("+1 يوم")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { viewModel.resetHijriOffset() }, modifier = Modifier.fillMaxWidth()) {
                    Text("إعادة الضبط الافتراضي (0)", color = Color.Red.copy(alpha = 0.7f))
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

// ==========================================
// 8. ABOUT SCREEN
// ==========================================
@Composable
fun AboutScreen(viewModel: AtharViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_app_logo),
            contentDescription = "شعار التطبيق",
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .shadow(8.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("أثر - الرفيق الإسلامي", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = IslamicGreenPrimary)
        Text("النسخة العالمية 1.0", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "تطبيق 'أثر' هو منصة إسلامية متكاملة تهدف لمساعدة المسلم في روتينه اليومي من تلاوة واستماع للقرآن الكريم، وقراءة حصن المسلم من الأذكار، وتتبع مواقيت الصلاة الدقيقة وتحديد اتجاه القبلة مع السبحة الإلكترونية بطرق تفاعلية وحلول تكنولوجية متقدمة.",
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("تصميم وتطوير بمقاييس عالمية", fontSize = 12.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
        Text("صدقة جارية لكل مسلم ومسلمة", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
    }
}
