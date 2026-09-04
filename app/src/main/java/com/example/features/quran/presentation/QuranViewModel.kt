package com.example.features.quran.presentation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.features.quran.data.QuranDatabase
import com.example.features.quran.domain.AyahModel
import com.example.features.quran.domain.QuranRepository
import com.example.features.quran.domain.SurahModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class QuranViewMode {
    INDEX, READER, FAVORITES
}

enum class ReaderTab {
    SURAH, PAGE, JUZ
}

class QuranViewModel(application: Application) : AndroidViewModel(application) {
    private val quranDao = QuranDatabase.getDatabase(application).quranDao()
    val repository = QuranRepository(quranDao)
    private val sharedPrefs = application.getSharedPreferences("quran_prefs", android.content.Context.MODE_PRIVATE)

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _initProgress = MutableStateFlow(0f)
    val initProgress: StateFlow<Float> = _initProgress.asStateFlow()

    private val _initError = MutableStateFlow<String?>(null)
    val initError: StateFlow<String?> = _initError.asStateFlow()

    val isTafsirDownloading = MutableStateFlow(false)
    val tafsirDownloadProgress = MutableStateFlow(0f)
    val tafsirDownloadError = MutableStateFlow<String?>(null)
    val hasLocalTafsir = MutableStateFlow(false)

    val allSurahs: StateFlow<List<SurahModel>> = repository.getAllSurahsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedAyahs: StateFlow<List<AyahModel>> = repository.getBookmarkedAyahsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteAyahs: StateFlow<List<AyahModel>> = repository.getFavoriteAyahsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state
    val viewMode = MutableStateFlow(
        if (sharedPrefs.getInt("last_page", 0) > 0) QuranViewMode.READER else QuranViewMode.INDEX
    )
    val readerTab = MutableStateFlow(ReaderTab.PAGE)
    val isFullScreen = MutableStateFlow(false)
    
    val selectedSurah = MutableStateFlow<SurahModel?>(null)
    val selectedPage = MutableStateFlow(sharedPrefs.getInt("last_page", 1).coerceIn(1, 604))
    val selectedJuz = MutableStateFlow(1)
    
    val fontSize = MutableStateFlow(20f) // Custom font size for Uthmani text (comfortable default)

    // Dynamic Ayahs list for reader screen
    private val _readerAyahs = MutableStateFlow<List<AyahModel>>(emptyList())
    val readerAyahs: StateFlow<List<AyahModel>> = _readerAyahs.asStateFlow()

    val highlightedAyahNumber = MutableStateFlow<Int?>(null)

    private var cachedAllAyahs: List<AyahModel> = emptyList()

    private val _quranSearchResults = MutableStateFlow<List<AyahModel>>(emptyList())
    val quranSearchResults: StateFlow<List<AyahModel>> = _quranSearchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    init {
        SurahPlaybackManager.updateDownloadedList(application, SurahPlaybackManager.currentReciter.value.id)
        checkInitialization()
    }

    private fun checkInitialization() {
        viewModelScope.launch {
            try {
                val count = repository.getAyahCount()
                if (count >= 6236) {
                    _isInitialized.value = true
                    checkTafsirStatus()
                    loadReaderAyahs()
                    loadAllAyahsForSearch()
                } else {
                    _isInitialized.value = false
                }
            } catch (e: Exception) {
                _initError.value = "خطأ في فحص قاعدة البيانات: ${e.message}"
            }
        }
    }

    fun checkTafsirStatus() {
        viewModelScope.launch {
            try {
                val tafsirCount = repository.getDownloadedTafsirCount()
                if (tafsirCount >= 6230) {
                    hasLocalTafsir.value = true
                } else {
                    hasLocalTafsir.value = false
                    downloadCompleteTafsirSilently()
                }
            } catch (e: Exception) {
                hasLocalTafsir.value = false
            }
        }
    }

    private fun downloadCompleteTafsirSilently() {
        viewModelScope.launch {
            isTafsirDownloading.value = true
            val result = downloadAndPopulateTafsirInternal()
            if (result.isSuccess) {
                hasLocalTafsir.value = true
            }
            isTafsirDownloading.value = false
        }
    }

