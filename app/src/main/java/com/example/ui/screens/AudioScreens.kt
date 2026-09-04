package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.features.quran.presentation.QuranAudioService
import com.example.features.quran.presentation.QuranViewModel
import com.example.features.quran.presentation.RECITERS_LIST
import com.example.features.quran.presentation.Reciter
import com.example.features.quran.presentation.RepeatMode
import com.example.features.quran.presentation.SurahPlaybackManager
import com.example.features.quran.presentation.playSurahViaService
import com.example.features.quran.presentation.seekAudioTo
import com.example.features.quran.presentation.sendAudioAction
import com.example.ui.components.AtharTopBar
import com.example.ui.components.EmptyState
import com.example.ui.components.IslamicPattern
import com.example.ui.components.ProgressBar
import com.example.ui.components.toArabicDigits
import com.example.ui.theme.AtharTheme
import com.example.ui.theme.thikrTextStyle
import java.util.Locale

/* ---------------------------------------------------------------------------
 *  مشغّل التلاوات + التنزيلات
 * ------------------------------------------------------------------------- */

fun formatMillis(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val m = total / 60
    val s = total % 60
    return toArabicDigits(String.format(Locale.US, "%02d:%02d", m, s))
}

@Composable
fun AudioPlayerScreen(quranVm: QuranViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val surahs by quranVm.allSurahs.collectAsState()

    val currentSurah by SurahPlaybackManager.currentSurah.collectAsState()
    val isPlaying by SurahPlaybackManager.isPlaying.collectAsState()
    val isBuffering by SurahPlaybackManager.isBuffering.collectAsState()
    val duration by SurahPlaybackManager.currentDuration.collectAsState()
    val position by SurahPlaybackManager.currentPosition.collectAsState()
    val reciter by SurahPlaybackManager.currentReciter.collectAsState()
    val speed by SurahPlaybackManager.playbackSpeed.collectAsState()
    val repeatMode by SurahPlaybackManager.repeatMode.collectAsState()
    val shuffled by SurahPlaybackManager.isShuffled.collectAsState()
    val downloaded by SurahPlaybackManager.downloadedSurahs.collectAsState()
    val progressMap by SurahPlaybackManager.downloadProgress.collectAsState()

    var showReciters by remember { mutableStateOf(false) }
    var showSurahs by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val spin = rememberInfiniteTransition(label = "disc")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing)),
        label = "disc_angle",
    )

    val extra = AtharTheme.extra

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(extra.heroStart, extra.heroMid, extra.heroStart)),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = extra.onHero,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "مشغّل التلاوات",
                    style = MaterialTheme.typography.titleMedium,
                    color = extra.onHero,
                )
                Text(
                    text = reciter.nameArabic,
                    style = MaterialTheme.typography.labelSmall,
                    color = extra.onHeroMuted,
                )
            }
            IconButton(onClick = { showReciters = true }) {
                Icon(Icons.Filled.RecordVoiceOver, contentDescription = "اختيار القارئ", tint = extra.gold)
            }
            IconButton(onClick = { showSurahs = true }) {
                Icon(Icons.Filled.PlaylistPlay, contentDescription = "قائمة السور", tint = extra.gold)
            }
        }

        // القرص
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .rotate(if (isPlaying) angle else 0f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(extra.heroEnd, extra.heroStart)),
                    )
                    .border(5.dp, extra.gold.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                IslamicPattern(
                    modifier = Modifier.fillMaxSize(),
                    color = extra.gold,
                    alpha = 0.18f,
                    cell = 50.dp,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentSurah?.name ?: "القرآن الكريم",
                        style = thikrTextStyle(28f),
                        color = extra.onHero,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = currentSurah?.englishName ?: "Holy Quran",
                        style = MaterialTheme.typography.labelSmall,
                        color = extra.gold,
                    )
                }
            }
        }

        // معلومات وتحكّم
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = currentSurah?.let { "سورة ${it.name}" } ?: "اختر سورة للاستماع",
                style = MaterialTheme.typography.titleMedium,
                color = extra.onHero,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Text(
                text = currentSurah?.let { "${it.revelationType} · ${toArabicDigits(it.numberOfVerses)} آية" } ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = extra.onHeroMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(10.dp))

            Slider(
                value = if (duration > 0) position.toFloat().coerceIn(0f, duration.toFloat()) else 0f,
                onValueChange = { seekAudioTo(context, it.toLong()) },
                valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                enabled = currentSurah != null && duration > 0,
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = formatMillis(position),
                    style = MaterialTheme.typography.labelSmall,
                    color = extra.onHeroMuted,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatMillis(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = extra.onHeroMuted,
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    SurahPlaybackManager.isShuffled.value = !shuffled
                }) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = "ترتيب عشوائي",
                        tint = if (shuffled) extra.gold else extra.onHeroMuted,
                    )
                }
                IconButton(onClick = { sendAudioAction(context, QuranAudioService.ACTION_PREV_SURAH) }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "السابق", tint = extra.onHero)
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(extra.gold)
                        .clickable {
                            val surah = currentSurah
                            if (surah == null) {
                                playSurahViaService(context, 1)
                            } else {
                                sendAudioAction(
                                    context,
                                    if (isPlaying) QuranAudioService.ACTION_PAUSE
                                    else QuranAudioService.ACTION_RESUME,
                                )
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = extra.heroStart,
                            strokeWidth = 3.dp,
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "تشغيل",
                            tint = androidx.compose.ui.graphics.Color(0xFF1B1400),
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }

                IconButton(onClick = { sendAudioAction(context, QuranAudioService.ACTION_NEXT_SURAH) }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "التالي", tint = extra.onHero)
                }
                IconButton(onClick = {
                    SurahPlaybackManager.repeatMode.value = when (repeatMode) {
                        RepeatMode.OFF -> RepeatMode.ALL
                        RepeatMode.ALL -> RepeatMode.ONE
                        RepeatMode.ONE -> RepeatMode.OFF
                    }
                }) {
                    Icon(
                        imageVector = if (repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "التكرار",
                        tint = if (repeatMode == RepeatMode.OFF) extra.onHeroMuted else extra.gold,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // السرعة
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                            val idx = speeds.indexOfFirst { kotlin.math.abs(it - speed) < 0.01f }
                            SurahPlaybackManager.playbackSpeed.value = speeds[(idx + 1) % speeds.size]
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Speed,
                        contentDescription = "السرعة",
                        tint = extra.onHeroMuted,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "×$speed",
                        style = MaterialTheme.typography.labelMedium,
                        color = extra.onHeroMuted,
                    )
                }

                // تنزيل السورة الحالية
                val surahNumber = currentSurah?.number
                val dlProgress = surahNumber?.let { progressMap[it] }
                val isDownloaded = surahNumber != null && downloaded.contains(surahNumber)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(enabled = surahNumber != null && !isDownloaded && dlProgress == null) {
                            if (surahNumber != null) {
                                SurahPlaybackManager.startDownload(context, surahNumber, reciter.id, scope)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                        contentDescription = "تنزيل",
                        tint = if (isDownloaded) extra.gold else extra.onHeroMuted,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            isDownloaded -> "محفوظة"
                            dlProgress != null && dlProgress >= 0f -> "${toArabicDigits((dlProgress * 100).toInt())}٪"
                            else -> "تنزيل"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = extra.onHeroMuted,
                    )
                }

                IconButton(onClick = { sendAudioAction(context, QuranAudioService.ACTION_STOP) }) {
                    Icon(Icons.Filled.Stop, contentDescription = "إيقاف", tint = extra.onHeroMuted)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showReciters) {
        ReciterDialog(
            current = reciter,
            onSelect = {
                SurahPlaybackManager.saveReciter(context, it)
                showReciters = false
            },
            onDismiss = { showReciters = false },
        )
    }

    if (showSurahs) {
        SurahPickerDialog(
            surahs = surahs.map { it.number to it.name },
            onSelect = {
                playSurahViaService(context, it)
                showSurahs = false
            },
            onDismiss = { showSurahs = false },
        )
    }
}

