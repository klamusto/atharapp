package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AtharTab
import com.example.data.AtharViewModel
import com.example.features.quran.presentation.QuranMode
import com.example.features.quran.presentation.QuranScreen
import com.example.features.quran.presentation.QuranViewModel
import com.example.ui.components.IslamicPattern
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AudioPlayerScreen
import com.example.ui.screens.AzkarScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HijriScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.PrayerTimesScreen
import com.example.ui.screens.QiblaScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TasbihScreen
import com.example.ui.theme.AtharTheme
import kotlinx.coroutines.delay

/* ---------------------------------------------------------------------------
 *  جذر التطبيق: السِمة + شاشة البداية + التنقّل
 * ------------------------------------------------------------------------- */

@Composable
fun AtharApp(viewModel: AtharViewModel) {
    val isDark by viewModel.isDarkMode.collectAsState()

    AtharTheme(darkTheme = isDark) {
        var showSplash by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            delay(1700)
            showSplash = false
        }

        Crossfade(
            targetState = showSplash,
            animationSpec = tween(450),
            label = "splash_fade",
        ) { splash ->
            if (splash) SplashScreen() else AtharScaffold(vm = viewModel)
        }
    }
}

// ---------------------------------------------------------------------------
// شاشة البداية
// ---------------------------------------------------------------------------

@Composable
fun SplashScreen() {
    val extra = AtharTheme.extra
    val transition = rememberInfiniteTransition(label = "splash")
    val ringRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing)),
        label = "ring",
    )
    var appear by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appear = true }
    val scale by animateFloatAsState(
        targetValue = if (appear) 1f else 0.82f,
        animationSpec = tween(900),
        label = "scale",
    )
    val fade by animateFloatAsState(
        targetValue = if (appear) 1f else 0f,
        animationSpec = tween(900),
        label = "fade",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(extra.heroStart, extra.heroMid, extra.heroStart))),
        contentAlignment = Alignment.Center,
    ) {
        IslamicPattern(
            modifier = Modifier.fillMaxSize(),
            color = extra.gold,
            alpha = 0.10f,
            cell = 62.dp,
        )
        Box(
            modifier = Modifier
                .size(230.dp)
                .rotate(ringRotation)
                .alpha(0.45f),
        ) {
            IslamicPattern(
                modifier = Modifier.fillMaxSize(),
                color = extra.gold,
                alpha = 0.55f,
                cell = 115.dp,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale)
                .alpha(fade),
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(extra.gold.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = null,
                    tint = extra.gold,
                    modifier = Modifier.size(54.dp),
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = "أَثَــر",
                style = MaterialTheme.typography.displayMedium,
                color = extra.gold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "رفيقك في القرآن والذكر والصلاة",
                style = MaterialTheme.typography.bodyMedium,
                color = extra.onHeroMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// الهيكل الرئيسي والتنقّل
// ---------------------------------------------------------------------------

private data class BottomDestination(
    val tab: AtharTab,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
    val tag: String,
)

private val bottomDestinations = listOf(
    BottomDestination(AtharTab.HOME, "الرئيسية", Icons.Filled.Home, Icons.Outlined.Home, "tab_home"),
    BottomDestination(AtharTab.QURAN, "القرآن", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "tab_quran"),
    BottomDestination(AtharTab.AZKAR, "الأذكار", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, "tab_azkar"),
    BottomDestination(AtharTab.PRAYER_TIMES, "الصلاة", Icons.Filled.Schedule, Icons.Outlined.Schedule, "tab_prayer"),
    BottomDestination(AtharTab.MORE, "المزيد", Icons.Filled.Apps, Icons.Outlined.Apps, "tab_more"),
)

@Composable
private fun AtharScaffold(vm: AtharViewModel) {
    val quranViewModel: QuranViewModel = viewModel()
    val currentTab by vm.currentTab.collectAsState()
    val backStack by vm.backStack.collectAsState()
    val quranImmersive by quranViewModel.isImmersive.collectAsState()
    val quranMode by quranViewModel.mode.collectAsState()

    val inReader = currentTab == AtharTab.QURAN && quranMode == QuranMode.READER
    val hideChrome = currentTab == AtharTab.QURAN && quranImmersive

    BackHandler(enabled = backStack.size > 1 || quranImmersive || inReader) {
        when {
            quranImmersive -> quranViewModel.setImmersive(false)
            inReader -> quranViewModel.openIndex()
            else -> vm.navigateBack()
        }
    }

    val navigate: (AtharTab) -> Unit = { vm.navigateTo(it) }
    val goBack: () -> Unit = { if (!vm.navigateBack()) vm.navigateHome() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!hideChrome) {
                AtharBottomBar(current = currentTab, onSelect = navigate)
            }
        },
    ) { innerPadding ->
        val padding = if (hideChrome) PaddingValues(0.dp) else innerPadding
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Crossfade(
                targetState = currentTab,
                animationSpec = tween(220),
                label = "screen_fade",
            ) { tab ->
                when (tab) {
                    AtharTab.HOME -> HomeScreen(vm, navigate)
                    AtharTab.QURAN -> QuranScreen(quranViewModel)
                    AtharTab.AZKAR -> AzkarScreen(vm)
                    AtharTab.PRAYER_TIMES -> PrayerTimesScreen(vm, navigate)
                    AtharTab.MORE -> MoreScreen(vm, navigate)
                    AtharTab.HIJRI -> HijriScreen(vm, goBack)
                    AtharTab.QIBLA -> QiblaScreen(vm, goBack)
                    AtharTab.TASBIH -> TasbihScreen(vm, goBack)
                    AtharTab.AUDIOPLAYER -> AudioPlayerScreen(quranViewModel, goBack)
                    AtharTab.DOWNLOADS -> DownloadsScreen(quranViewModel, goBack)
                    AtharTab.FAVORITES -> FavoritesScreen(vm, quranViewModel, goBack, navigate)
                    AtharTab.SETTINGS -> SettingsScreen(vm, goBack)
                    AtharTab.ABOUT -> AboutScreen(goBack)
                }
            }
        }
    }
}

@Composable
private fun AtharBottomBar(current: AtharTab, onSelect: (AtharTab) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        bottomDestinations.forEach { dest ->
            val selected = current == dest.tab
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(dest.tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.icon,
                        contentDescription = dest.label,
                    )
                },
                label = {
                    Text(
                        text = dest.label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.testTag(dest.tag),
            )
        }
    }
}