    fun downloadCompleteTafsir() {
        viewModelScope.launch {
            isTafsirDownloading.value = true
            tafsirDownloadError.value = null
            tafsirDownloadProgress.value = 0.05f
            val result = downloadAndPopulateTafsirInternal()
            if (result.isSuccess) {
                hasLocalTafsir.value = true
                isTafsirDownloading.value = false
                tafsirDownloadProgress.value = 1f
            } else {
                tafsirDownloadError.value = "حدث خطأ أثناء تنزيل التفسير: ${result.exceptionOrNull()?.message}"
                isTafsirDownloading.value = false
            }
        }
    }

    private suspend fun downloadAndPopulateTafsirInternal(): Result<Unit> {
        return repository.downloadAndPopulateTafsir { progress ->
            tafsirDownloadProgress.value = progress
        }
    }

    fun startDatabaseInitialization() {
        viewModelScope.launch {
            _initError.value = null
            _initProgress.value = 0.02f
            
            // 1. Download Quran Text (takes 0% to 50%)
            val resultText = repository.downloadAndPopulateDatabase { progress ->
                _initProgress.value = progress * 0.5f
            }
            
            if (resultText.isFailure) {
                _initError.value = "حدث خطأ أثناء تنزيل بيانات المصحف. يرجى التحقق من اتصالك بالإنترنت وإعادة المحاولة. التفاصيل: ${resultText.exceptionOrNull()?.message}"
                _initProgress.value = 0f
                return@launch
            }

            // 2. Download Tafsir Text (takes 50% to 100%)
            val resultTafsir = repository.downloadAndPopulateTafsir { progress ->
                _initProgress.value = 0.5f + (progress * 0.5f)
            }

            if (resultTafsir.isFailure) {
                _initError.value = "حدث خطأ أثناء تنزيل التفسير الميسر. يرجى التحقق من اتصالك بالإنترنت وإعادة المحاولة. التفاصيل: ${resultTafsir.exceptionOrNull()?.message}"
                _initProgress.value = 0f
                return@launch
            }

            _isInitialized.value = true
            checkTafsirStatus()
            loadAllAyahsForSearch()
        }
    }

