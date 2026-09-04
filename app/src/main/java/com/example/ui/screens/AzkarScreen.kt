package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.AtharViewModel
import com.example.data.Thikr
import com.example.ui.components.AtharTopBar
import com.example.ui.components.ChoiceChip
import com.example.ui.components.EmptyState
import com.example.ui.components.LoadingState
import com.example.ui.components.ProgressBar
import com.example.ui.components.toArabicDigits
import com.example.ui.theme.AtharTheme
import com.example.ui.theme.thikrTextStyle

/* ---------------------------------------------------------------------------
 *  حصن المسلم — الأذكار
 * ------------------------------------------------------------------------- */

@Composable
fun AzkarScreen(vm: AtharViewModel) {
    val context = LocalContext.current
    val thikrs by vm.allThikrs.collectAsState()
    val order by vm.customCategoryOrder.collectAsState()
    val selected by vm.selectedCategory.collectAsState()
    val counts by vm.thikrCounts.collectAsState()
    val favorites by vm.favorites.collectAsState()

    var showReorder by remember { mutableStateOf(false) }

    val categories = remember(thikrs, order) { vm.getCategories() }
    val current = selected ?: categories.firstOrNull()
    val items = remember(thikrs, current) {
        thikrs.filter { it.category == current }.sortedBy { it.order }
    }

    val done = items.count { (counts[it.id] ?: it.count) == 0 }
    val progress = if (items.isEmpty()) 0f else done.toFloat() / items.size.toFloat()

    Column(modifier = Modifier.fillMaxSize()) {
        AtharTopBar(
            title = "حصن المسلم",
            subtitle = current?.let { "$it · ${toArabicDigits(items.size)} ذكر" },
            actions = {
                IconButton(onClick = { showReorder = true }) {
                    Icon(Icons.Filled.SwapVert, contentDescription = "ترتيب الأقسام")
                }
                IconButton(onClick = { vm.resetSelectedCategoryCounts() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "تصفير العدّادات")
                }
            },
        )

        if (thikrs.isEmpty()) {
            LoadingState("جارٍ تحميل الأذكار…", modifier = Modifier.fillMaxSize())
            return@Column
        }

        // أقسام الأذكار
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(categories) { cat ->
                ChoiceChip(
                    text = cat,
                    selected = cat == current,
                    onClick = { vm.selectCategory(cat) },
                )
            }
        }

        // شريط التقدّم
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProgressBar(progress = progress, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            Text(
                text = "${toArabicDigits(done)}/${toArabicDigits(items.size)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (items.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Favorite,
                title = "لا توجد أذكار في هذا القسم",
                message = "اختر قسماً آخر من الأعلى.",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = { it.id }) { thikr ->
                    ThikrCard(
                        thikr = thikr,
                        remaining = counts[thikr.id] ?: thikr.count,
                        isFavorite = favorites.contains(thikr.id),
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

    if (showReorder) {
        ReorderCategoriesDialog(
            categories = categories,
            onMove = { from, to -> vm.moveCategory(from, to) },
            onReset = { vm.resetCategoryOrder() },
            onDismiss = { showReorder = false },
        )
    }
}

@Composable
fun ThikrCard(
    thikr: Thikr,
    remaining: Int,
    isFavorite: Boolean,
    onTap: () -> Unit,
    onReset: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
) {
    val isDone = remaining == 0
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (isDone) MaterialTheme.colorScheme.surfaceContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .clickable(enabled = !isDone) { onTap() }
                .padding(18.dp),
        ) {
            Text(
                text = thikr.text.trim(),
                style = thikrTextStyle(20f),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Justify,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isDone) 0.45f else 1f),
            )

            if ((thikr.virtue.isNotBlank() || thikr.reference.isNotBlank()) && expanded) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (thikr.virtue.isNotBlank()) {
                            Text(
                                text = "الفضل: ${thikr.virtue}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (thikr.reference.isNotBlank()) {
                            Text(
                                text = "المصدر: ${thikr.reference}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // العدّاد
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDone) AtharTheme.extra.success.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.primary,
                        )
                        .clickable { if (isDone) onReset() else onTap() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDone) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "تم",
                            tint = AtharTheme.extra.success,
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        Text(
                            text = toArabicDigits(remaining),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isDone) "اكتمل الذكر" else "اضغط للعدّ",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isDone) AtharTheme.extra.success else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (thikr.virtue.isNotBlank() || thikr.reference.isNotBlank()) {
                        Text(
                            text = if (expanded) "إخفاء التفاصيل" else "عرض الفضل والمصدر",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { expanded = !expanded }
                                .padding(vertical = 2.dp),
                        )
                    }
                }

                IconButton(onClick = onFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "المفضلة",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onCopy) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "نسخ",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = "مشاركة",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AnimatedVisibility(visible = isDone) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "إعادة العدّ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onReset() }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReorderCategoriesDialog(
    categories: List<String>,
    onMove: (Int, Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ترتيب الأقسام", style = MaterialTheme.typography.titleMedium) },
        text = {
            LazyColumn(modifier = Modifier.height(360.dp)) {
                itemsIndexed(categories) { index, cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { if (index > 0) onMove(index, index - 1) },
                            enabled = index > 0,
                        ) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = "أعلى")
                        }
                        IconButton(
                            onClick = { if (index < categories.lastIndex) onMove(index, index + 1) },
                            enabled = index < categories.lastIndex,
                        ) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = "أسفل")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("تم") }
        },
        dismissButton = {
            TextButton(onClick = onReset) { Text("الترتيب الافتراضي") }
        },
    )
}

// ---------------------------------------------------------------------------
// أدوات المشاركة والنسخ
// ---------------------------------------------------------------------------

fun shareText(context: Context, text: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "$text\n\n— من تطبيق أثر")
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة"))
    } catch (e: Exception) {
        Toast.makeText(context, "تعذّرت المشاركة", Toast.LENGTH_SHORT).show()
    }
}

fun copyText(context: Context, text: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("أثر", text))
        Toast.makeText(context, "تم النسخ", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "تعذّر النسخ", Toast.LENGTH_SHORT).show()
    }
}
