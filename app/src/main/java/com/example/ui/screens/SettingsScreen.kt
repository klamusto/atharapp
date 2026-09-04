package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.data.AtharViewModel
import com.example.ui.components.AtharTopBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.to12HourArabic
import com.example.ui.components.toArabicDigits
import com.example.ui.theme.AtharTheme
import java.util.Locale

/* ---------------------------------------------------------------------------
 *  الإعدادات
 * ------------------------------------------------------------------------- */

@Composable
fun SettingsScreen(vm: AtharViewModel, onBack: () -> Unit) {
    val isDark by vm.isDarkMode.collectAsState()
    val hijriOffset by vm.hijriOffset.collectAsState()

    val athanEnabled by vm.isAthanEnabled.collectAsState()
    val alertOnly by vm.isAlertOnly.collectAsState()
    val athanSound by vm.selectedAthanSound.collectAsState()
    val athanVolume by vm.athanVolume.collectAsState()

    val morningEnabled by vm.isMorningNotificationEnabled.collectAsState()
    val eveningEnabled by vm.isEveningNotificationEnabled.collectAsState()
    val sleepEnabled by vm.isSleepNotificationEnabled.collectAsState()
    val morningTime by vm.morningNotificationTime.collectAsState()
    val eveningTime by vm.eveningNotificationTime.collectAsState()
    val sleepTime by vm.sleepNotificationTime.collectAsState()

    var timePickerFor by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AtharTopBar(title = "الإعدادات", onBack = onBack)

        // ---------------- المظهر ----------------
        SettingsGroup(title = "المظهر") {
            ToggleRow(
                title = "الوضع الليلي",
                subtitle = "ألوان داكنة مريحة للعين",
                icon = Icons.Filled.DarkMode,
                checked = isDark,
                onCheckedChange = { vm.setDarkMode(it) },
            )
        }

        // ---------------- الأذان ----------------
        SettingsGroup(title = "الأذان وتنبيهات الصلاة") {
            ToggleRow(
                title = "تنبيهات الأذان",
                subtitle = "إشعار وصوت عند دخول وقت الصلاة",
                icon = Icons.Filled.NotificationsActive,
                checked = athanEnabled,
                onCheckedChange = { vm.setAthanEnabled(it) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ToggleRow(
                title = "تنبيه قصير فقط",
                subtitle = "نغمة قصيرة بدل الأذان الكامل",
                icon = Icons.Filled.Schedule,
                checked = alertOnly,
                enabled = athanEnabled,
                onCheckedChange = { vm.setAthanAlertOnly(it) },
            )

            if (athanEnabled && !alertOnly) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Text(
                    text = "صوت المؤذّن",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
                )
                val sounds = listOf(
                    "Muaiqly" to "الشيخ ماهر المعيقلي",
                    "Makkah" to "أذان الحرم المكي",
                    "Madinah" to "أذان الحرم المدني",
                )
                sounds.forEach { (id, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.setAthanSound(id) }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = athanSound == id, onClick = { vm.setAthanSound(id) })
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (athanEnabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "مستوى الصوت",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.width(10.dp))
                    Slider(
                        value = athanVolume,
                        onValueChange = { vm.setAthanVolume(it) },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${toArabicDigits((athanVolume * 100).toInt())}٪",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ---------------- تنبيهات الأذكار ----------------
        SettingsGroup(title = "تنبيهات الأذكار اليومية") {
            AzkarReminderRow(
                title = "أذكار الصباح",
                icon = Icons.Filled.WbSunny,
                time = morningTime,
                enabled = morningEnabled,
                onToggle = { vm.setMorningEnabled(it) },
                onEditTime = { timePickerFor = "morning" },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            AzkarReminderRow(
                title = "أذكار المساء",
                icon = Icons.Filled.WbTwilight,
                time = eveningTime,
                enabled = eveningEnabled,
                onToggle = { vm.setEveningEnabled(it) },
                onEditTime = { timePickerFor = "evening" },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            AzkarReminderRow(
                title = "أذكار النوم",
                icon = Icons.Filled.Bedtime,
                time = sleepTime,
                enabled = sleepEnabled,
                onToggle = { vm.setSleepEnabled(it) },
                onEditTime = { timePickerFor = "sleep" },
            )
        }

        // ---------------- التقويم ----------------
        SettingsGroup(title = "ضبط التقويم الهجري") {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "عدّل التاريخ الهجري بيوم أو أكثر ليوافق الرؤية الشرعية في بلدك.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = AtharTheme.extra.gold,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (hijriOffset == 0) "بدون تعديل"
                        else "${if (hijriOffset > 0) "+" else "−"}${toArabicDigits(kotlin.math.abs(hijriOffset))} يوم",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    RoundControl(Icons.Filled.Remove, "إنقاص") { vm.adjustHijriOffset(-1) }
                    Spacer(Modifier.width(10.dp))
                    RoundControl(Icons.Filled.Add, "زيادة") { vm.adjustHijriOffset(1) }
                }
                if (hijriOffset != 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "إعادة الضبط الافتراضي",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { vm.resetHijriOffset() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "أثر · الإصدار ٢٫٠",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }

    val target = timePickerFor
    if (target != null) {
        val initial = when (target) {
            "morning" -> morningTime
            "evening" -> eveningTime
            else -> sleepTime
        }
        TimePickerDialog(
            initialTime = initial,
            title = when (target) {
                "morning" -> "وقت تنبيه أذكار الصباح"
                "evening" -> "وقت تنبيه أذكار المساء"
                else -> "وقت تنبيه أذكار النوم"
            },
            onDismiss = { timePickerFor = null },
            onConfirm = { newTime ->
                when (target) {
                    "morning" -> vm.setMorningTime(newTime)
                    "evening" -> vm.setEveningTime(newTime)
                    else -> vm.setSleepTime(newTime)
                }
                timePickerFor = null
            },
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(14.dp))
    SectionHeader(title = title, modifier = Modifier.padding(horizontal = 16.dp))
    Spacer(Modifier.height(4.dp))
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun AzkarReminderRow(
    title: String,
    icon: ImageVector,
    time: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onEditTime: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AtharTheme.extra.gold.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = AtharTheme.extra.gold,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = to12HourArabic(time),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onEditTime() }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun RoundControl(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime: String,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val parts = initialTime.split(":")
    val initHour = parts.getOrNull(0)?.toIntOrNull() ?: 5
    val initMinute = parts.getOrNull(1)?.toIntOrNull() ?: 30

    val state = rememberTimePickerState(
        initialHour = initHour,
        initialMinute = initMinute,
        is24Hour = false,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(String.format(Locale.US, "%02d:%02d", state.hour, state.minute))
            }) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}
