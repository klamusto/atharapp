package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.AtharTab
import com.example.data.AtharViewModel
import com.example.data.PrayerTimesCalculator
import com.example.ui.components.AtharCard
import com.example.ui.components.AtharTopBar
import com.example.ui.components.GradientHero
import com.example.ui.components.PillButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.to12HourArabic
import com.example.ui.theme.AtharTheme
import com.example.ui.util.formatCountdown
import com.example.ui.util.formatGregorianLong
import kotlinx.coroutines.delay

/* ---------------------------------------------------------------------------
 *  مواقيت الصلاة
 * ------------------------------------------------------------------------- */

@Composable
fun PrayerTimesScreen(vm: AtharViewModel, onNavigate: (AtharTab) -> Unit) {
    val context = LocalContext.current
    val times by vm.prayerTimes.collectAsState()
    val isLocationFetched by vm.isLocationFetched.collectAsState()
    val fetchedName by vm.fetchedLocationName.collectAsState()
    val city by vm.selectedCity.collectAsState()
    val athanEnabled by vm.isAthanEnabled.collectAsState()

    var showCityPicker by remember { mutableStateOf(false) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000)
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result.values.any { it }
        if (granted) vm.fetchDeviceLocation()
    }

    val requestLocation: () -> Unit = {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            vm.fetchDeviceLocation()
        } else {
            locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    val next = times?.let { vm.computeNextPrayer(nowMs) }
    val currentName = times?.let { vm.currentPrayerName(nowMs) }
    val locationLabel = if (isLocationFetched) fetchedName else city.nameAr

    Column(modifier = Modifier.fillMaxSize()) {
        AtharTopBar(
            title = "مواقيت الصلاة",
            subtitle = formatGregorianLong(),
            actions = {
                IconButton(onClick = { vm.setAthanEnabled(!athanEnabled) }) {
                    Icon(
                        imageVector = if (athanEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                        contentDescription = "تفعيل الأذان",
                        tint = if (athanEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onNavigate(AtharTab.SETTINGS) }) {
                    Icon(Icons.Filled.Settings, contentDescription = "الإعدادات")
                }
            },
        )

        LazyColumn(
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                GradientHero(shape = RoundedCornerShape(0.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = AtharTheme.extra.gold,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = locationLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = AtharTheme.extra.onHero,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        if (next != null) {
                            Text(
                                text = "الوقت المتبقّي لصلاة ${next.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AtharTheme.extra.onHeroMuted,
                            )
                            Text(
                                text = formatCountdown(next.timeMs - nowMs),
                                style = MaterialTheme.typography.displaySmall,
                                color = AtharTheme.extra.gold,
                            )
                            Text(
                                text = "أذان ${next.name} عند ${to12HourArabic(next.time24)}" +
                                    if (next.isTomorrow) " (غداً)" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = AtharTheme.extra.onHeroMuted,
                            )
                        } else {
                            Text(
                                text = "جارٍ حساب المواقيت…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AtharTheme.extra.onHeroMuted,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PillButton(
                                text = "موقعي الحالي",
                                icon = Icons.Filled.MyLocation,
                                container = AtharTheme.extra.gold,
                                contentColor = androidx.compose.ui.graphics.Color(0xFF1B1400),
                                onClick = requestLocation,
                            )
                            PillButton(
                                text = "اختيار مدينة",
                                icon = Icons.Filled.LocationCity,
                                container = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.16f),
                                contentColor = AtharTheme.extra.onHero,
                                onClick = { showCityPicker = true },
                            )
                        }
                    }
                }
            }

            val t = times
            if (t != null) {
                val rows = listOf(
                    PrayerRow("الفجر", t.fajr, Icons.Filled.Brightness4),
                    PrayerRow("الشروق", t.sunrise, Icons.Filled.WbSunny),
                    PrayerRow("الظهر", t.dhuhr, Icons.Filled.LightMode),
                    PrayerRow("العصر", t.asr, Icons.Filled.WbCloudy),
                    PrayerRow("المغرب", t.maghrib, Icons.Filled.Brightness6),
                    PrayerRow("العشاء", t.isha, Icons.Filled.DarkMode),
                )
                items(rows) { row ->
                    PrayerTimeItem(
                        row = row,
                        isCurrent = row.name == currentName,
                        isNext = row.name == next?.name,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                SectionHeader(title = "أدوات مساعدة", modifier = Modifier.padding(horizontal = 16.dp))
                AtharCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = { onNavigate(AtharTab.QIBLA) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Explore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("اتجاه القبلة", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "بوصلة دقيقة تعتمد على موقعك",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "طريقة الحساب: أم القرى · التوقيت المحلّي للجهاز",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }

    if (showCityPicker) {
        CityPickerDialog(vm = vm, onDismiss = { showCityPicker = false })
    }
}

private data class PrayerRow(val name: String, val time: String, val icon: ImageVector)

@Composable
private fun PrayerTimeItem(
    row: PrayerRow,
    isCurrent: Boolean,
    isNext: Boolean,
    modifier: Modifier = Modifier,
) {
    val container = when {
        isNext -> MaterialTheme.colorScheme.primaryContainer
        isCurrent -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val onContainer = when {
        isNext -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = container,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                row.icon,
                contentDescription = null,
                tint = if (isNext) onContainer else AtharTheme.extra.gold,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(row.name, style = MaterialTheme.typography.titleSmall, color = onContainer)
                if (isNext) {
                    Text(
                        "الصلاة القادمة",
                        style = MaterialTheme.typography.labelSmall,
                        color = onContainer.copy(alpha = 0.75f),
                    )
                } else if (isCurrent) {
                    Text(
                        "الوقت الحالي",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = to12HourArabic(row.time),
                style = MaterialTheme.typography.titleMedium,
                color = onContainer,
            )
        }
    }
}

@Composable
private fun CityPickerDialog(vm: AtharViewModel, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val results by vm.searchResults.collectAsState()
    val isSearching by vm.isSearching.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختيار المدينة", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("ابحث عن مدينة…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { vm.searchCityOnline(query) }) {
                            Icon(Icons.Filled.Search, contentDescription = "بحث")
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
                if (isSearching) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                }
                val list = if (results.isNotEmpty()) results else PrayerTimesCalculator.DEFAULT_CITIES
                Text(
                    text = if (results.isNotEmpty()) "نتائج البحث" else "مدن مقترحة",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(list) { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    vm.selectCity(c)
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.LocationCity,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = c.nameAr,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        },
    )
}
