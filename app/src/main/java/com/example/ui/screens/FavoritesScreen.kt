package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.AtharTab
import com.example.data.AtharViewModel
import com.example.features.quran.presentation.QuranViewModel
import com.example.ui.components.AtharTopBar
import com.example.ui.components.ChoiceChip
import com.example.ui.components.EmptyState
import com.example.ui.components.toArabicDigits
import com.example.ui.theme.AtharTheme
import com.example.ui.theme.quranTextStyle

/* ---------------------------------------------------------------------------
 *  المفضلة: أذكار محفوظة + آيات محفوظة
 * ------------------------------------------------------------------------- */

@Composable
fun FavoritesScreen(
    vm: AtharViewModel,
    quranVm: QuranViewModel,
    onBack: () -> Unit,
    onNavigate: (AtharTab) -> Unit,
) {
    val allThikrs by vm.allThikrs.collectAsState()
    val favoriteIds by vm.favorites.collectAsState()
    val counts by vm.thikrCounts.collectAsState()
    val favoriteAyahs by quranVm.favoriteAyahs.collectAsState()
    val bookmarkedAyahs by quranVm.bookmarkedAyahs.collectAsState()

    var tabIndex by remember { mutableStateOf(0) }
    val context = LocalContext.current

    val favoriteThikrs = remember(allThikrs, favoriteIds) {
        allThikrs.filter { favoriteIds.contains(it.id) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AtharTopBar(
            title = "المفضلة",
            subtitle = "${toArabicDigits(favoriteThikrs.size)} ذكر · ${toArabicDigits(favoriteAyahs.size)} آية",
            onBack = onBack,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChoiceChip(text = "الأذكار", selected = tabIndex == 0) { tabIndex = 0 }
            ChoiceChip(text = "الآيات", selected = tabIndex == 1) { tabIndex = 1 }
            ChoiceChip(text = "العلامات", selected = tabIndex == 2) { tabIndex = 2 }
        }

        when (tabIndex) {
            0 -> {
                if (favoriteThikrs.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.StarBorder,
                        title = "لا توجد أذكار مفضّلة",
                        message = "اضغط على النجمة في أي ذكر لإضافته هنا.",
                        modifier = Modifier.fillMaxSize(),
                        actionLabel = "تصفّح الأذكار",
                        onAction = { onNavigate(AtharTab.AZKAR) },
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(favoriteThikrs, key = { it.id }) { thikr ->
                            ThikrCard(
                                thikr = thikr,
                                remaining = counts[thikr.id] ?: thikr.count,
                                isFavorite = true,
                                onTap = { vm.decrementThikrCount(thikr.id) },
                                onReset = { vm.resetThikrCount(thikr.id) },
                                onFavorite = { vm.toggleFavorite(thikr.id) },
                                onShare = { shareText(context, thikr.text) },
                                onCopy = { copyText(context, thikr.text) },
                            )
                        }
                    }
                }
            }

            1 -> AyahFavoriteList(
                ayahs = favoriteAyahs,
                quranVm = quranVm,
                emptyTitle = "لا توجد آيات مفضّلة",
                emptyMessage = "افتح المصحف واضغط على أي آية ثم اختر «مفضلة».",
                onNavigate = onNavigate,
                isBookmarkList = false,
            )

            else -> AyahFavoriteList(
                ayahs = bookmarkedAyahs,
                quranVm = quranVm,
                emptyTitle = "لا توجد علامات مرجعية",
                emptyMessage = "ضع علامة على أي آية للرجوع إليها بسرعة.",
                onNavigate = onNavigate,
                isBookmarkList = true,
            )
        }
    }
}

@Composable
private fun AyahFavoriteList(
    ayahs: List<com.example.features.quran.domain.AyahModel>,
    quranVm: QuranViewModel,
    emptyTitle: String,
    emptyMessage: String,
    onNavigate: (AtharTab) -> Unit,
    isBookmarkList: Boolean,
) {
    val context = LocalContext.current

    if (ayahs.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.MenuBook,
            title = emptyTitle,
            message = emptyMessage,
            modifier = Modifier.fillMaxSize(),
            actionLabel = "فتح المصحف",
            onAction = { onNavigate(AtharTab.QURAN) },
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(ayahs, key = { it.number }) { ayah ->
            val surahName = quranVm.surahOf(ayah.surahNumber)?.name ?: ""
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        quranVm.openAyah(ayah)
                        onNavigate(AtharTab.QURAN)
                    },
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "سورة $surahName",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "الآية ${toArabicDigits(ayah.numberInSurah)} · صفحة ${toArabicDigits(ayah.page)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = ayah.text,
                        style = quranTextStyle(21f),
                        color = AtharTheme.extra.onPaper,
                        textAlign = TextAlign.Justify,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { copyText(context, "${ayah.text}\n[$surahName: ${ayah.numberInSurah}]") }) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = "نسخ",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        IconButton(onClick = { shareText(context, "${ayah.text}\n[$surahName: ${ayah.numberInSurah}]") }) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = "مشاركة",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        IconButton(onClick = {
                            if (isBookmarkList) quranVm.toggleBookmark(ayah) else quranVm.toggleFavorite(ayah)
                        }) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = "إزالة",
                                tint = AtharTheme.extra.gold,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
