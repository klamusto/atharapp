package com.example.features.quran.presentation

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.features.quran.domain.AyahModel
import com.example.ui.components.toArabicDigits
import com.example.ui.screens.copyText
import com.example.ui.screens.shareText
import com.example.ui.theme.AtharTheme
import com.example.ui.theme.quranTextStyle
import com.example.ui.theme.thikrTextStyle

/* ---------------------------------------------------------------------------
 *  قارئ المصحف
 * ------------------------------------------------------------------------- */

private const val BASMALA = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(vm: QuranViewModel) {
    val context = LocalContext.current
    val currentPage by vm.currentPage.collectAsState()
    val fontSize by vm.fontSize.collectAsState()
    val immersive by vm.isImmersive.collectAsState()
    val highlighted by vm.highlightedAyah.collectAsState()
    val bookmarks by vm.bookmarkedAyahs.collectAsState()
    val favorites by vm.favoriteAyahs.collectAsState()
    val playingSurah by SurahPlaybackManager.currentSurah.collectAsState()

    val pagerState = rememberPagerState(
        initialPage = (currentPage - 1).coerceIn(0, QuranViewModel.TOTAL_PAGES - 1),
        pageCount = { QuranViewModel.TOTAL_PAGES },
    )

    var selectedAyah by remember { mutableStateOf<AyahModel?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { index ->
            vm.onPageChanged(index + 1)
        }
    }
    LaunchedEffect(currentPage) {
        val target = (currentPage - 1).coerceIn(0, QuranViewModel.TOTAL_PAGES - 1)
        if (pagerState.currentPage != target) {
            pagerState.scrollToPage(target)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { index ->
            val page = index + 1
            val ayahs by produceState(
                initialValue = emptyList<AyahModel>(),
                key1 = page,
                key2 = bookmarks,
                key3 = favorites,
            ) {
                value = vm.ayahsForPage(page)
            }

            QuranPage(
                page = page,
                ayahs = ayahs,
                fontSize = fontSize,
                highlightedAyah = highlighted,
                surahNameOf = { n -> vm.surahOf(n)?.name ?: "" },
                revelationOf = { n -> vm.surahOf(n)?.revelationType ?: "" },
                versesOf = { n -> vm.surahOf(n)?.numberOfVerses ?: 0 },
                topPadding = if (immersive) 28.dp else 74.dp,
                bottomPadding = if (immersive) 28.dp else 120.dp,
                onAyahClick = { ayah ->
                    vm.highlightedAyah.value = ayah.number
                    selectedAyah = ayah
                },
                onBackgroundTap = { vm.toggleImmersive() },
            )
        }

        // الشريط العلوي
        AnimatedVisibility(
            visible = !immersive,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            ReaderTopBar(page = currentPage, vm = vm)
        }

        // الشريط السفلي
        AnimatedVisibility(
            visible = !immersive,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column {
                if (playingSurah != null) {
                    MiniPlayerBar(context = context)
                }
                ReaderBottomBar(vm = vm, page = currentPage)
            }
        }

        // زر الخروج من وضع القراءة الكاملة
        if (immersive) {
            IconButton(
                onClick = { vm.setImmersive(false) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
            ) {
                Icon(
                    Icons.Filled.FullscreenExit,
                    contentDescription = "خروج من ملء الشاشة",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    val ayah = selectedAyah
    if (ayah != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedAyah = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            AyahSheetContent(
                vm = vm,
                ayah = ayah,
                onClose = { selectedAyah = null },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// صفحة واحدة من المصحف
// ---------------------------------------------------------------------------

@Composable
private fun QuranPage(
    page: Int,
    ayahs: List<AyahModel>,
    fontSize: Float,
    highlightedAyah: Int?,
    surahNameOf: (Int) -> String,
    revelationOf: (Int) -> String,
    versesOf: (Int) -> Int,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onAyahClick: (AyahModel) -> Unit,
    onBackgroundTap: () -> Unit,
) {
    val paper = AtharTheme.extra.paper
    val onPaper = AtharTheme.extra.onPaper
    val gold = AtharTheme.extra.gold

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(paper)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onBackgroundTap() })
            },
    ) {
        if (ayahs.isEmpty()) {
            QuranMiniLoader()
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = topPadding, bottom = bottomPadding),
        ) {
            val groups = ayahs.groupBy { it.surahNumber }.toList().sortedBy { it.first }

            groups.forEach { (surahNumber, list) ->
                val startsHere = list.any { it.numberInSurah == 1 }
                if (startsHere) {
                    SurahBanner(
                        name = surahNameOf(surahNumber),
                        revelation = revelationOf(surahNumber),
                        verses = versesOf(surahNumber),
                        gold = gold,
                        onPaper = onPaper,
                    )
                    if (surahNumber != 1 && surahNumber != 9) {
                        Text(
                            text = BASMALA,
                            style = quranTextStyle(fontSize + 2f),
                            color = gold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                        )
                    }
                }

                val annotated = buildAnnotatedString {
                    list.forEach { ayah ->
                        val isHighlighted = highlightedAyah == ayah.number
                        pushStringAnnotation(tag = "ayah", annotation = ayah.number.toString())
                        if (isHighlighted) {
                            withStyle(
                                SpanStyle(
                                    background = gold.copy(alpha = 0.22f),
                                    color = onPaper,
                                ),
                            ) {
                                append(cleanAyahText(ayah))
                            }
                        } else {
                            withStyle(SpanStyle(color = onPaper)) {
                                append(cleanAyahText(ayah))
                            }
                        }
                        append(" ")
                        withStyle(
                            SpanStyle(
                                color = gold,
                                fontSize = (fontSize * 0.78f).sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        ) {
                            append("﴿${toArabicDigits(ayah.numberInSurah)}﴾")
                        }
                        append("  ")
                        pop()
                    }
                }

                ClickableText(
                    text = annotated,
                    style = quranTextStyle(fontSize).copy(textAlign = TextAlign.Justify),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    onClick = { offset ->
                        annotated.getStringAnnotations("ayah", offset, offset)
                            .firstOrNull()
                            ?.let { annotation ->
                                val number = annotation.item.toIntOrNull()
                                val ayah = list.firstOrNull { it.number == number }
                                if (ayah != null) onAyahClick(ayah)
                            }
                    },
                )
            }

            Spacer(Modifier.height(18.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = CircleShape,
                    color = gold.copy(alpha = 0.14f),
                ) {
                    Text(
                        text = toArabicDigits(page),
                        style = MaterialTheme.typography.labelLarge,
                        color = gold,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SurahBanner(
    name: String,
    revelation: String,
    verses: Int,
    gold: androidx.compose.ui.graphics.Color,
    onPaper: androidx.compose.ui.graphics.Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gold.copy(alpha = 0.10f))
            .border(1.dp, gold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "سورة $name",
                style = thikrTextStyle(24f),
                color = onPaper,
            )
            Text(
                text = "$revelation · ${toArabicDigits(verses)} آية",
                style = MaterialTheme.typography.labelSmall,
                color = gold,
            )
        }
    }
}

private fun cleanAyahText(ayah: AyahModel): String {
    val text = ayah.text.trim()
    if (ayah.numberInSurah == 1 && ayah.surahNumber != 1 && ayah.surahNumber != 9) {
        if (text.startsWith(BASMALA)) {
            return text.removePrefix(BASMALA).trim()
        }
    }
    return text
}

// ---------------------------------------------------------------------------
// أشرطة التحكّم
// ---------------------------------------------------------------------------

@Composable
private fun ReaderTopBar(page: Int, vm: QuranViewModel) {
    val juz = QuranViewModel.juzOfPage(page)
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.openIndex() }) {
                Icon(Icons.Filled.List, contentDescription = "الفهرس")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "صفحة ${toArabicDigits(page)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "الجزء ${toArabicDigits(juz)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { vm.decreaseFont() }) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "تصغير الخط")
            }
            Icon(
                Icons.Filled.TextFields,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            IconButton(onClick = { vm.increaseFont() }) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "تكبير الخط")
            }
            IconButton(onClick = { vm.setImmersive(true) }) {
                Icon(Icons.Filled.Fullscreen, contentDescription = "ملء الشاشة")
            }
        }
    }
}

@Composable
private fun ReaderBottomBar(vm: QuranViewModel, page: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = toArabicDigits(1),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = page.toFloat(),
                    onValueChange = { vm.onPageChanged(it.toInt()) },
                    valueRange = 1f..QuranViewModel.TOTAL_PAGES.toFloat(),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                )
                Text(
                    text = toArabicDigits(QuranViewModel.TOTAL_PAGES),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerBar(context: Context) {
    val surah by SurahPlaybackManager.currentSurah.collectAsState()
    val isPlaying by SurahPlaybackManager.isPlaying.collectAsState()
    val reciter by SurahPlaybackManager.currentReciter.collectAsState()

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Headset,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surah?.name ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                )
                Text(
                    text = reciter.nameArabic,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    maxLines = 1,
                )
            }
            IconButton(onClick = {
                sendAudioAction(
                    context,
                    if (isPlaying) QuranAudioService.ACTION_PAUSE else QuranAudioService.ACTION_RESUME,
                )
            }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "تشغيل/إيقاف",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            IconButton(onClick = { sendAudioAction(context, QuranAudioService.ACTION_STOP) }) {
                Icon(
                    Icons.Filled.Stop,
                    contentDescription = "إيقاف",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ورقة خيارات الآية
// ---------------------------------------------------------------------------

@Composable
private fun AyahSheetContent(vm: QuranViewModel, ayah: AyahModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val hasTafsir by vm.hasTafsir.collectAsState()
    val tafsirDownloading by vm.isTafsirDownloading.collectAsState()
    val tafsirProgress by vm.tafsirProgress.collectAsState()
    val surahName = vm.surahOf(ayah.surahNumber)?.name ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "سورة $surahName · الآية ${toArabicDigits(ayah.numberInSurah)}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "إغلاق")
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = ayah.text,
            style = quranTextStyle(22f),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Justify,
        )

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SheetAction(
                icon = if (ayah.isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                label = "علامة",
                active = ayah.isBookmarked,
            ) { vm.toggleBookmark(ayah) }
            SheetAction(
                icon = if (ayah.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                label = "مفضلة",
                active = ayah.isFavorite,
            ) { vm.toggleFavorite(ayah) }
            SheetAction(icon = Icons.Filled.ContentCopy, label = "نسخ") {
                copyText(context, "${ayah.text}\n[$surahName: ${ayah.numberInSurah}]")
            }
            SheetAction(icon = Icons.Filled.Share, label = "مشاركة") {
                shareText(context, "${ayah.text}\n[$surahName: ${ayah.numberInSurah}]")
            }
            SheetAction(icon = Icons.Filled.MenuBook, label = "السورة") {
                playSurahViaService(context, ayah.surahNumber)
            }
        }

        Spacer(Modifier.height(18.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "التفسير الميسّر",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                when {
                    !ayah.tafsir.isNullOrBlank() -> Text(
                        text = ayah.tafsir ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Justify,
                    )
                    tafsirDownloading -> Column {
                        Text(
                            text = "جارٍ تنزيل التفسير… ${toArabicDigits((tafsirProgress * 100).toInt())}٪",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        com.example.ui.components.ProgressBar(progress = tafsirProgress)
                    }
                    hasTafsir -> Text(
                        text = "لا يتوفّر تفسير لهذه الآية.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> Column {
                        Text(
                            text = "التفسير غير منزّل بعد. يمكنك تنزيله مرة واحدة لاستخدامه دون إنترنت.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        com.example.ui.components.PillButton(
                            text = "تنزيل التفسير",
                            onClick = { vm.downloadTafsir() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .size(width = 66.dp, height = 64.dp)
                .clickable { onClick() }
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp),
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** مشغّل بسيط لتلاوة آية واحدة. */
object AyahAudioPlayer {
    private var player: MediaPlayer? = null

    fun play(url: String) {
        stop()
        try {
            player = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { start() }
                setOnCompletionListener { stop() }
                prepareAsync()
            }
        } catch (e: Exception) {
            stop()
        }
    }

    fun stop() {
        try {
            player?.release()
        } catch (e: Exception) {
            // تجاهل
        }
        player = null
    }
}
