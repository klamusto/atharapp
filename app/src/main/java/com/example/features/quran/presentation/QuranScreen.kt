package com.example.features.quran.presentation

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.features.quran.domain.SurahModel
import com.example.ui.components.AtharCard
import com.example.ui.components.AtharTopBar
import com.example.ui.components.ChoiceChip
import com.example.ui.components.EmptyState
import com.example.ui.components.GradientHero
import com.example.ui.components.LoadingState
import com.example.ui.components.PillButton
import com.example.ui.components.ProgressBar
import com.example.ui.components.toArabicDigits
import com.example.ui.theme.AtharTheme
import com.example.ui.theme.quranTextStyle
import com.example.ui.theme.thikrTextStyle

/* ---------------------------------------------------------------------------
 *  القرآن الكريم — نقطة الدخول والفهرس
 * ------------------------------------------------------------------------- */

@Composable
fun QuranScreen(vm: QuranViewModel) {
    val checking by vm.isChecking.collectAsState()
    val initialized by vm.isInitialized.collectAsState()
    val mode by vm.mode.collectAsState()

    when {
        checking -> Box(modifier = Modifier.fillMaxSize()) {
            LoadingState("جارٍ فتح المصحف…", modifier = Modifier.fillMaxSize())
        }
        !initialized -> QuranSetupScreen(vm)
        mode == QuranMode.READER -> QuranReaderScreen(vm)
        else -> QuranIndexScreen(vm)
    }
}

// ---------------------------------------------------------------------------
// شاشة التهيئة (تنزيل نص المصحف أول مرة)
// ---------------------------------------------------------------------------

