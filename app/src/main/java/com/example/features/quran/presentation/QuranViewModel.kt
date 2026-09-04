package com.example.features.quran.presentation

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.features.quran.data.QuranDatabase
import com.example.features.quran.domain.AyahModel
import com.example.features.quran.domain.QuranRepository
import com.example.features.quran.domain.SurahModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** أوضاع شاشة القرآن. */
enum class QuranMode { INDEX, READER }

/** تبويبات الفهرس. */
enum class IndexTab { SURAH, JUZ, PAGE, SAVED }

class QuranViewModel(application: Application) : AndroidViewModel(application) {

    private val quranDao = QuranDatabase.getDatabase(application).quranDao()
    val repository = QuranRepository(quranDao)
    private val prefs = application.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE)

    // ---------------- تهيئة قاعدة البيانات ----------------
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isChecking = MutableStateFlow(true)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _initProgress = MutableStateFlow(0f)
    val initProgress: StateFlow<Float> = _initProgress.asStateFlow()

    private val _initError = MutableStateFlow<String?>(null)
    val initError: StateFlow<String?> = _initError.asStateFlow()

    // ---------------- التفسير ----------------
    val hasTafsir = MutableStateFlow(false)
    val isTafsirDownloading = MutableStateFlow(false)
    val tafsirProgress = MutableStateFlow(0f)
    val tafsirError = MutableStateFlow<String?>(null)

    // ---------------- بيانات ----------------
    val allSurahs: StateFlow<List<SurahModel>> = repository.getAllSurahsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedAyahs: StateFlow<List<AyahModel>> = repository.getBookmarkedAyahsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteAyahs: StateFlow<List<AyahModel>> = repository.getFavoriteAyahsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---------------- حالة الواجهة ----------------
    val mode = MutableStateFlow(QuranMode.INDEX)
    val indexTab = MutableStateFlow(IndexTab.SURAH)
    val isImmersive = MutableStateFlow(false)
    val currentPage = MutableStateFlow(prefs.getInt("last_page", 1).coerceIn(1, TOTAL_PAGES))
    val fontSize = MutableStateFlow(prefs.getFloat("font_size", 23f))
    val showTafsirInline = MutableStateFlow(prefs.getBoolean("show_tafsir", false))
    val highlightedAyah = MutableStateFlow<Int?>(null)

    // ---------------- البحث ----------------
    val searchQuery = MutableStateFlow("")
    val searchResults = MutableStateFlow<List<AyahModel>>(emptyList())
    val isSearching = MutableStateFlow(false)
    private var cachedAyahs: List<AyahModel> = emptyList()

    init {
        SurahPlaybackManager.loadSavedReciter(application)
        checkDatabase()
    }

    private fun checkDatabase() {
        viewModelScope.launch {
            _isChecking.value = true
            try {
                val count = repository.getAyahCount()
                _isInitialized.value = count >= 6236
                if (_isInitialized.value) {
                    refreshTafsirStatus()
                    warmSearchCache()
                }
            } catch (e: Exception) {
                _initError.value = "تعذّر فتح قاعدة بيانات المصحف: ${e.message}"
            } finally {
                _isChecking.value = false
            }
        }
    }

    fun startDatabaseInitialization() {
        if (_isDownloading.value) return
        viewModelScope.launch {
            _isDownloading.value = true
            _initError.value = null
            _initProgress.value = 0.02f

            val result = repository.downloadAndPopulateDatabase { p -> _initProgress.value = p }
            if (result.isFailure) {
                _initError.value =
                    "تعذّر تنزيل بيانات المصحف. تحقّق من اتصالك بالإنترنت ثم أعد المحاولة."
                _initProgress.value = 0f
                _isDownloading.value = false
                return@launch
            }

            _isInitialized.value = true
            _isDownloading.value = false
            _initProgress.value = 1f
            refreshTafsirStatus()
            warmSearchCache()
        }
    }

    private fun refreshTafsirStatus() {
        viewModelScope.launch {
            hasTafsir.value = try {
                repository.getDownloadedTafsirCount() >= 6230
            } catch (e: Exception) {
                false
            }
        }
    }

    fun downloadTafsir() {
        if (isTafsirDownloading.value) return
        viewModelScope.launch {
            isTafsirDownloading.value = true
            tafsirError.value = null
            tafsirProgress.value = 0.03f
            val result = repository.downloadAndPopulateTafsir { p -> tafsirProgress.value = p }
            if (result.isSuccess) {
                hasTafsir.value = true
                tafsirProgress.value = 1f
            } else {
                tafsirError.value = "تعذّر تنزيل التفسير. تحقّق من الاتصال وأعد المحاولة."
            }
            isTafsirDownloading.value = false
        }
    }

    // ---------------- التنقّل داخل المصحف ----------------
    fun openIndex() {
        mode.value = QuranMode.INDEX
        isImmersive.value = false
    }

    fun openReader(page: Int) {
        val p = page.coerceIn(1, TOTAL_PAGES)
        currentPage.value = p
        prefs.edit().putInt("last_page", p).apply()
        mode.value = QuranMode.READER
    }

    fun onPageChanged(page: Int) {
        val p = page.coerceIn(1, TOTAL_PAGES)
        currentPage.value = p
        prefs.edit().putInt("last_page", p).apply()
    }

    fun openSurah(surah: SurahModel) {
        viewModelScope.launch {
            val pages = repository.getPagesInSurah(surah.number)
            openReader(pages.firstOrNull() ?: 1)
        }
    }

    fun openJuz(juz: Int) = openReader(juzStartPage(juz))

    fun openAyah(ayah: AyahModel) {
        highlightedAyah.value = ayah.number
        openReader(ayah.page)
    }

    fun setImmersive(value: Boolean) {
        isImmersive.value = value
    }

    fun toggleImmersive() {
        isImmersive.value = !isImmersive.value
    }

    fun increaseFont() {
        val next = (fontSize.value + 2f).coerceAtMost(42f)
        fontSize.value = next
        prefs.edit().putFloat("font_size", next).apply()
    }

    fun decreaseFont() {
        val next = (fontSize.value - 2f).coerceAtLeast(16f)
        fontSize.value = next
        prefs.edit().putFloat("font_size", next).apply()
    }

    fun setShowTafsirInline(value: Boolean) {
        showTafsirInline.value = value
        prefs.edit().putBoolean("show_tafsir", value).apply()
    }

    suspend fun ayahsForPage(page: Int): List<AyahModel> = repository.getAyahsByPage(page)

    fun toggleBookmark(ayah: AyahModel) {
        viewModelScope.launch { repository.toggleBookmark(ayah.number, !ayah.isBookmarked) }
    }

    fun toggleFavorite(ayah: AyahModel) {
        viewModelScope.launch { repository.toggleFavorite(ayah.number, !ayah.isFavorite) }
    }

    fun surahOf(number: Int): SurahModel? = allSurahs.value.firstOrNull { it.number == number }

    // ---------------- البحث ----------------
    private fun warmSearchCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                cachedAyahs = repository.getAllAyahs()
            } catch (e: Exception) {
                Log.e(TAG, "search cache failed", e)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        if (query.trim().length < 2) {
            searchResults.value = emptyList()
            isSearching.value = false
        }
    }

    fun search(query: String = searchQuery.value) {
        val q = query.trim()
        if (q.length < 2) {
            searchResults.value = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            isSearching.value = true
            if (cachedAyahs.isEmpty()) {
                cachedAyahs = try {
                    repository.getAllAyahs()
                } catch (e: Exception) {
                    emptyList()
                }
            }
            val normalized = normalizeArabic(q)
            searchResults.value = cachedAyahs
                .filter { normalizeArabic(it.text).contains(normalized) }
                .take(300)
            isSearching.value = false
        }
    }

    fun clearSearch() {
        searchQuery.value = ""
        searchResults.value = emptyList()
        isSearching.value = false
    }

    companion object {
        private const val TAG = "QuranViewModel"
        const val TOTAL_PAGES = 604

        /** أرقام صفحات بداية كل جزء. */
        private val JUZ_START_PAGES = intArrayOf(
            1, 22, 42, 62, 82, 102, 121, 142, 162, 182,
            201, 222, 242, 262, 282, 302, 322, 342, 362, 382,
            402, 422, 442, 462, 482, 502, 522, 542, 562, 582,
        )

        fun juzStartPage(juz: Int): Int = JUZ_START_PAGES.getOrElse(juz - 1) { 1 }

        fun juzOfPage(page: Int): Int {
            var juz = 1
            for (i in JUZ_START_PAGES.indices) {
                if (page >= JUZ_START_PAGES[i]) juz = i + 1
            }
            return juz
        }

        /** تجريد النص العربي من التشكيل لتسهيل البحث. */
        fun normalizeArabic(text: String): String {
            var s = text
            val diacritics = charArrayOf(
                '\u064B', '\u064C', '\u064D', '\u064E', '\u064F', '\u0650',
                '\u0651', '\u0652', '\u0653', '\u0654', '\u0655', '\u0670',
            )
            for (c in diacritics) s = s.replace(c.toString(), "")
            s = s.replace("[أإآٱ]".toRegex(), "ا")
            s = s.replace("ى", "ي")
            s = s.replace("ة", "ه")
            s = s.replace("[\u06D6-\u06ED]".toRegex(), "")
            return s.trim()
        }
    }
}