    fun loadAllAyahsForSearch() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                cachedAllAyahs = repository.getAllAyahs()
            } catch (e: Exception) {
                Log.e("QuranViewModel", "Error caching all ayahs: ${e.message}", e)
            }
        }
    }

    fun selectAyah(ayah: AyahModel) {
        val coercedPage = ayah.page.coerceIn(1, 604)
        saveLastPage(coercedPage)
        readerTab.value = ReaderTab.PAGE
        viewMode.value = QuranViewMode.READER
        loadReaderAyahs()
        highlightedAyahNumber.value = ayah.number
    }

    fun performQuranSearch(query: String) {
        viewModelScope.launch(Dispatchers.Default) {
            if (query.trim().length < 2) {
                _quranSearchResults.value = emptyList()
                _isSearching.value = false
                return@launch
            }
            
            _isSearching.value = true
            
            if (cachedAllAyahs.isEmpty()) {
                try {
                    cachedAllAyahs = repository.getAllAyahs()
                } catch (e: Exception) {
                    Log.e("QuranViewModel", "Error loading search ayahs: ${e.message}", e)
                }
            }

            val normalizedQuery = normalizeArabicText(query)
            val filtered = cachedAllAyahs.filter { ayah ->
                val normalizedText = normalizeArabicText(ayah.text)
                normalizedText.contains(normalizedQuery)
            }
            
            _quranSearchResults.value = filtered
            _isSearching.value = false
        }
    }

    fun normalizeArabicText(text: String): String {
        var str = text
        // 1. Remove diacritics
        val diacritics = charArrayOf(
            '\u064B', '\u064C', '\u064D', '\u064E', '\u064F', '\u0650', // Tanween & Harakat
            '\u0651', '\u0652', // Shadda & Sukun
            '\u0653', '\u0654', '\u0655', // Maddah & Hamza
            '\u0670' // Superscript Alif
        )
        for (c in diacritics) {
            str = str.replace(c.toString(), "")
        }

        // 2. Normalize Alif forms (أ, إ, آ, ٱ, ا) to bare Alif (ا)
        str = str.replace("[أإآٱ]".toRegex(), "ا")

        // 3. Normalize Yah (ي, ى) to ى or ي (let's normalize both to ي)
        str = str.replace("ى", "ي")

        // 4. Normalize Ta Marbutah (ة) to Ha (ه)
        str = str.replace("ة", "ه")

        // 5. Remove any other special Quran symbols
        str = str.replace("[\u06D6-\u06ED]".toRegex(), "")

        return str.trim()
    }

    fun selectSurah(surah: SurahModel) {
        viewModelScope.launch {
            val pages = repository.getPagesInSurah(surah.number)
            val startPage = pages.firstOrNull() ?: 1
            selectedSurah.value = suridToSurah(surah.number)
            selectPage(startPage)
        }
    }

    fun saveLastPage(page: Int) {
        val coercedPage = page.coerceIn(1, 604)
        selectedPage.value = coercedPage
        sharedPrefs.edit().putInt("last_page", coercedPage).apply()
    }

    fun selectPage(page: Int) {
        val coercedPage = page.coerceIn(1, 604)
        saveLastPage(coercedPage)
        readerTab.value = ReaderTab.PAGE
        viewMode.value = QuranViewMode.READER
        loadReaderAyahs()
    }

    fun selectJuz(juz: Int) {
        val coercedJuz = juz.coerceIn(1, 30)
        selectedJuz.value = coercedJuz
        val startPage = getJuzStartPage(coercedJuz)
        selectPage(startPage)
    }

    fun loadReaderAyahs() {
        viewModelScope.launch {
            when (readerTab.value) {
                ReaderTab.SURAH -> {
                    selectedSurah.value?.let { surah ->
                        repository.getAyahsBySurahFlow(surah.number).collectLatest { list ->
                            _readerAyahs.value = list
                        }
                    }
                }
                ReaderTab.PAGE -> {
                    repository.getAyahsByPageFlow(selectedPage.value).collectLatest { list ->
                        _readerAyahs.value = list
                        // Automatically update current surah based on first ayah of the page
                        if (list.isNotEmpty()) {
                            val surahNum = list.first().surahNumber
                            selectedSurah.value = suridToSurah(surahNum)
                        }
                    }
                }
                ReaderTab.JUZ -> {
                    // For Juz view, we filter the full surah or pages belonging to that Juz
                    // To keep it clean and robust, we can get pages of the Juz or get ayahs belonging to the Juz.
                    // Alquran.cloud has ayahs with Juz number. We can query ayahs in that Juz.
                    // But loading a whole Juz in memory can be heavy (around 200 ayahs), which is fine.
                    // Let's load ayahs for the selected Juz:
                    // Since we don't have a specific `getAyahsByJuz` in Dao yet, let's add it, or load via database query.
                    // Wait! A simple way is to load pages of that Juz. Juz starts at certain pages:
                    val startPage = getJuzStartPage(selectedJuz.value)
                    selectPage(startPage)
                }
            }
        }
    }

    fun toggleBookmark(ayah: AyahModel) {
        viewModelScope.launch {
            repository.toggleBookmark(ayah.number, !ayah.isBookmarked)
            // Reload
            loadReaderAyahs()
        }
    }

    fun toggleFavorite(ayah: AyahModel) {
        viewModelScope.launch {
            repository.toggleFavorite(ayah.number, !ayah.isFavorite)
            // Reload
            loadReaderAyahs()
        }
    }

    fun increaseFontSize() {
        if (fontSize.value < 40f) fontSize.value += 2f
    }

    fun decreaseFontSize() {
        if (fontSize.value > 16f) fontSize.value -= 2f
    }

    private fun suridToSurah(num: Int): SurahModel? {
        return allSurahs.value.find { it.number == num }
    }

    companion object {
        fun getJuzStartPage(juz: Int): Int {
            return when (juz) {
                1 -> 1; 2 -> 22; 3 -> 42; 4 -> 62; 5 -> 82; 6 -> 102; 7 -> 121; 8 -> 142; 9 -> 162; 10 -> 182
                11 -> 201; 12 -> 222; 13 -> 242; 14 -> 262; 15 -> 282; 16 -> 302; 17 -> 322; 18 -> 342; 19 -> 362; 20 -> 382
                21 -> 402; 22 -> 422; 23 -> 442; 24 -> 462; 25 -> 482; 26 -> 502; 27 -> 522; 28 -> 542; 29 -> 562; 30 -> 582
                else -> 1
            }
        }
    }
}
