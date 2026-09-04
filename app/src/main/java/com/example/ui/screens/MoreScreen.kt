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
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.data.AtharTab
import com.example.data.AtharViewModel
import com.example.ui.components.AtharTopBar
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AtharTheme

/* ---------------------------------------------------------------------------
 *  شاشة «المزيد» — مركز الوصول لكل أقسام التطبيق
 * ------------------------------------------------------------------------- */

private data class MoreEntry(
    val tab: AtharTab,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
)

@Composable
fun MoreScreen(vm: AtharViewModel, onNavigate: (AtharTab) -> Unit) {
    val isDark by vm.isDarkMode.collectAsState()

    val tools = listOf(
        MoreEntry(AtharTab.TASBIH, "السبحة الإلكترونية", "عدّاد تسبيح بأذكار مختارة", Icons.Filled.TouchApp, Color(0xFF3E8E7E)),
        MoreEntry(AtharTab.QIBLA, "اتجاه القبلة", "بوصلة تعتمد على موقعك", Icons.Filled.Explore, Color(0xFFC28B2B)),
        MoreEntry(AtharTab.HIJRI, "التقويم الهجري", "الشهور والمناسبات الإسلامية", Icons.Filled.DateRange, Color(0xFF2F7EA8)),
    )
    val audio = listOf(
        MoreEntry(AtharTab.AUDIOPLAYER, "مشغّل التلاوات", "استمع بصوت كبار القرّاء", Icons.Filled.Headset, Color(0xFF7A6BC4)),
        MoreEntry(AtharTab.DOWNLOADS, "التنزيلات", "السور المحفوظة على جهازك", Icons.Filled.Download, Color(0xFF2E8B6F)),
        MoreEntry(AtharTab.FAVORITES, "المفضلة", "الأذكار والآيات المحفوظة", Icons.Filled.Star, Color(0xFFC2762B)),
    )
    val app = listOf(
        MoreEntry(AtharTab.SETTINGS, "الإعدادات", "الأذان، التنبيهات، المظهر", Icons.Filled.Settings, Color(0xFF6A7C74)),
        MoreEntry(AtharTab.ABOUT, "حول التطبيق", "عن أثر والإصدار", Icons.Filled.Info, Color(0xFF8A7B5C)),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AtharTopBar(title = "المزيد", subtitle = "كل أدوات أثر في مكان واحد")

        Spacer(Modifier.height(4.dp))

        // مبدّل الوضع الليلي
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(AtharTheme.extra.gold.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.DarkMode,
                        contentDescription = null,
                        tint = AtharTheme.extra.gold,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("الوضع الليلي", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (isDark) "مفعّل" else "معطّل",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = isDark, onCheckedChange = { vm.setDarkMode(it) })
            }
        }

        MoreGroup("أدوات المسلم", tools, onNavigate)
        MoreGroup("التلاوة والاستماع", audio, onNavigate)
        MoreGroup("التطبيق", app, onNavigate)

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun MoreGroup(
    title: String,
    entries: List<MoreEntry>,
    onNavigate: (AtharTab) -> Unit,
) {
    Spacer(Modifier.height(18.dp))
    SectionHeader(title = title, modifier = Modifier.padding(horizontal = 16.dp))
    Spacer(Modifier.height(6.dp))
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        entries.forEach { entry ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onNavigate(entry.tab) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(entry.accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            entry.icon,
                            contentDescription = null,
                            tint = entry.accent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.title, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            entry.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
