package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AtharTab
import com.example.data.AtharViewModel
import com.example.ui.components.AtharCard
import com.example.ui.components.FeatureTile
import com.example.ui.components.GradientHero
import com.example.ui.components.SectionHeader
import com.example.ui.components.to12HourArabic
import com.example.ui.theme.AtharTheme
import com.example.ui.theme.thikrTextStyle
import com.example.ui.util.adjustedHijri
import com.example.ui.util.formatCountdown
import com.example.ui.util.formatGregorianLong
import com.example.ui.util.formatHijriLong
import kotlinx.coroutines.delay
import java.time.LocalDate

/* ---------------------------------------------------------------------------
 *  الشاشة الرئيسية — لوحة اليوم
 * ------------------------------------------------------------------------- */

@Composable
fun HomeScreen(vm: AtharViewModel, onNavigate: (AtharTab) -> Unit) {
    val isDark by vm.isDarkMode.collectAsState()
    val hijriOffset by vm.hijriOffset.collectAsState()
    val prayerTimes by vm.prayerTimes.collectAsState()
    val isLocationFetched by vm.isLocationFetched.collectAsState()
    val fetchedName by vm.fetchedLocationName.collectAsState()
    val city by vm.selectedCity.collectAsState()
    val thikrs by vm.allThikrs.collectAsState()

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000)
        }
    }

    val next = prayerTimes?.let { vm.computeNextPrayer(nowMs) }
    val remaining = next?.let { it.timeMs - nowMs } ?: 0L
    val locationLabel = if (isLocationFetched) fetchedName else city.nameAr

    val hijriText = remember(hijriOffset, LocalDate.now().dayOfYear) {
        try {
            formatHijriLong(adjustedHijri(hijriOffset))
        } catch (e: Exception) {
            ""
        }
    }
    val gregorianText = remember(LocalDate.now().dayOfYear) { formatGregorianLong() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // ---------------- الترويسة ----------------
        GradientHero {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = "شعار أثر",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, AtharTheme.extra.gold.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "أَثَــر",
                            style = MaterialTheme.typography.titleLarge,
                            color = AtharTheme.extra.gold,
                        )
                        Text(
                            text = "رفيقك الإسلامي اليومي",
                            style = MaterialTheme.typography.bodySmall,
                            color = AtharTheme.extra.onHeroMuted,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { vm.setDarkMode(!isDark) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "تبديل الوضع الليلي",
                            tint = AtharTheme.extra.gold,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = hijriText,
                    style = MaterialTheme.typography.titleMedium,
                    color = AtharTheme.extra.onHero,
                )
                Text(
                    text = gregorianText,
                    style = MaterialTheme.typography.bodySmall,
                    color = AtharTheme.extra.onHeroMuted,
                )

                Spacer(Modifier.height(18.dp))

                // بطاقة الصلاة القادمة
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.13f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = AtharTheme.extra.onHeroMuted,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = locationLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = AtharTheme.extra.onHeroMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "المواقيت",
                                style = MaterialTheme.typography.labelMedium,
                                color = AtharTheme.extra.gold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onNavigate(AtharTab.PRAYER_TIMES) }
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        if (next != null) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "الصلاة القادمة · ${next.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AtharTheme.extra.onHeroMuted,
                                    )
                                    Text(
                                        text = to12HourArabic(next.time24),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = AtharTheme.extra.onHero,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "المتبقّي",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AtharTheme.extra.onHeroMuted,
                                    )
                                    Text(
                                        text = formatCountdown(remaining),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = AtharTheme.extra.gold,
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "جارٍ حساب مواقيت الصلاة…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AtharTheme.extra.onHeroMuted,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ---------------- إجراءات سريعة ----------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickAction("القرآن", Icons.Filled.MenuBook, Modifier.weight(1f)) { onNavigate(AtharTab.QURAN) }
            QuickAction("الأذكار", Icons.Filled.Favorite, Modifier.weight(1f)) { onNavigate(AtharTab.AZKAR) }
            QuickAction("السبحة", Icons.Filled.TouchApp, Modifier.weight(1f)) { onNavigate(AtharTab.TASBIH) }
            QuickAction("القبلة", Icons.Filled.Explore, Modifier.weight(1f)) { onNavigate(AtharTab.QIBLA) }
        }

        Spacer(Modifier.height(22.dp))

        // ---------------- شريط مواقيت اليوم ----------------
        if (prayerTimes != null) {
            SectionHeader(
                title = "مواقيت اليوم",
                icon = Icons.Filled.WbSunny,
                actionLabel = "التفاصيل",
                onAction = { onNavigate(AtharTab.PRAYER_TIMES) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            val t = prayerTimes!!
            val items = listOf(
                Triple("الفجر", t.fajr, Icons.Filled.Brightness4),
                Triple("الشروق", t.sunrise, Icons.Filled.WbSunny),
                Triple("الظهر", t.dhuhr, Icons.Filled.LightMode),
                Triple("العصر", t.asr, Icons.Filled.WbCloudy),
                Triple("المغرب", t.maghrib, Icons.Filled.Brightness6),
                Triple("العشاء", t.isha, Icons.Filled.DarkMode),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items) { item ->
                    val isNext = next?.name == item.first
                    PrayerPill(item.first, item.second, item.third, isNext)
                }
            }
            Spacer(Modifier.height(22.dp))
        }

        // ---------------- ذكر اليوم ----------------
        if (thikrs.isNotEmpty()) {
            var seed by remember { mutableIntStateOf(0) }
            val thikr = remember(thikrs, seed) { thikrs.random() }
            SectionHeader(
                title = "ذكر ووِرد",
                icon = Icons.Filled.Star,
                actionLabel = "ذكر آخر",
                onAction = { seed++ },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            AtharCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = { onNavigate(AtharTab.AZKAR) },
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Text(
                    text = thikr.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = thikr.text.trim(),
                    style = thikrTextStyle(18f),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
                if (thikr.reference.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = thikr.reference,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
        }

        // ---------------- بقية الأقسام ----------------
        SectionHeader(
            title = "كل الأقسام",
            icon = Icons.Filled.DateRange,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))

        val tiles = listOf(
            HomeTile(AtharTab.HIJRI, "التقويم الهجري", "الشهور والمناسبات", Icons.Filled.DateRange, Color(0xFF3E8E7E)),
            HomeTile(AtharTab.AUDIOPLAYER, "الاستماع", "تلاوات بصوت كبار القرّاء", Icons.Filled.Headset, Color(0xFF7A6BC4)),
            HomeTile(AtharTab.DOWNLOADS, "التنزيلات", "السور المحفوظة للاستماع دون نت", Icons.Filled.Download, Color(0xFF2F7EA8)),
            HomeTile(AtharTab.FAVORITES, "المفضلة", "أذكارك وآياتك المحفوظة", Icons.Filled.Star, Color(0xFFC28B2B)),
            HomeTile(AtharTab.SETTINGS, "الإعدادات", "الأذان والتنبيهات والمظهر", Icons.Filled.Settings, Color(0xFF6A7C74)),
            HomeTile(AtharTab.ABOUT, "حول التطبيق", "تعريف بأثر", Icons.Filled.Info, Color(0xFF8A7B5C)),
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            tiles.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { tile ->
                        FeatureTile(
                            title = tile.title,
                            subtitle = tile.subtitle,
                            icon = tile.icon,
                            accent = tile.accent,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(tile.tab) },
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

private data class HomeTile(
    val tab: AtharTab,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
)

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .clickable { onClick() }
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PrayerPill(name: String, time24: String, icon: ImageVector, highlighted: Boolean) {
    val container = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow
    val content = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = container,
        modifier = Modifier.width(96.dp),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (highlighted) MaterialTheme.colorScheme.onPrimary else AtharTheme.extra.gold,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(name, style = MaterialTheme.typography.labelMedium, color = content)
            Spacer(Modifier.height(2.dp))
            Text(
                text = to12HourArabic(time24),
                style = MaterialTheme.typography.bodySmall,
                color = content.copy(alpha = 0.85f),
                maxLines = 1,
            )
        }
    }
}