@Composable
private fun QuranSetupScreen(vm: QuranViewModel) {
    val downloading by vm.isDownloading.collectAsState()
    val progress by vm.initProgress.collectAsState()
    val error by vm.initError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        GradientHero {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(AtharTheme.extra.gold.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.MenuBook,
                        contentDescription = null,
                        tint = AtharTheme.extra.gold,
                        modifier = Modifier.size(44.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "القرآن الكريم",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AtharTheme.extra.onHero,
                )
                Text(
                    text = "المصحف كاملاً بخط عثماني — تنزيل لمرة واحدة",
                    style = MaterialTheme.typography.bodySmall,
                    color = AtharTheme.extra.onHeroMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        AtharCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "لتصفّح المصحف دون الحاجة للإنترنت لاحقاً، سنقوم بتنزيل نص القرآن الكريم (٦٢٣٦ آية) وحفظه على جهازك. الحجم صغير ولا يستغرق سوى لحظات.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Justify,
            )
            Spacer(Modifier.height(16.dp))

            if (downloading) {
                ProgressBar(progress = progress)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "جارٍ التنزيل… ${toArabicDigits((progress * 100).toInt())}٪",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                PillButton(
                    text = "تنزيل المصحف الآن",
                    icon = Icons.Filled.CloudDownload,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { vm.startDatabaseInitialization() },
                )
            }

            if (error != null) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = error ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

// ---------------------------------------------------------------------------
// الفهرس
// ---------------------------------------------------------------------------

@Composable
private fun QuranIndexScreen(vm: QuranViewModel) {
    val context = LocalContext.current
    val surahs by vm.allSurahs.collectAsState()
    val tab by vm.indexTab.collectAsState()
    val lastPage by vm.currentPage.collectAsState()
    val query by vm.searchQuery.collectAsState()
    val results by vm.searchResults.collectAsState()
    val searching by vm.isSearching.collectAsState()
    val bookmarks by vm.bookmarkedAyahs.collectAsState()
    val favorites by vm.favoriteAyahs.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        AtharTopBar(
            title = "القرآن الكريم",
            subtitle = "١١٤ سورة · ٦٠٤ صفحات · ٣٠ جزءاً",
        )

        // البحث
        OutlinedTextField(
            value = query,
            onValueChange = { vm.onSearchQueryChange(it) },
            placeholder = { Text("ابحث في آيات القرآن…") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            leadingIcon = {
                IconButton(onClick = { vm.search() }) {
                    Icon(Icons.Filled.Search, contentDescription = "بحث")
                }
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { vm.clearSearch() }) {
                        Icon(Icons.Filled.Close, contentDescription = "مسح")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.search() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )

        if (query.trim().length >= 2) {
            SearchResults(vm = vm, searching = searching, resultsCount = results.size)
            return@Column
        }

        // متابعة القراءة
        AtharCard(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            border = null,
            onClick = { vm.openReader(lastPage) },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "متابعة القراءة",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "صفحة ${toArabicDigits(lastPage)} · الجزء ${toArabicDigits(QuranViewModel.juzOfPage(lastPage))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        // التبويبات
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChoiceChip("السور", tab == IndexTab.SURAH) { vm.indexTab.value = IndexTab.SURAH }
            ChoiceChip("الأجزاء", tab == IndexTab.JUZ) { vm.indexTab.value = IndexTab.JUZ }
            ChoiceChip("الصفحات", tab == IndexTab.PAGE) { vm.indexTab.value = IndexTab.PAGE }
            ChoiceChip("المحفوظات", tab == IndexTab.SAVED) { vm.indexTab.value = IndexTab.SAVED }
        }

        Spacer(Modifier.height(8.dp))

        when (tab) {
            IndexTab.SURAH -> SurahList(surahs = surahs, vm = vm, context = context)
            IndexTab.JUZ -> JuzGrid(vm = vm)
            IndexTab.PAGE -> PageGrid(vm = vm)
            IndexTab.SAVED -> SavedList(vm = vm, bookmarksCount = bookmarks.size, favoritesCount = favorites.size)
        }
    }
}

@Composable
private fun SearchResults(vm: QuranViewModel, searching: Boolean, resultsCount: Int) {
    val results by vm.searchResults.collectAsState()
    if (searching) {
        LoadingState("جارٍ البحث…", modifier = Modifier.fillMaxSize())
        return
    }
    if (resultsCount == 0) {
        EmptyState(
            icon = Icons.Filled.Search,
            title = "لا توجد نتائج",
            message = "اضغط زرّ البحث بعد كتابة كلمة من الآية.",
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "${toArabicDigits(resultsCount)} نتيجة",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(results, key = { it.number }) { ayah ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .clickable { vm.openAyah(ayah) }
                            .padding(14.dp),
                    ) {
                        Text(
                            text = ayah.text,
                            style = quranTextStyle(19f),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${vm.surahOf(ayah.surahNumber)?.name ?: ""} · آية ${toArabicDigits(ayah.numberInSurah)} · صفحة ${toArabicDigits(ayah.page)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SurahList(surahs: List<SurahModel>, vm: QuranViewModel, context: Context) {
    if (surahs.isEmpty()) {
        LoadingState(modifier = Modifier.fillMaxSize())
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(surahs, key = { it.number }) { surah ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .clickable { vm.openSurah(surah) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(AtharTheme.extra.gold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = toArabicDigits(surah.number),
                            style = MaterialTheme.typography.labelLarge,
                            color = AtharTheme.extra.gold,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = surah.name,
                            style = thikrTextStyle(21f),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        Text(
                            text = "${surah.revelationType} · ${toArabicDigits(surah.numberOfVerses)} آية",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { playSurahViaService(context, surah.number) }) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "استماع",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JuzGrid(vm: QuranViewModel) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items((1..30).toList().chunked(2)) { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { juz ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable { vm.openJuz(juz) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = toArabicDigits(juz),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("الجزء ${toArabicDigits(juz)}", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "صفحة ${toArabicDigits(QuranViewModel.juzStartPage(juz))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PageGrid(vm: QuranViewModel) {
    val pages = remember { (1..QuranViewModel.TOTAL_PAGES).toList().chunked(5) }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(pages) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { page ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { vm.openReader(page) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = toArabicDigits(page),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun SavedList(vm: QuranViewModel, bookmarksCount: Int, favoritesCount: Int) {
    val bookmarks by vm.bookmarkedAyahs.collectAsState()
    val favorites by vm.favoriteAyahs.collectAsState()

    if (bookmarksCount == 0 && favoritesCount == 0) {
        EmptyState(
            icon = Icons.Filled.Bookmark,
            title = "لا توجد محفوظات بعد",
            message = "اضغط على أي آية أثناء القراءة لإضافتها إلى العلامات أو المفضلة.",
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (bookmarks.isNotEmpty()) {
            item {
                Text(
                    text = "العلامات المرجعية",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            itemsIndexed(bookmarks, key = { _, a -> "b${a.number}" }) { _, ayah ->
                SavedAyahRow(
                    text = ayah.text,
                    meta = "${vm.surahOf(ayah.surahNumber)?.name ?: ""} · آية ${toArabicDigits(ayah.numberInSurah)}",
                    icon = Icons.Filled.Bookmark,
                    onClick = { vm.openAyah(ayah) },
                    onRemove = { vm.toggleBookmark(ayah) },
                )
            }
        }
        if (favorites.isNotEmpty()) {
            item {
                Text(
                    text = "الآيات المفضّلة",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            itemsIndexed(favorites, key = { _, a -> "f${a.number}" }) { _, ayah ->
                SavedAyahRow(
                    text = ayah.text,
                    meta = "${vm.surahOf(ayah.surahNumber)?.name ?: ""} · آية ${toArabicDigits(ayah.numberInSurah)}",
                    icon = Icons.Filled.Star,
                    onClick = { vm.openAyah(ayah) },
                    onRemove = { vm.toggleFavorite(ayah) },
                )
            }
        }
    }
}

@Composable
private fun SavedAyahRow(
    text: String,
    meta: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .clickable { onClick() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = AtharTheme.extra.gold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = quranTextStyle(18f),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "إزالة",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// أدوات مشتركة
// ---------------------------------------------------------------------------

fun playSurahViaService(context: Context, surahNumber: Int) {
    val intent = Intent(context, QuranAudioService::class.java).apply {
        action = QuranAudioService.ACTION_PLAY_SURAH
        putExtra(QuranAudioService.EXTRA_SURAH_NUMBER, surahNumber)
    }
    context.startService(intent)
}

fun sendAudioAction(context: Context, audioAction: String) {
    val intent = Intent(context, QuranAudioService::class.java).apply {
        action = audioAction
    }
    context.startService(intent)
}

fun seekAudioTo(context: Context, positionMs: Long) {
    val intent = Intent(context, QuranAudioService::class.java).apply {
        action = QuranAudioService.ACTION_SEEK_TO
        putExtra(QuranAudioService.EXTRA_SEEK_POSITION, positionMs)
    }
    context.startService(intent)
}

@Composable
internal fun QuranMiniLoader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(26.dp),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
