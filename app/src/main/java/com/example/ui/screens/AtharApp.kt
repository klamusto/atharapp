package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.R
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IslamicGreenPrimary
import com.example.ui.theme.IslamicGreenSecondary
import android.content.Context
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress

@Composable
fun AtharApp(viewModel: AtharViewModel) {
    MyApplicationTheme(darkTheme = viewModel.isDarkMode.collectAsState().value) {
        Box(modifier = Modifier.fillMaxSize()) {
            MainContent(viewModel)
        }
    }
}

// 1. SPLASH SCREEN
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF071B11),
                        Color(0xFF0F5132),
                        Color(0xFF071B11)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant Golden Islamic Geometric Ornament representation
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(Color(0x1AD4AF37), CircleShape)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "Logo",
                    tint = GoldAccent,
                    modifier = Modifier.size(70.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "أَثَــر",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = GoldAccent,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "القرآن الكريم والأذكار والتقويم الهجري",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// 2. MAIN APPLICATION SCAFFOLD WITH NAVIGATION
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(viewModel: AtharViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val quranViewModel: com.example.features.quran.presentation.QuranViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val isQuranFullScreen by quranViewModel.isFullScreen.collectAsState()

    val isReadingFullScreen = currentTab == AtharTab.QURAN && isQuranFullScreen

    Scaffold(
        topBar = {
            if (!isReadingFullScreen) {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentTab) {
                                AtharTab.HOME -> "أَثَــر"
                                AtharTab.QURAN -> "القرآن الكريم"
                                AtharTab.AZKAR -> "حصن المسلم"
                                AtharTab.HIJRI -> "التقويم الهجري"
                                AtharTab.PRAYER_TIMES -> "مواقيت الصلاة"
                                AtharTab.QIBLA -> "اتجاه القبلة"
                                AtharTab.TASBIH -> "السبحة الإلكترونية"
                                AtharTab.AUDIOPLAYER -> "مشغل الاستماع"
                                AtharTab.DOWNLOADS -> "التنزيلات"
                                AtharTab.FAVORITES -> "المفضلة"
                                AtharTab.SETTINGS -> "الإعدادات والتنبيهات"
                                AtharTab.ABOUT -> "حول التطبيق"
                            },
                            fontWeight = FontWeight.Bold,
                            color = IslamicGreenPrimary,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        if (currentTab != AtharTab.HOME) {
                            IconButton(onClick = { viewModel.selectTab(AtharTab.HOME) }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "الرئيسية",
                                    tint = IslamicGreenPrimary
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.setDarkMode(!isDarkMode) }) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "الوضع الداكن/المضيء",
                                tint = GoldAccent
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = IslamicGreenPrimary
                    ),
                    modifier = Modifier.shadow(4.dp)
                )
            }
        },
        bottomBar = {
            if (!isReadingFullScreen && (currentTab == AtharTab.QURAN || currentTab == AtharTab.AZKAR || currentTab == AtharTab.HIJRI)) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.shadow(16.dp)
                ) {
                    // Tab 1: Hijri
                    NavigationBarItem(
                        selected = currentTab == AtharTab.HIJRI,
                        onClick = { viewModel.selectTab(AtharTab.HIJRI) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == AtharTab.HIJRI) Icons.Filled.DateRange else Icons.Outlined.DateRange,
                                contentDescription = "التقويم الهجري"
                            )
                        },
                        label = { Text("التقويم", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IslamicGreenPrimary,
                            selectedTextColor = IslamicGreenPrimary,
                            indicatorColor = GoldAccent.copy(alpha = 0.2f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("tab_hijri")
                    )

                    // Tab 2: Azkar
                    NavigationBarItem(
                        selected = currentTab == AtharTab.AZKAR,
                        onClick = { viewModel.selectTab(AtharTab.AZKAR) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == AtharTab.AZKAR) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "الأذكار"
                            )
                        },
                        label = { Text("الأذكار", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IslamicGreenPrimary,
                            selectedTextColor = IslamicGreenPrimary,
                            indicatorColor = GoldAccent.copy(alpha = 0.2f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("tab_azkar")
                    )

                    // Tab 3: Quran
                    NavigationBarItem(
                        selected = currentTab == AtharTab.QURAN,
                        onClick = { viewModel.selectTab(AtharTab.QURAN) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == AtharTab.QURAN) Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                                contentDescription = "القرآن الكريم"
                            )
                        },
                        label = { Text("القرآن", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IslamicGreenPrimary,
                            selectedTextColor = IslamicGreenPrimary,
                            indicatorColor = GoldAccent.copy(alpha = 0.2f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("tab_quran")
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isReadingFullScreen) PaddingValues(0.dp) else innerPadding)
        ) {
            Crossfade(targetState = currentTab, label = "tab_fade") { tab ->
                when (tab) {
                    AtharTab.HOME -> HomeScreen(viewModel)
                    AtharTab.AZKAR -> AzkarScreen(viewModel)
                    AtharTab.QURAN -> QuranScreen(quranViewModel)
                    AtharTab.HIJRI -> HijriScreen(viewModel)
                    AtharTab.PRAYER_TIMES -> PrayerTimesScreen(viewModel)
                    AtharTab.QIBLA -> QiblaScreen(viewModel)
                    AtharTab.TASBIH -> TasbihScreen(viewModel)
                    AtharTab.AUDIOPLAYER -> AudioPlayerScreen(viewModel)
                    AtharTab.DOWNLOADS -> DownloadsScreen(viewModel)
                    AtharTab.FAVORITES -> FavoritesScreen(viewModel)
                    AtharTab.SETTINGS -> SettingsScreen(viewModel)
                    AtharTab.ABOUT -> AboutScreen(viewModel)
                }
            }
        }
    }
}

