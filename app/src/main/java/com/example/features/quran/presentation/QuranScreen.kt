package com.example.features.quran.presentation

import android.app.Activity
import android.content.ContextWrapper
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import android.os.Build
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.features.quran.domain.AyahModel
import com.example.features.quran.domain.SurahModel
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IslamicGreenPrimary
import com.example.ui.theme.IslamicGreenSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Media player instance to play real audio of Alafasy
private var mediaPlayer: MediaPlayer? = null
private var currentPlayingAyahNumber: Int? = null

val LocalQuranFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Serif }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScreen(viewModel: QuranViewModel) {
    val isInitialized by viewModel.isInitialized.collectAsState()
    val initProgress by viewModel.initProgress.collectAsState()
    val initError by viewModel.initError.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()

    val context = LocalContext.current
    val quranFontFamily = remember(context) {
        try {
            val typeface = android.graphics.Typeface.createFromAsset(context.assets, "fonts/scheherazade_new.ttf")
            FontFamily(typeface)
        } catch (e: Exception) {
            Log.e("QuranScreen", "Error loading Scheherazade font from assets: ${e.message}", e)
            FontFamily.Serif
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalQuranFontFamily provides quranFontFamily
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (!isInitialized) {
                QuranSetupScreen(
                    progress = initProgress,
                    error = initError,
                    onStart = { viewModel.startDatabaseInitialization() }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (viewMode) {
                        QuranViewMode.INDEX -> QuranIndexScreen(viewModel)
                        QuranViewMode.READER -> QuranReaderScreen(viewModel)
                        QuranViewMode.FAVORITES -> QuranFavoritesScreen(viewModel)
                    }
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        FloatingSurahPlayerBar(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun QuranSetupScreen(
    progress: Float,
    error: String?,
    onStart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "القرآن الكريم",
                    tint = IslamicGreenPrimary,
                    modifier = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "تهيئة المصحف الشريف",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = IslamicGreenPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "عند أول تشغيل، سنقوم بتحميل النص القرآني العثماني بالكامل وبناء قاعدة البيانات المحلية لتمكين القراءة والبحث دون اتصال بالإنترنت مدى الحياة.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (progress > 0f) {
                    val progressPercent = (progress * 100).toInt()
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = IslamicGreenPrimary,
                        trackColor = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "جاري البناء: $progressPercent%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGreenPrimary
                    )
                } else {
                    Button(
                        onClick = onStart,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                    ) {
                        Text(
                            text = "تنزيل وإعداد المصحف الآن",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                error?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranIndexScreen(viewModel: QuranViewModel) {
    val surahs by viewModel.allSurahs.collectAsState()
    val readerTab by viewModel.readerTab.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val bookmarkedAyahs by viewModel.bookmarkedAyahs.collectAsState()
    val quranResults by viewModel.quranSearchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentSurahAudio by SurahPlaybackManager.currentSurah.collectAsState()
    val isPlayingAudio by SurahPlaybackManager.isPlaying.collectAsState()
    val isBufferingAudio by SurahPlaybackManager.isBuffering.collectAsState()
    val downloadProgressMap by SurahPlaybackManager.downloadProgress.collectAsState()
    val downloadedSurahsSet by SurahPlaybackManager.downloadedSurahs.collectAsState()

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "الرجاء السماح بالإشعارات للتمكن من التحكم بالتلاوة في الخلفية", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(searchQuery) {
        viewModel.performQuranSearch(searchQuery)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "المصحف الشريف",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.viewMode.value = QuranViewMode.FAVORITES }) {
                        Icon(Icons.Default.Favorite, contentDescription = "المفضلة", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = IslamicGreenPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Quick Continue Reading Card
            if (bookmarkedAyahs.isNotEmpty()) {
                val lastBookmark = bookmarkedAyahs.last()
                val surahName = surahs.find { it.number == lastBookmark.surahNumber }?.name ?: ""
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.readerTab.value = ReaderTab.PAGE
                            viewModel.selectPage(lastBookmark.page)
                        },
                    colors = CardDefaults.cardColors(containerColor = IslamicGreenPrimary.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp),
                    border = borderLight()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "متابعة القراءة",
                                fontSize = 12.sp,
                                color = IslamicGreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${formatSurahName(surahName, withTashkeel = false)} (آية ${lastBookmark.numberInSurah})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "صفحة ${lastBookmark.page} • الجزء ${lastBookmark.juz}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }



            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث في المصحف أو السور...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = IslamicGreenPrimary) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IslamicGreenPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (searchQuery.isNotEmpty()) {
                if (isSearching) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = IslamicGreenPrimary)
                    }
                } else if (quranResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("لا توجد نتائج مطابقة لبحثك.", color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    Text(
                        text = "نتائج البحث في الآيات (${quranResults.size}):",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGreenPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        items(quranResults) { ayah ->
                            val surahName = surahs.find { it.number == ayah.surahNumber }?.name ?: ""
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectAyah(ayah) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = ayah.text,
                                        fontSize = 16.sp,
                                        fontFamily = LocalQuranFontFamily.current,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "صفحة ${ayah.page}",
                                            fontSize = 12.sp,
                                            color = Color(0xFFC5A880)
                                        )
                                        Text(
                                            text = "سورة $surahName • آية ${ayah.numberInSurah}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFC5A880)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Navigation Tabs
                var selectedTabIndex by remember { mutableStateOf(0) }
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = IslamicGreenPrimary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("السور", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        selectedContentColor = IslamicGreenPrimary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("الصفحات", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        selectedContentColor = IslamicGreenPrimary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = { Text("الأجزاء", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        selectedContentColor = IslamicGreenPrimary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Lists based on selected tab
                when (selectedTabIndex) {
                    0 -> {
                        val filteredSurahs = surahs.filter {
                            it.name.contains(searchQuery) ||
                                    it.englishName.contains(searchQuery, ignoreCase = true) ||
                                    it.number.toString() == searchQuery
                        }
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            items(filteredSurahs) { surah ->
                                val isCurrentlyPlaying = currentSurahAudio?.number == surah.number
                                val isCurrentlyBuffering = isCurrentlyPlaying && isBufferingAudio
                                val downloadProgress = downloadProgressMap[surah.number]
                                val isDownloaded = downloadedSurahsSet.contains(surah.number)

                                SurahIndexItem(
                                    surah = surah,
                                    onClick = { viewModel.selectSurah(surah) },
                                    onPlayClick = {
                                        if (isCurrentlyPlaying) {
                                            val action = if (isPlayingAudio) QuranAudioService.ACTION_PAUSE else QuranAudioService.ACTION_RESUME
                                            val playPauseIntent = Intent(context, QuranAudioService::class.java).apply {
                                                this.action = action
                                            }
                                            context.startService(playPauseIntent)
                                        } else {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                            }
                                            val playIntent = Intent(context, QuranAudioService::class.java).apply {
                                                action = QuranAudioService.ACTION_PLAY_SURAH
                                                putExtra(QuranAudioService.EXTRA_SURAH_NUMBER, surah.number)
                                            }
                                            context.startService(playIntent)
                                        }
                                    },
                                    onDownloadClick = {
                                        SurahPlaybackManager.startDownload(context, surah.number, SurahPlaybackManager.currentReciter.value.id, scope = coroutineScope)
                                    },
                                    onDeleteClick = {
                                        SurahPlaybackManager.deleteDownload(context, surah.number, SurahPlaybackManager.currentReciter.value.id)
                                    },
                                    isCurrentlyPlaying = isCurrentlyPlaying,
                                    isBuffering = isCurrentlyBuffering,
                                    downloadProgress = downloadProgress,
                                    isDownloaded = isDownloaded
                                )
                            }
                        }
                    }
                    1 -> {
                        // Grid of Pages (1 to 604)
                        val pagesList = (1..604).toList().filter { it.toString().contains(searchQuery) }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            items(pagesList) { pageNum ->
                                PageGridItem(pageNum = pageNum, onClick = { viewModel.selectPage(pageNum) })
                            }
                        }
                    }
                    2 -> {
                        // List of Juz (1 to 30)
                        val juzList = (1..30).toList().filter { "جزء $it".contains(searchQuery) || it.toString().contains(searchQuery) }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            items(juzList) { juzNum ->
                                JuzGridItem(juzNum = juzNum, onClick = { viewModel.selectJuz(juzNum) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SurahIndexItem(
    surah: SurahModel,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isCurrentlyPlaying: Boolean,
    isBuffering: Boolean,
    downloadProgress: Float?,
    isDownloaded: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        border = borderLight()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Surah Number
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(IslamicGreenPrimary.copy(alpha = 0.08f), CircleShape)
                        .border(1.dp, IslamicGreenPrimary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = surah.number.toString(),
                        color = IslamicGreenPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = formatSurahName(surah.name, withTashkeel = false),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGreenPrimary
                    )
                    Text(
                        text = "${surah.revelationType} • آياتها ${surah.numberOfVerses}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Play/Pause Action Button
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isCurrentlyPlaying) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = IslamicGreenPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "إيقاف مؤقت",
                                tint = IslamicGreenPrimary
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "تشغيل السورة",
                            tint = IslamicGreenPrimary
                        )
                    }
                }

                // Download/Delete Action Button
                if (downloadProgress != null) {
                    // Downloading State
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp)) {
                        CircularProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 2.dp,
                            color = GoldAccent
                        )
                        Text(
                            text = "${(downloadProgress * 100).toInt()}",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else if (isDownloaded) {
                    // Downloaded State
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "محفوظة محلياً",
                            tint = IslamicGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف السورة المحملة",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    // Not Downloaded State
                    IconButton(
                        onClick = onDownloadClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "تنزيل السورة",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PageGridItem(pageNum: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = borderLight()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = pageNum.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = IslamicGreenPrimary
                )
                Text(
                    text = "صفحة",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun JuzGridItem(juzNum: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = borderLight()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "الجزء $juzNum",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = IslamicGreenPrimary
                )
                Text(
                    text = "ص ${QuranViewModel.getJuzStartPage(juzNum)}",
                    fontSize = 11.sp,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun formatSurahName(name: String, withTashkeel: Boolean = true): String {
    if (name.isBlank()) return ""
    val cleanName = name
        .replace("سُورَةُ", "")
        .replace("سُورَةِ", "")
        .replace("سُورَةَ", "")
        .replace("سُورَة", "")
        .replace("سورة", "")
        .trim()
    return if (withTashkeel) "سُورَةُ $cleanName" else "سورة $cleanName"
}

fun getJuzNameArabic(juz: Int): String {
    return when (juz) {
        1 -> "الجزء الأول"
        2 -> "الجزء الثاني"
        3 -> "الجزء الثالث"
        4 -> "الجزء الرابع"
        5 -> "الجزء الخامس"
        6 -> "الجزء السادس"
        7 -> "الجزء السابع"
        8 -> "الجزء الثامن"
        9 -> "الجزء التاسع"
        10 -> "الجزء العاشر"
        11 -> "الجزء الحادي عشر"
        12 -> "الجزء الثاني عشر"
        13 -> "الجزء الثالث عشر"
        14 -> "الجزء الرابع عشر"
        15 -> "الجزء الخامس عشر"
        16 -> "الجزء السادس عشر"
        17 -> "الجزء السابع عشر"
        18 -> "الجزء الثامن عشر"
        19 -> "الجزء التاسع عشر"
        20 -> "الجزء العشرون"
        21 -> "الجزء الحادي والعشرون"
        22 -> "الجزء الثاني والعشرون"
        23 -> "الجزء الثالث والعشرون"
        24 -> "الجزء الرابع والعشرون"
        25 -> "الجزء الخامس والعشرون"
        26 -> "الجزء السادس والعشرون"
        27 -> "الجزء السابع والعشرون"
        28 -> "الجزء الثامن والعشرون"
        29 -> "الجزء التاسع والعشرون"
        30 -> "الجزء الثلاثون"
        else -> "الجزء $juz"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(viewModel: QuranViewModel) {
    val page by viewModel.selectedPage.collectAsState()
    val fontSizeState by viewModel.fontSize.collectAsState()
    val surahs by viewModel.allSurahs.collectAsState()
    val highlightedAyahNumber by viewModel.highlightedAyahNumber.collectAsState()

    var selectedAyahForSheet by remember { mutableStateOf<AyahModel?>(null) }
    var showPageTafsirSheet by remember { mutableStateOf(false) }
    val isFullScreen by viewModel.isFullScreen.collectAsState()
    var isPlayingState by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val pagerState = rememberPagerState(
        initialPage = (page - 1).coerceIn(0, 603),
        pageCount = { 604 }
    )

    // Sync pager page with viewmodel selectedPage
    LaunchedEffect(pagerState.currentPage) {
        val newPage = pagerState.currentPage + 1
        if (newPage != page) {
            viewModel.saveLastPage(newPage)
        }
    }

    // Sync selectedPage with pager page
    LaunchedEffect(page) {
        if (pagerState.currentPage + 1 != page) {
            pagerState.scrollToPage(page - 1)
        }
    }

    // Monitor current playing audio
    LaunchedEffect(currentPlayingAyahNumber) {
        isPlayingState = currentPlayingAyahNumber != null
    }

    // Manage True Full Screen (Immersive Mode) dynamically
    DisposableEffect(isFullScreen) {
        val activity = context as? Activity ?: (context as? ContextWrapper)?.baseContext as? Activity
        activity?.let { act ->
            val window = act.window
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullScreen) {
                // Hide both status bar and system navigation bar
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                // Enable immersive behavior (show transient bars on swipe)
                windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                // Show status bar and system navigation bar again
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            // Guarantee restoration of system bars when leaving the reader screen
            val activity = context as? Activity ?: (context as? ContextWrapper)?.baseContext as? Activity
            activity?.let { act ->
                val window = act.window
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0C140F)
    val readerBg = if (isDark) Color(0xFF121212) else Color(0xFFFBF7EE)
    val readerTextColor = if (isDark) Color(0xFFE0E0E0) else Color(0xFF2C2620)
    val footerColor = if (isDark) Color(0xFFC5A880) else Color(0xFF8C7355)
    val borderCol = if (isDark) Color(0xFF333333) else Color(0xFFC5A880).copy(alpha = 0.4f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readerBg)
    ) {
        // Main Horizontal Pager for Page-by-Page reading
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val pageNum = pageIndex + 1
            
            // Dynamic loader for each page (high performance lazy loading)
            val pageAyahsState = remember(pageNum) {
                viewModel.repository.getAyahsByPageFlow(pageNum)
            }.collectAsState(initial = emptyList())
            
            val ayahsOnPage = pageAyahsState.value

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = { viewModel.isFullScreen.value = !isFullScreen }
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .border(0.5.dp, borderCol, RoundedCornerShape(6.dp))
                    .padding(12.dp)
            ) {
                if (ayahsOnPage.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF425648))
                    }
                } else {
                    val firstAyah = ayahsOnPage.firstOrNull()
                    val surahName = firstAyah?.let { surahs.find { s -> s.number == it.surahNumber }?.name } ?: ""
                    val juzNum = firstAyah?.juz ?: 1
                    val formattedSurah = formatSurahName(surahName)
                    val formattedJuz = getJuzNameArabic(juzNum)
                    
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 1. Top Header (اسم السورة ورقم الجزء)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formattedSurah,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = footerColor
                            )
                            Text(
                                text = formattedJuz,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = footerColor
                            )
                        }

                        // 2. Central Content (Scrollable & Adaptive to Surah Headers)
                        val hasSurahHeader = ayahsOnPage.any { it.numberInSurah == 1 }
                        val adjustedFontSize = if (hasSurahHeader) (fontSizeState * 0.85f).coerceAtLeast(14f) else fontSizeState

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val groupedAyahs = ayahsOnPage.groupBy { it.surahNumber }
                            
                            groupedAyahs.forEach { (surahNumber, surahAyahs) ->
                                val surahModel = surahs.find { it.number == surahNumber }
                                val firstInGroup = surahAyahs.firstOrNull()
                                
                                // Render Surah Title Banner if this Surah begins on this page
                                if (firstInGroup != null && firstInGroup.numberInSurah == 1) {
                                    SurahHeaderBanner(
                                        name = surahModel?.name ?: "",
                                        revelationType = surahModel?.revelationType ?: "",
                                        numberOfVerses = surahModel?.numberOfVerses ?: 0,
                                        fontSize = adjustedFontSize,
                                        isDark = isDark
                                    )
                                    
                                    // Show Bismillah if appropriate
                                    if (surahNumber != 1 && surahNumber != 9) {
                                        BismillahBanner(fontSize = adjustedFontSize, isDark = isDark)
                                    }
                                }

                                // Render the continuous paragraph text of the Surah's ayahs
                                val annotatedString = buildAnnotatedString {
                                    surahAyahs.forEach { ayah ->
                                        val start = length
                                        val cleanedText = cleanAyahText(ayah.text, ayah.numberInSurah == 1, ayah.surahNumber)
                                        append(cleanedText)
                                        
                                        // End of Ayah marker inside standard styled bracket ornaments
                                        val numStart = length
                                        append(" ﴿${ayah.numberInSurah}﴾ ")
                                        val numEnd = length
                                        val end = length

                                        if (ayah.number == highlightedAyahNumber) {
                                            addStyle(
                                                style = SpanStyle(
                                                    background = if (isDark) Color(0xFFC5A880).copy(alpha = 0.35f) else Color(0xFFFFF176).copy(alpha = 0.6f),
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                start = start,
                                                end = end
                                            )
                                        }
                                        
                                        addStyle(
                                            style = SpanStyle(
                                                color = Color(0xFFC5A880),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = (adjustedFontSize - 3f).coerceAtLeast(12f).sp
                                            ),
                                            start = numStart,
                                            end = numEnd
                                        )
                                        
                                        addStringAnnotation(
                                            tag = "ayah_click",
                                            annotation = ayah.number.toString(),
                                            start = start,
                                            end = end
                                        )
                                    }
                                }

                                ClickableText(
                                    text = annotatedString,
                                    style = TextStyle(
                                        fontSize = adjustedFontSize.sp,
                                        fontFamily = LocalQuranFontFamily.current,
                                        lineHeight = (adjustedFontSize * 1.8f).sp,
                                        textAlign = TextAlign.Center,
                                        color = readerTextColor
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    onClick = { offset ->
                                        annotatedString.getStringAnnotations(tag = "ayah_click", start = offset, end = offset)
                                            .firstOrNull()?.let { annotation ->
                                                val clickedAyahNum = annotation.item.toIntOrNull()
                                                if (clickedAyahNum != null) {
                                                    val clickedAyah = surahAyahs.find { it.number == clickedAyahNum }
                                                    if (clickedAyah != null) {
                                                        selectedAyahForSheet = clickedAyah
                                                    }
                                                }
                                            }
                                    }
                                )
                            }
                        }

                        // 3. Beautiful Page Footer (رقم الصفحة فقط في المنتصف)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pageNum.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = footerColor
                            )
                        }
                    }
                }
            }
        }

        // FLOATING OVERLAY: Top bar controls (disappears in Full-Screen mode)
        AnimatedVisibility(
            visible = !isFullScreen,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = Color(0xFF425648), // Dark Green Theme top bar
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { 
                                viewModel.viewMode.value = QuranViewMode.INDEX 
                                viewModel.isFullScreen.value = false // clear full screen mode
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "المصحف الشريف",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Index Button in place of Tafsir
                        IconButton(
                            onClick = { 
                                viewModel.viewMode.value = QuranViewMode.INDEX
                                viewModel.isFullScreen.value = false // clear full screen mode
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.List, contentDescription = "الفهرس", tint = Color.White)
                        }
                    }
                }
            }
        }

        // FLOATING OVERLAY: Bottom page controllers (disappears in Full-Screen mode)
        AnimatedVisibility(
            visible = !isFullScreen,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = if (isDark) Color(0xFF1E1E1E) else Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        enabled = page > 1,
                        onClick = { viewModel.selectPage(page - 1) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronRight, 
                            contentDescription = "الصفحة السابقة", 
                            tint = if (isDark) Color.White else Color(0xFF425648)
                        )
                    }

                    Text(
                        text = "صفحة $page من 604",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF425648)
                    )

                    IconButton(
                        enabled = page < 604,
                        onClick = { viewModel.selectPage(page + 1) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft, 
                            contentDescription = "الصفحة التالية", 
                            tint = if (isDark) Color.White else Color(0xFF425648)
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for clicked Ayah (Strict minimalist requirement)
    if (selectedAyahForSheet != null) {
        var isPlaying by remember { mutableStateOf(currentPlayingAyahNumber == selectedAyahForSheet!!.number) }
        
        LaunchedEffect(currentPlayingAyahNumber) {
            isPlaying = currentPlayingAyahNumber == selectedAyahForSheet!!.number
        }

        AyahActionSheet(
            ayah = selectedAyahForSheet!!,
            isPlaying = isPlaying,
            viewModel = viewModel,
            onPlayClick = {
                if (isPlaying) {
                    mediaPlayer?.pause()
                    currentPlayingAyahNumber = null
                    isPlaying = false
                } else {
                    playAyahAudio(selectedAyahForSheet!!.number, context) {
                        isPlaying = false
                    }
                    isPlaying = true
                }
            },
            onCopyClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Ayah Text", selectedAyahForSheet!!.text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "تم نسخ الآية الكريمة!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { selectedAyahForSheet = null }
        )
    }

    // Modal Bottom Sheet for Independent Page Tafsir
    if (showPageTafsirSheet) {
        val currentLoadedPageAyahs = remember(page) {
            viewModel.repository.getAyahsByPageFlow(page)
        }.collectAsState(initial = emptyList())

        PageTafsirSheet(
            page = page,
            ayahs = currentLoadedPageAyahs.value,
            viewModel = viewModel,
            onDismiss = { showPageTafsirSheet = false }
        )
    }
}

// Helper to strip redundant Bismillah leading from the first ayah text to avoid double headers
fun cleanAyahText(text: String, isFirstAyah: Boolean, surahNumber: Int): String {
    if (!isFirstAyah || surahNumber == 1 || surahNumber == 9) return text
    val bismillahText1 = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
    val bismillahText2 = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
    return when {
        text.startsWith(bismillahText1) -> text.substring(bismillahText1.length).trim()
        text.startsWith(bismillahText2) -> text.substring(bismillahText2.length).trim()
        else -> text
    }
}

@Composable
fun SurahHeaderBanner(name: String, revelationType: String, numberOfVerses: Int, fontSize: Float, isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .border(1.dp, Color(0xFFC5A880), RoundedCornerShape(8.dp))
            .background(Color(0xFFC5A880).copy(alpha = 0.05f))
            .padding(vertical = 10.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatSurahName(name, withTashkeel = true),
                fontSize = (fontSize + 1f).coerceAtLeast(18f).sp,
                fontFamily = LocalQuranFontFamily.current,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFC5A880) else Color(0xFF425648),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$revelationType • آيَاتُهَا $numberOfVerses",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC5A880),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BismillahBanner(fontSize: Float, isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            fontSize = (fontSize + 1f).coerceAtLeast(18f).sp,
            fontFamily = LocalQuranFontFamily.current,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White else Color(0xFF2C2620),
            textAlign = TextAlign.Center
        )
    }
}

// Highly simplified bottom sheet for selected Ayah containing text, Play button, Copy button, and Tafsir auto-show
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyahActionSheet(
    ayah: AyahModel,
    isPlaying: Boolean,
    viewModel: QuranViewModel,
    onPlayClick: () -> Unit,
    onCopyClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var tafsirText by remember { mutableStateOf<String?>(null) }
    var isLoadingTafsir by remember { mutableStateOf(false) }
    var errorTafsir by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0C140F)

    LaunchedEffect(ayah.number) {
        if (ayah.tafsir != null && ayah.tafsir.isNotEmpty()) {
            tafsirText = ayah.tafsir
        } else {
            isLoadingTafsir = true
            errorTafsir = null
            // Check local repository first
            val localAyah = viewModel.repository.getAyahByNumber(ayah.number)
            val localTafsir = localAyah?.tafsir
            if (localTafsir != null && localTafsir.isNotEmpty()) {
                tafsirText = localTafsir
                isLoadingTafsir = false
            } else {
                // Fetch from network
                withContext(Dispatchers.IO) {
                    try {
                        val url = URL("https://api.alquran.cloud/v1/ayah/${ayah.number}/ar.muyassar")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "GET"
                        conn.connectTimeout = 10000
                        conn.readTimeout = 10000
                        if (conn.responseCode == 200) {
                            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                            val text = json.getJSONObject("data").getString("text")
                            
                            // Save to database
                            viewModel.repository.updateAyahTafsir(ayah.number, text)
                            
                            withContext(Dispatchers.Main) {
                                tafsirText = text
                                isLoadingTafsir = false
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                errorTafsir = "فشل تحميل التفسير من المصدر."
                                isLoadingTafsir = false
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            errorTafsir = "التفسير غير متوفر محلياً، يرجى الاتصال بالإنترنت لعرضه."
                            isLoadingTafsir = false
                        }
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFBF7EE),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Verse Text
            Text(
                text = ayah.text,
                fontSize = 22.sp,
                fontFamily = LocalQuranFontFamily.current,
                textAlign = TextAlign.Center,
                color = if (isDark) Color.White else Color(0xFF2C2620),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Play & Copy Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Button
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) Color(0xFFD32F2F) else Color(0xFF425648)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "استماع",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPlaying) "إيقاف" else "استماع",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Copy Button
                Button(
                    onClick = onCopyClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC5A880)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "نسخ",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "نسخ",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tafsir Card - Displayed Automatically
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFDF9)
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC5A880).copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "التَّفْسِيرُ الميسر:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC5A880),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isLoadingTafsir) {
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF425648))
                        }
                    } else if (errorTafsir != null) {
                        Text(
                            text = errorTafsir!!,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = tafsirText ?: "",
                            fontSize = 16.sp,
                            color = if (isDark) Color.White else Color(0xFF2C2620),
                            lineHeight = 24.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Dedicated independent Bottom Sheet for Page-by-Page Tafsir
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageTafsirSheet(
    page: Int,
    ayahs: List<AyahModel>,
    viewModel: QuranViewModel,
    onDismiss: () -> Unit
) {
    var tafsirMap by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(page, ayahs) {
        if (ayahs.isEmpty()) return@LaunchedEffect
        
        val allLocal = ayahs.all { it.tafsir != null && it.tafsir.isNotEmpty() }
        if (allLocal) {
            tafsirMap = ayahs.associate { it.numberInSurah to it.tafsir!! }
            isLoading = false
            errorText = null
        } else {
            isLoading = true
            errorText = null
            withContext(Dispatchers.IO) {
                try {
                    val url = URL("https://api.alquran.cloud/v1/page/$page/ar.muyassar")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 15000
                    conn.readTimeout = 15000
                    if (conn.responseCode == 200) {
                        val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                        val ayahsArray = json.getJSONObject("data").getJSONArray("ayahs")
                        val map = mutableMapOf<Int, String>()
                        for (i in 0 until ayahsArray.length()) {
                            val ayahObj = ayahsArray.getJSONObject(i)
                            val numInSurah = ayahObj.getInt("numberInSurah")
                            val text = ayahObj.getString("text")
                            val globalNum = ayahObj.getInt("number")
                            map[numInSurah] = text
                            
                            // Cache to database
                            viewModel.repository.updateAyahTafsir(globalNum, text)
                        }
                        tafsirMap = map
                        isLoading = false
                    } else {
                        errorText = "فشل تحميل تفسير الصفحة."
                        isLoading = false
                    }
                } catch (e: Exception) {
                    errorText = "يرجى التحقق من اتصالك بالإنترنت لعرض التفسير الميسر."
                    isLoading = false
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFBF7EE),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "تَفْسِيرُ الصَّفْحَةِ $page (التفسير الميسر)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF425648),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF425648))
                }
            } else if (errorText != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(ayahs) { ayah ->
                        val tafsir = tafsirMap[ayah.numberInSurah] ?: "جاري التحميل..."
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "﴿${formatArabicNumber(ayah.numberInSurah)}﴾ ${ayah.text}",
                                fontSize = 16.sp,
                                fontFamily = LocalQuranFontFamily.current,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF425648),
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tafsir,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = Color(0xFF2C2620).copy(alpha = 0.85f),
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = Color(0xFFC5A880).copy(alpha = 0.15f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranFavoritesScreen(viewModel: QuranViewModel) {
    val favorites by viewModel.favoriteAyahs.collectAsState()
    var selectedAyahForSheet by remember { mutableStateOf<AyahModel?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("المفضلة والآيات المحفوظة", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.viewMode.value = QuranViewMode.INDEX }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = IslamicGreenPrimary)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (favorites.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = IslamicGreenPrimary.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "لا توجد آيات في المفضلة بعد.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "اضغط على أي آية أثناء القراءة لتضيفها إلى المفضلة.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(favorites) { ayah ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAyahForSheet = ayah },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = borderLight()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "الآية ${ayah.numberInSurah}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFC5A880),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = ayah.text,
                                    fontSize = 16.sp,
                                    fontFamily = LocalQuranFontFamily.current,
                                    color = Color(0xFF2C2620),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedAyahForSheet != null) {
        val context = LocalContext.current
        var isPlaying by remember { mutableStateOf(currentPlayingAyahNumber == selectedAyahForSheet!!.number) }
        
        LaunchedEffect(currentPlayingAyahNumber) {
            isPlaying = currentPlayingAyahNumber == selectedAyahForSheet!!.number
        }

        AyahActionSheet(
            ayah = selectedAyahForSheet!!,
            isPlaying = isPlaying,
            viewModel = viewModel,
            onPlayClick = {
                if (isPlaying) {
                    mediaPlayer?.pause()
                    currentPlayingAyahNumber = null
                    isPlaying = false
                } else {
                    playAyahAudio(selectedAyahForSheet!!.number, context) {
                        isPlaying = false
                    }
                    isPlaying = true
                }
            },
            onCopyClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Ayah Text", selectedAyahForSheet!!.text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "تم نسخ الآية الكريمة!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { selectedAyahForSheet = null }
        )
    }
}

// Play real-time audio from Alafasy recitation
private fun playAyahAudio(ayahNumber: Int, context: Context, onComplete: () -> Unit) {
    try {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        
        mediaPlayer = MediaPlayer().apply {
            setDataSource("https://cdn.islamic.network/quran/audio/128/ar.alafasy/$ayahNumber.mp3")
            setOnPreparedListener { 
                start()
                currentPlayingAyahNumber = ayahNumber
            }
            setOnCompletionListener { 
                onComplete()
                currentPlayingAyahNumber = null
            }
            setOnErrorListener { _, _, _ ->
                Toast.makeText(context, "فشل تشغيل الصوت، تأكد من اتصالك بالإنترنت", Toast.LENGTH_SHORT).show()
                onComplete()
                true
            }
            prepareAsync()
        }
    } catch (e: Exception) {
        Log.e("QuranAudio", "Error playing audio", e)
        onComplete()
    }
}

fun formatArabicNumber(number: Int): String {
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val builder = StringBuilder()
    var n = number
    if (n == 0) return "٠"
    while (n > 0) {
        builder.append(arabicDigits[n % 10])
        n /= 10
    }
    return builder.reverse().toString()
}

@Composable
fun borderLight() = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
)

fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%02d:%02d", mins, secs)
}

@Composable
fun FloatingSurahPlayerBar(viewModel: QuranViewModel) {
    val context = LocalContext.current
    val currentSurah by SurahPlaybackManager.currentSurah.collectAsState()
    val isPlaying by SurahPlaybackManager.isPlaying.collectAsState()
    val currentPosition by SurahPlaybackManager.currentPosition.collectAsState()
    val currentDuration by SurahPlaybackManager.currentDuration.collectAsState()
    val isBuffering by SurahPlaybackManager.isBuffering.collectAsState()

    if (currentSurah != null) {
        val surah = currentSurah!!
        var sliderPosition by remember { mutableStateOf<Float?>(null) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(8.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, IslamicGreenPrimary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = IslamicGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "سورة ${surah.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = IslamicGreenPrimary
                            )
                            Text(
                                text = "الشيخ مشاري العفاسي",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = IslamicGreenPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        IconButton(
                            onClick = {
                                val stopIntent = Intent(context, QuranAudioService::class.java).apply {
                                    action = QuranAudioService.ACTION_STOP
                                }
                                context.startService(stopIntent)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق المشغل",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress Slider & Time Labels
                val progressValue = sliderPosition ?: currentPosition.toFloat()
                val durationValue = currentDuration.toFloat().coerceAtLeast(1f)

                Column {
                    Slider(
                        value = progressValue.coerceIn(0f, durationValue),
                        onValueChange = { sliderPosition = it },
                        onValueChangeFinished = {
                            sliderPosition?.let { pos ->
                                val seekIntent = Intent(context, QuranAudioService::class.java).apply {
                                    action = QuranAudioService.ACTION_SEEK_TO
                                    putExtra(QuranAudioService.EXTRA_SEEK_POSITION, pos.toLong())
                                }
                                context.startService(seekIntent)
                            }
                            sliderPosition = null
                        },
                        valueRange = 0f..durationValue,
                        colors = SliderDefaults.colors(
                            thumbColor = GoldAccent,
                            activeTrackColor = IslamicGreenPrimary,
                            inactiveTrackColor = IslamicGreenPrimary.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.height(18.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = formatTime(currentDuration),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Control Buttons: Prev, Play/Pause, Next
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val prevIntent = Intent(context, QuranAudioService::class.java).apply {
                                action = QuranAudioService.ACTION_PREV_SURAH
                            }
                            context.startService(prevIntent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "السورة السابقة",
                            tint = IslamicGreenPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    IconButton(
                        onClick = {
                            val playPauseAction = if (isPlaying) QuranAudioService.ACTION_PAUSE else QuranAudioService.ACTION_RESUME
                            val playPauseIntent = Intent(context, QuranAudioService::class.java).apply {
                                action = playPauseAction
                            }
                            context.startService(playPauseIntent)
                        },
                        modifier = Modifier
                            .background(IslamicGreenPrimary, CircleShape)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    IconButton(
                        onClick = {
                            val nextIntent = Intent(context, QuranAudioService::class.java).apply {
                                action = QuranAudioService.ACTION_NEXT_SURAH
                            }
                            context.startService(nextIntent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "السورة التالية",
                            tint = IslamicGreenPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
