package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.AtharViewModel
import com.example.ui.components.AtharCard
import com.example.ui.components.AtharTopBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.toArabicDigits
import com.example.ui.theme.AtharTheme
import com.example.ui.util.ISLAMIC_OCCASIONS
import com.example.ui.util.adjustedHijri
import com.example.ui.util.formatGregorianLong
import com.example.ui.util.formatHijriLong
import com.example.ui.util.hijriDay
import com.example.ui.util.hijriMonth
import com.example.ui.util.hijriMonthLength
import com.example.ui.util.hijriMonthName
import com.example.ui.util.hijriMonthStartWeekDayIndex
import com.example.ui.util.hijriToGregorian
import com.example.ui.util.hijriYear
import java.time.LocalDate

/* ---------------------------------------------------------------------------
 *  التقويم الهجري
 * ------------------------------------------------------------------------- */

private val WEEK_HEADERS = listOf("السبت", "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")

@Composable
fun HijriScreen(vm: AtharViewModel, onBack: () -> Unit) {
    val offset by vm.hijriOffset.collectAsState()

    val today = remember(offset, LocalDate.now().dayOfYear) { adjustedHijri(offset) }
    val todayY = hijriYear(today)
    val todayM = hijriMonth(today)
    val todayD = hijriDay(today)

    var viewYear by remember(todayY) { mutableIntStateOf(todayY) }
    var viewMonth by remember(todayM) { mutableIntStateOf(todayM) }

    val monthLength = remember(viewYear, viewMonth) { hijriMonthLength(viewYear, viewMonth) }
    val startIndex = remember(viewYear, viewMonth) { hijriMonthStartWeekDayIndex(viewYear, viewMonth) }

    val occasions = remember(viewMonth) {
        ISLAMIC_OCCASIONS.filterKeys { it.first == viewMonth }
            .toList()
            .sortedBy { it.first.second }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AtharTopBar(title = "التقويم الهجري", onBack = onBack)

        // بطاقة اليوم
        AtharCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            border = null,
        ) {
            Text(
                text = "تاريخ اليوم",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatHijriLong(today),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = formatGregorianLong(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "تعديل التاريخ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                SmallRoundButton(Icons.Filled.Remove, "إنقاص يوم") { vm.adjustHijriOffset(-1) }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (offset == 0) "بدون تعديل" else "${if (offset > 0) "+" else "−"}${toArabicDigits(kotlin.math.abs(offset))} يوم",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.width(8.dp))
                SmallRoundButton(Icons.Filled.Add, "زيادة يوم") { vm.adjustHijriOffset(1) }
            }
            if (offset != 0) {
                Text(
                    text = "إعادة الضبط",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { vm.resetHijriOffset() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // تنقّل الشهور
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                if (viewMonth == 1) {
                    viewMonth = 12
                    viewYear -= 1
                } else {
                    viewMonth -= 1
                }
            }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "الشهر السابق")
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${hijriMonthName(viewMonth)} ${toArabicDigits(viewYear)} هـ",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (viewMonth != todayM || viewYear != todayY) {
                    Text(
                        text = "العودة لهذا الشهر",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                viewMonth = todayM
                                viewYear = todayY
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            IconButton(onClick = {
                if (viewMonth == 12) {
                    viewMonth = 1
                    viewYear += 1
                } else {
                    viewMonth += 1
                }
            }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "الشهر التالي")
            }
        }

        Spacer(Modifier.height(6.dp))

        // رؤوس الأيام
        Row(modifier = Modifier.padding(horizontal = 14.dp)) {
            WEEK_HEADERS.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // شبكة الأيام
        val cells = ArrayList<Int?>()
        repeat(startIndex) { cells.add(null) }
        for (d in 1..monthLength) cells.add(d)
        while (cells.size % 7 != 0) cells.add(null)

        Column(modifier = Modifier.padding(horizontal = 14.dp)) {
            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.86f)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (day != null) {
                                DayCell(
                                    day = day,
                                    gregorian = hijriToGregorian(viewYear, viewMonth, day),
                                    isToday = day == todayD && viewMonth == todayM && viewYear == todayY,
                                    hasOccasion = ISLAMIC_OCCASIONS.containsKey(viewMonth to day),
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // المناسبات
        SectionHeader(
            title = "مناسبات ${hijriMonthName(viewMonth)}",
            icon = Icons.Filled.Star,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(6.dp))
        if (occasions.isEmpty()) {
            Text(
                text = "لا توجد مناسبات معروفة في هذا الشهر.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                occasions.forEach { (key, name) ->
                    val day = key.second
                    val gregorian = hijriToGregorian(viewYear, viewMonth, day)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(AtharTheme.extra.gold.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = toArabicDigits(day),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AtharTheme.extra.gold,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.bodyLarge)
                                if (gregorian != null) {
                                    Text(
                                        text = "يوافق ${toArabicDigits(gregorian.dayOfMonth)}/${toArabicDigits(gregorian.monthValue)}/${toArabicDigits(gregorian.year)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Icon(
                                Icons.Filled.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SmallRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun DayCell(
    day: Int,
    gregorian: LocalDate?,
    isToday: Boolean,
    hasOccasion: Boolean,
) {
    val container = when {
        isToday -> MaterialTheme.colorScheme.primary
        hasOccasion -> AtharTheme.extra.gold.copy(alpha = 0.16f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val content = when {
        isToday -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .then(
                if (hasOccasion && !isToday) {
                    Modifier.border(1.dp, AtharTheme.extra.gold.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = toArabicDigits(day),
                style = MaterialTheme.typography.labelLarge,
                color = content,
            )
            if (gregorian != null) {
                Text(
                    text = gregorian.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = content.copy(alpha = 0.6f),
                )
            }
        }
    }
}
