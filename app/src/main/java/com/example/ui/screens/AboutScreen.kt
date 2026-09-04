package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.AtharTopBar
import com.example.ui.components.GradientHero
import com.example.ui.theme.AtharTheme
import com.example.ui.theme.thikrTextStyle

/* ---------------------------------------------------------------------------
 *  حول التطبيق
 * ------------------------------------------------------------------------- */

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val features = listOf(
        "المصحف الكامل بخط عثماني مع التفسير الميسّر" to Icons.Filled.MenuBook,
        "حصن المسلم مع عدّاد ذكي وحفظ المفضلة" to Icons.Filled.Favorite,
        "مواقيت صلاة دقيقة وتنبيهات أذان" to Icons.Filled.Schedule,
        "بوصلة قبلة تعتمد على المستشعرات" to Icons.Filled.Explore,
        "سبحة إلكترونية مع إحصاءات" to Icons.Filled.TouchApp,
        "تلاوات صوتية لكبار القرّاء مع التنزيل" to Icons.Filled.Headset,
        "تقويم هجري بالمناسبات الإسلامية" to Icons.Filled.DateRange,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AtharTopBar(title = "حول التطبيق", onBack = onBack)

        GradientHero(shape = RoundedCornerShape(24.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "شعار أثر",
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(24.dp)),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "أَثَــر",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AtharTheme.extra.gold,
                )
                Text(
                    text = "رفيقك الإسلامي اليومي · الإصدار ٢٫٠",
                    style = MaterialTheme.typography.bodySmall,
                    color = AtharTheme.extra.onHeroMuted,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "«أثر» تطبيق إسلامي متكامل يجمع لك القرآن الكريم والأذكار ومواقيت الصلاة والقبلة والتقويم الهجري في واجهة عربية أنيقة وسهلة، مع إمكانية العمل دون اتصال بالإنترنت بعد التنزيل الأول.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Justify,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            features.forEach { (label, icon) ->
                FeatureLine(label, icon)
            }
        }

        Spacer(Modifier.height(22.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "﴿وَقُل رَّبِّ زِدْنِي عِلْمًا﴾",
                    style = thikrTextStyle(22f),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "هذا العمل صدقة جارية — لا تنسونا من دعائكم",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun FeatureLine(label: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