@Composable
private fun ReciterDialog(current: Reciter, onSelect: (Reciter) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختيار القارئ", style = MaterialTheme.typography.titleMedium) },
        text = {
            LazyColumn(modifier = Modifier.height(340.dp)) {
                items(RECITERS_LIST) { r ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(r) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Headset,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(r.nameArabic, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        if (r.id == current.id) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
    )
}

@Composable
private fun SurahPickerDialog(
    surahs: List<Pair<Int, String>>,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختيار السورة", style = MaterialTheme.typography.titleMedium) },
        text = {
            if (surahs.isEmpty()) {
                Text(
                    "يجب تنزيل بيانات المصحف أولاً من تبويب القرآن.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                LazyColumn(modifier = Modifier.height(360.dp)) {
                    items(surahs) { (number, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelect(number) }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = toArabicDigits(number),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(32.dp),
                            )
                            Text(text = name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
    )
}

// ---------------------------------------------------------------------------
// شاشة التنزيلات
// ---------------------------------------------------------------------------

@Composable
fun DownloadsScreen(quranVm: QuranViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val surahs by quranVm.allSurahs.collectAsState()
    val downloaded by SurahPlaybackManager.downloadedSurahs.collectAsState()
    val progressMap by SurahPlaybackManager.downloadProgress.collectAsState()
    val reciter by SurahPlaybackManager.currentReciter.collectAsState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        AtharTopBar(
            title = "التنزيلات",
            subtitle = "${reciter.nameArabic} · ${toArabicDigits(downloaded.size)} سورة محفوظة",
            onBack = onBack,
        )

        if (surahs.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Download,
                title = "لا توجد بيانات بعد",
                message = "نزّل بيانات المصحف من تبويب القرآن لعرض قائمة السور.",
                modifier = Modifier.fillMaxSize(),
            )
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(surahs, key = { it.number }) { surah ->
                val isDownloaded = downloaded.contains(surah.number)
                val progress = progressMap[surah.number]
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = toArabicDigits(surah.number),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(30.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(surah.name, style = thikrTextStyle(19f))
                                Text(
                                    text = if (isDownloaded) "محفوظة على الجهاز" else "غير منزّلة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { playSurahViaService(context, surah.number) }) {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = "تشغيل",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            if (isDownloaded) {
                                IconButton(onClick = {
                                    SurahPlaybackManager.deleteDownload(context, surah.number, reciter.id)
                                }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "حذف",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            } else if (progress == null) {
                                IconButton(onClick = {
                                    SurahPlaybackManager.startDownload(context, surah.number, reciter.id, scope)
                                }) {
                                    Icon(
                                        Icons.Filled.Download,
                                        contentDescription = "تنزيل",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        if (progress != null && progress >= 0f) {
                            Spacer(Modifier.height(8.dp))
                            ProgressBar(progress = progress, height = 5.dp)
                        }
                    }
                }
            }
        }
    }
}