// HOME SCREEN (DASHBOARD)
@Composable
fun HomeScreen(viewModel: AtharViewModel) {
    val context = LocalContext.current
    val hijriOffset by viewModel.hijriOffset.collectAsState()
    
    // Calculate Hijri Date with offset
    val todayHijri = remember(hijriOffset) {
        try {
            val date = java.time.LocalDate.now().plusDays(hijriOffset.toLong())
            val hijri = java.time.chrono.HijrahDate.from(date)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale("ar"))
            "الأربعاء، " + hijri.format(formatter) // Simple placeholder for day name prefix + hijri formatted date
        } catch (e: Exception) {
            "اليوم"
        }
    }
    
    val todayGregorian = remember {
        val date = java.time.LocalDate.now()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", java.util.Locale("ar"))
        date.format(formatter)
    }

    val cards = listOf(
        HomeCardItem(AtharTab.QURAN, "القرآن الكريم", "قراءة وتدبر الذكر الحكيم", Icons.Default.MenuBook, Color(0xFFE8F5E9)),
        HomeCardItem(AtharTab.AZKAR, "حصن المسلم", "الأذكار والتحصين اليومي", Icons.Default.Favorite, Color(0xFFFCE4EC)),
        HomeCardItem(AtharTab.PRAYER_TIMES, "مواقيت الصلاة", "أوقات الأذان والتنبيهات الدقيقة", Icons.Default.AccessTime, Color(0xFFE3F2FD)),
        HomeCardItem(AtharTab.QIBLA, "بوصلة القبلة", "تحديد اتجاه القبلة بالبوصلة", Icons.Default.Explore, Color(0xFFFFF3E0)),
        HomeCardItem(AtharTab.TASBIH, "السبحة الإلكترونية", "عداد تسبيح واستغفار ذكي", Icons.Default.PlusOne, Color(0xFFE8EAF6)),
        HomeCardItem(AtharTab.AUDIOPLAYER, "الاستماع والتلاوة", "تلاوات قرآنية وتحميل السور", Icons.Default.Audiotrack, Color(0xFFE0F2F1)),
        HomeCardItem(AtharTab.SETTINGS, "الإعدادات والتنبيهات", "ضبط التطبيق والمظهر والمنبهات", Icons.Default.Settings, Color(0xFFECEFF1))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Hero Header Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(IslamicGreenPrimary, Color(0xFF072416))
                    )
                )
                .padding(top = 24.dp, bottom = 32.dp, start = 20.dp, end = 20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Logo & App Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = "أثر",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, GoldAccent, RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "أَثَــر",
                            color = GoldAccent,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right
                        )
                        Text(
                            text = "الرفيق الإسلامي اليومي",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Date Display Container
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = todayHijri,
                            color = GoldAccent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = todayGregorian,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Grid Section Title
        Text(
            text = "الخدمات الرئيسية",
            color = IslamicGreenPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Custom elegant layout list / grid representation using Column and Rows
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group cards in pairs of two for a beautiful 2-column Grid
            val chunkedCards = cards.chunked(2)
            chunkedCards.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    pair.forEach { card ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            HomeCard(card) {
                                viewModel.selectTab(card.tab)
                            }
                        }
                    }
                    // If odd item, fill space with an empty spacer or stretch
                    if (pair.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

data class HomeCardItem(
    val tab: AtharTab,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val baseColor: Color
)

@Composable
fun HomeCard(item: HomeCardItem, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val cardBgColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
        Color.White
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable(onClick = onClick)
            .testTag("home_card_${item.tab.name.lowercase()}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant Icon container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isDark) IslamicGreenPrimary.copy(alpha = 0.2f) else item.baseColor,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = if (isDark) GoldAccent else IslamicGreenPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = item.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isDark) Color.White else IslamicGreenPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = item.subtitle,
                fontSize = 10.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// 3. AZKAR SCREEN
@Composable
fun AzkarScreen(viewModel: AtharViewModel) {
    val allThikrs by viewModel.allThikrs.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val categories = viewModel.getCategories()
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "الأذكار اليومية",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = IslamicGreenPrimary
                )
                Text(
                    text = "حصن المسلم والأذكار المستحبة",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            Row {
                // Theme Toggle
                IconButton(onClick = { viewModel.setDarkMode(!isDarkMode) }) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "الوضع الداكن/المضيء",
                        tint = GoldAccent
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث عن الأذكار...", fontSize = 14.sp, maxLines = 1, softWrap = false) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "بحث", tint = IslamicGreenPrimary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Gray)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = IslamicGreenPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (searchQuery.isEmpty()) {
            // Category selector Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) IslamicGreenPrimary else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .clickable { viewModel.selectCategory(category) }
                            .shadow(2.dp, RoundedCornerShape(20.dp))
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Thikr Cards list
        val filteredThikrs = if (searchQuery.isNotEmpty()) {
            val normalizedQuery = normalizeArabic(searchQuery)
            allThikrs.filter {
                normalizeArabic(it.category).contains(normalizedQuery, ignoreCase = true)
            }
        } else {
            allThikrs.filter { it.category == selectedCategory }
        }

        if (allThikrs.isEmpty()) {
            val hasNetwork = viewModel.isNetworkAvailable()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (!hasNetwork) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "لا يوجد اتصال",
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "يجب الاتصال بالإنترنت مرة واحدة لتحميل الأذكار.",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = IslamicGreenPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "جاري تحميل الأذكار...",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else if (filteredThikrs.isEmpty() && searchQuery.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لم يتم العثور على نتائج لمطابقة البحث.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredThikrs, key = { it.id }) { thikr ->
                    val isFav = favorites.contains(thikr.id)
                    ThikrCard(
                        thikr = thikr,
                        isFav = isFav,
                        onFavClick = { viewModel.toggleFavorite(thikr.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ThikrCard(
    thikr: Thikr,
    isFav: Boolean,
    onFavClick: () -> Unit
) {
    var remainingCount by remember(thikr.id) { mutableIntStateOf(thikr.count) }
    val isCompleted = remainingCount <= 0

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("thikr_card_${thikr.id}")
            .clickable(enabled = !isCompleted) {
                if (remainingCount > 0) {
                    remainingCount--
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row: Favorite (Left) + Elegant Badge (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onFavClick) {
                    Icon(
                        imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "إضافة للمفضلة",
                        tint = if (isFav) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Small elegant badge in the top-right corner
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isCompleted) {
                                IslamicGreenPrimary.copy(alpha = 0.15f)
                            } else {
                                GoldAccent.copy(alpha = 0.15f)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (isCompleted) IslamicGreenPrimary.copy(alpha = 0.3f) else GoldAccent.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "تم",
                            tint = IslamicGreenPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Text(
                            text = remainingCount.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (MaterialTheme.colorScheme.background == Color(0xFF0C140F)) GoldAccent else Color(0xFF8C7355)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main text in Amiri or high quality Arabic font styling
            Text(
                text = thikr.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                style = LocalTextStyle.current.copy(
                    textDirection = TextDirection.Rtl
                )
            )

            if (thikr.virtue.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = thikr.virtue,
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )
            }

            if (thikr.reference.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = thikr.reference,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// 4. QURAN SCREEN
@Composable
fun QuranScreen(quranViewModel: com.example.features.quran.presentation.QuranViewModel) {
    com.example.features.quran.presentation.QuranScreen(quranViewModel)
}

// 5. HIJRI CALENDAR SCREEN
@Composable
fun HijriScreen(viewModel: AtharViewModel) {
    val hijriOffsetDays by viewModel.hijriOffset.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var showGregorianPrimary by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val todayHijri = remember { viewModel.getAdjustedHijrahDate() }
    val occasions = viewModel.getHijriOccasions()

    var displayedHijriDate by remember { mutableStateOf(todayHijri) }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "التقويم الهجري والمناسبات",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = IslamicGreenPrimary
                )

                // Settings Button (Pristine calendar UX requirement)
                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "إعدادات التقويم",
                        tint = IslamicGreenPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Date Highlight Card
            Card(
                colors = CardDefaults.cardColors(containerColor = IslamicGreenPrimary),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "التاريخ الحالي المعدل",
                        color = GoldAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val formattedHijri = formatHijriArabic(todayHijri)
                    Text(
                        text = formattedHijri,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val cal = java.util.Calendar.getInstance().apply {
                        add(java.util.Calendar.DAY_OF_YEAR, hijriOffsetDays)
                    }
                    val sdf = java.text.SimpleDateFormat("d MMMM yyyy", Locale("ar"))
                    val gregorianFormatted = sdf.format(cal.time)
                    Text(
                        text = "\u200F$gregorianFormatted م",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )

                    // Highlight if today has an Islamic occasion
                    val monthVal = todayHijri.get(ChronoField.MONTH_OF_YEAR)
                    val dayVal = todayHijri.get(ChronoField.DAY_OF_MONTH)
                    val todayOccasion = occasions[Pair(monthVal, dayVal)]
                    if (todayOccasion != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .background(GoldAccent, RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "اليوم: $todayOccasion",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calendar Settings Dialog (Keep screen clean)
            if (showSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { showSettingsDialog = false },
                    title = {
                        Text(
                            text = "إعدادات التقويم الهجري",
                            fontWeight = FontWeight.Bold,
                            color = IslamicGreenPrimary,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "تعديل فارق رؤية الهلال يدويًا ومظهر أرقام التقويم.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Divider()

                            // 1. Vision Offset Control
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "معالجة فارق الرؤية:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IslamicGreenPrimary,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "الفارق الحالي: $hijriOffsetDays يوم",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = { viewModel.adjustHijriOffset(-1) },
                                            colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Text("-1 يوم", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { viewModel.adjustHijriOffset(1) },
                                            colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Text("+1 يوم", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        TextButton(onClick = { viewModel.resetHijriOffset() }) {
                                            Text("تصفير", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Divider()

                            // 2. Number Mode Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "عرض الأرقام الميلادية كأساسية:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Switch(
                                    checked = showGregorianPrimary,
                                    onCheckedChange = { showGregorianPrimary = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = GoldAccent,
                                        checkedTrackColor = IslamicGreenPrimary
                                    )
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showSettingsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary)
                        ) {
                            Text("إغلاق", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }


        // Month Calendar View
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Month Navigation Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Month Button (RTL direction-aware: ChevronRight moves back)
                    IconButton(
                        onClick = {
                            displayedHijriDate = displayedHijriDate.minus(1, java.time.temporal.ChronoUnit.MONTHS)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "الشهر السابق",
                            tint = IslamicGreenPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val currentMonthName = getHijriMonthNameArabic(displayedHijriDate.get(ChronoField.MONTH_OF_YEAR))
                        Text(
                            text = "تقويم شهر $currentMonthName ${displayedHijriDate.get(ChronoField.YEAR)} هـ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = IslamicGreenPrimary,
                            textAlign = TextAlign.Center
                        )

                        // Show "Return to Today" button if viewing different month
                        val isDifferentMonth = displayedHijriDate.get(ChronoField.MONTH_OF_YEAR) != todayHijri.get(ChronoField.MONTH_OF_YEAR) ||
                                displayedHijriDate.get(ChronoField.YEAR) != todayHijri.get(ChronoField.YEAR)
                        if (isDifferentMonth) {
                            Text(
                                text = "العودة لليوم الحالي",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                modifier = Modifier
                                    .clickable { displayedHijriDate = todayHijri }
                                    .padding(top = 2.dp)
                            )
                        }
                    }

                    // Next Month Button (RTL direction-aware: ChevronLeft moves forward)
                    IconButton(
                        onClick = {
                            displayedHijriDate = displayedHijriDate.plus(1, java.time.temporal.ChronoUnit.MONTHS)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "الشهر التالي",
                            tint = IslamicGreenPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Week Day labels
                val weekLabels = listOf("سبت", "أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة")
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (label in weekLabels) {
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Generate Month Grid cells
                val firstDay = HijrahDate.of(
                    displayedHijriDate.get(ChronoField.YEAR),
                    displayedHijriDate.get(ChronoField.MONTH_OF_YEAR),
                    1
                )
                val monthLen = firstDay.lengthOfMonth()

                // dayOfWeek: Monday is 1, Sunday is 7
                val rawDayOfWeek = firstDay.get(ChronoField.DAY_OF_WEEK)
                // Map day-of-week to Saturday-based index (Saturday is 6, Sunday is 7, Monday is 1... Friday is 5)
                val initialEmptyCells = when (rawDayOfWeek) {
                    6 -> 0 // Saturday
                    7 -> 1 // Sunday
                    1 -> 2 // Monday
                    2 -> 3 // Tuesday
                    3 -> 4 // Wednesday
                    4 -> 5 // Thursday
                    5 -> 6 // Friday
                    else -> 0
                }

                val cells = mutableListOf<HijriDayCellModel?>()
                for (i in 0 until initialEmptyCells) {
                    cells.add(null)
                }

                for (day in 1..monthLen) {
                    val dateCell = HijrahDate.of(
                        displayedHijriDate.get(ChronoField.YEAR),
                        displayedHijriDate.get(ChronoField.MONTH_OF_YEAR),
                        day
                    )
                    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                        timeInMillis = dateCell.toEpochDay() * 24 * 3600 * 1000
                    }
                    val correspondingGregorianDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
                    val isTodayCell = day == todayHijri.get(ChronoField.DAY_OF_MONTH) &&
                            displayedHijriDate.get(ChronoField.MONTH_OF_YEAR) == todayHijri.get(ChronoField.MONTH_OF_YEAR) &&
                            displayedHijriDate.get(ChronoField.YEAR) == todayHijri.get(ChronoField.YEAR)
                    val cellOccasion = occasions[Pair(displayedHijriDate.get(ChronoField.MONTH_OF_YEAR), day)]

                    cells.add(
                        HijriDayCellModel(
                            hijriDay = day,
                            gregorianDay = correspondingGregorianDay,
                            isToday = isTodayCell,
                            occasion = cellOccasion
                        )
                    )
                }

                // Chunk into weeks (7 cells each)
                val weeks = cells.chunked(7)

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(weeks) { week ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (cell in week) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (cell != null) {
                                        val isSpecial = cell.occasion != null
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    color = when {
                                                        cell.isToday -> GoldAccent
                                                        isSpecial -> IslamicGreenPrimary.copy(alpha = 0.15f)
                                                        else -> Color.Transparent
                                                    },
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    if (isSpecial) {
                                                        coroutineScope.launch {
                                                            ScaffoldMessengerState.showSnackbar(context, "مناسبة: ${cell.occasion}")
                                                        }
                                                    }
                                                },
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = if (showGregorianPrimary) cell.gregorianDay.toString() else cell.hijriDay.toString(),
                                                color = when {
                                                    cell.isToday -> Color.Black
                                                    isSpecial -> GoldAccent
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                },
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )

                                            Text(
                                                text = if (showGregorianPrimary) cell.hijriDay.toString() else cell.gregorianDay.toString(),
                                                color = when {
                                                    cell.isToday -> Color.Black.copy(alpha = 0.6f)
                                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                                },
                                                fontSize = 9.sp
                                            )

                                            if (isSpecial) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .background(GoldAccent, CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            // Fill trailing empty cells in last week
                            val rem = 7 - week.size
                            if (rem > 0) {
                                for (i in 0 until rem) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

data class HijriDayCellModel(
    val hijriDay: Int,
    val gregorianDay: Int,
    val isToday: Boolean,
    val occasion: String?
)

// LOCAL UTILITIES FOR CLEAN ARABIC FORMATTING
fun getHijriMonthNameArabic(month: Int): String {
    return when (month) {
        1 -> "محرم"
        2 -> "صفر"
        3 -> "ربيع الأول"
        4 -> "ربيع الآخر"
        5 -> "جمادى الأولى"
        6 -> "جمادى الآخرة"
        7 -> "رجب"
        8 -> "شعبان"
        9 -> "رمضان"
        10 -> "شوال"
        11 -> "ذو القعدة"
        12 -> "ذو الحجة"
        else -> ""
    }
}

fun formatHijriArabic(hijriDate: HijrahDate): String {
    val day = hijriDate.get(ChronoField.DAY_OF_MONTH)
    val month = hijriDate.get(ChronoField.MONTH_OF_YEAR)
    val year = hijriDate.get(ChronoField.YEAR)
    val monthName = getHijriMonthNameArabic(month)
    return "\u200F$day \u200F$monthName \u200F$year هـ"
}

// Global Custom Scaffold Alert/Snackbar helper to avoid heavy toast delays
object ScaffoldMessengerState {
    fun showSnackbar(context: Context, text: String) {
        android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT).show()
    }
}

fun normalizeArabic(text: String): String {
    var str = text
    val diacritics = charArrayOf(
        '\u064B', '\u064C', '\u064D', '\u064E', '\u064F', '\u0650',
        '\u0651', '\u0652', '\u0653', '\u0654', '\u0655', '\u0670'
    )
    for (c in diacritics) {
        str = str.replace(c.toString(), "")
    }
    str = str.replace("[أإآٱ]".toRegex(), "ا")
    str = str.replace("ى", "ي")
    str = str.replace("ة", "ه")
    return str.trim()
}
