package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.AtharViewModel
import com.example.data.TASBIH_PHRASES
import com.example.ui.components.AtharTopBar
import com.example.ui.components.ChoiceChip
import com.example.ui.components.toArabicDigits
import com.example.ui.theme.AtharTheme
import com.example.ui.theme.thikrTextStyle

/* ---------------------------------------------------------------------------
 *  السبحة الإلكترونية
 * ------------------------------------------------------------------------- */

@Composable
fun TasbihScreen(vm: AtharViewModel, onBack: () -> Unit) {
    val phraseIndex by vm.tasbihPhraseIndex.collectAsState()
    val count by vm.tasbihCount.collectAsState()
    val rounds by vm.tasbihRounds.collectAsState()
    val total by vm.tasbihTotal.collectAsState()
    val vibrate by vm.tasbihVibrate.collectAsState()

    val phrase = TASBIH_PHRASES.getOrElse(phraseIndex) { TASBIH_PHRASES[0] }
    val progress = if (phrase.target == 0) 0f else count.toFloat() / phrase.target.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(280),
        label = "tasbih_progress",
    )
    val bump by animateFloatAsState(
        targetValue = if (count % 2 == 0) 1f else 0.985f,
        animationSpec = tween(90),
        label = "tasbih_bump",
    )

    Column(modifier = Modifier.fillMaxSize()) {
        AtharTopBar(
            title = "السبحة الإلكترونية",
            subtitle = "المجموع الكلي: ${toArabicDigits(total)}",
            onBack = onBack,
            actions = {
                IconButton(onClick = { vm.resetTasbih() }) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = "تصفير")
                }
            },
        )

        // اختيار الذكر
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(TASBIH_PHRASES) { p ->
                val index = TASBIH_PHRASES.indexOf(p)
                ChoiceChip(
                    text = p.text,
                    selected = index == phraseIndex,
                    onClick = { vm.selectTasbihPhrase(index) },
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = phrase.text,
            style = thikrTextStyle(28f),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )
        if (phrase.virtue.isNotBlank()) {
            Text(
                text = phrase.virtue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(258.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    strokeWidth = 12.dp,
                )
                Box(
                    modifier = Modifier
                        .size(216.dp)
                        .scale(bump)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    AtharTheme.extra.heroMid,
                                ),
                            ),
                        )
                        .clickable { vm.incrementTasbih() },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = toArabicDigits(count),
                            style = MaterialTheme.typography.displayLarge,
                            color = AtharTheme.extra.onHero,
                        )
                        Text(
                            text = "من ${toArabicDigits(phrase.target)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AtharTheme.extra.onHeroMuted,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "اضغط للتسبيح",
                            style = MaterialTheme.typography.labelSmall,
                            color = AtharTheme.extra.gold,
                        )
                    }
                }
            }
        }

        // إحصاءات
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatBox("الدورات المكتملة", toArabicDigits(rounds), Modifier.weight(1f))
            StatBox("المجموع الكلي", toArabicDigits(total), Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Vibration,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "الاهتزاز عند العدّ",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = vibrate, onCheckedChange = { vm.setTasbihVibrate(it) })
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = "تصفير المجموع الكلي",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { vm.resetTasbihTotal() }
                .padding(vertical = 10.dp),
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StatBox(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
