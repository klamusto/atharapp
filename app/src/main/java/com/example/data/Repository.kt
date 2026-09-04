package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

// Data models
data class Thikr(
    val id: Int,
    val category: String,
    val text: String,
    val count: Int,
    val reference: String,
    val virtue: String = "",
    val order: Int = 0
)

data class SurahOutlineItem(
    val surahName: String,
    val pageNumber: Int,
    val order: Int
)

sealed interface DownloadState {
    object Idle : DownloadState
    data class Downloading(val progress: Float, val speedKbS: Float = 0f, val downloadedMb: Float = 0f, val totalMb: Float = 0f) : DownloadState
    object Success : DownloadState
    data class Error(val message: String) : DownloadState
}

class AtharRepository(private val context: Context) {
    private val database = AtharDatabase.getDatabase(context)
    private val settingDao = database.settingDao()
    private val favoriteThikrDao = database.favoriteThikrDao()
    private val customCategoryOrderDao = database.customCategoryOrderDao()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    // 1. Azkar Data Loading
    private var cachedAzkar: List<Thikr>? = null

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
                return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                return activeNetworkInfo != null && activeNetworkInfo.isConnected
            }
        }
        return false
    }

    suspend fun downloadAndUpdateAzkar(): Boolean = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) return@withContext false
        val urlStr = "https://raw.githubusercontent.com/klamusto/abdellahtvapp/refs/heads/main/azkar.json"
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                // Validate JSON has "azkar" key
                val jsonObj = JSONObject(jsonString)
                if (jsonObj.has("azkar")) {
                    val file = File(context.filesDir, "azkar.json")
                    file.writeText(jsonString)
                    cachedAzkar = null // Invalidate cache
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
        false
    }

    suspend fun getAzkar(): List<Thikr> = withContext(Dispatchers.IO) {
        cachedAzkar?.let { return@withContext it }
        val file = File(context.filesDir, "azkar.json")
        val jsonString = if (file.exists()) {
            file.readText()
        } else {
            ""
        }
        if (jsonString.isEmpty()) return@withContext emptyList()
        try {
            val list = mutableListOf<Thikr>()
            val trimmed = jsonString.trim()
            if (trimmed.startsWith("[")) {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        Thikr(
                            id = obj.optInt("id", i + 1),
                            category = obj.optString("category", obj.optString("sub_category", "الأذكار")),
                            text = obj.optString("text", ""),
                            count = obj.optInt("count", obj.optInt("repeat", 1)),
                            reference = obj.optString("reference", ""),
                            virtue = obj.optString("virtue", ""),
                            order = obj.optInt("order", i + 1)
                        )
                    )
                }
            } else if (trimmed.startsWith("{")) {
                val jsonObj = JSONObject(jsonString)
                val jsonArray = jsonObj.getJSONArray("azkar")
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        Thikr(
                            id = obj.optInt("id", i + 1),
                            category = obj.optString("sub_category", obj.optString("category", "الأذكار")),
                            text = obj.optString("text", ""),
                            count = obj.optInt("repeat", obj.optInt("count", 1)),
                            reference = obj.optString("reference", ""),
                            virtue = obj.optString("virtue", ""),
                            order = obj.optInt("order", i + 1)
                        )
                    )
                }
            }
            cachedAzkar = list
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 1.2 Quran Outline Extraction with Fallback
    fun getQuranOutlineFile(): File {
        return File(context.filesDir, "quran_outline.json")
    }

    suspend fun getQuranOutline(): List<SurahOutlineItem> = withContext(Dispatchers.IO) {
        val file = getQuranOutlineFile()
        if (file.exists()) {
            try {
                val jsonStr = file.readText()
                val jsonArray = JSONArray(jsonStr)
                val list = mutableListOf<SurahOutlineItem>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        SurahOutlineItem(
                            surahName = obj.getString("surahName"),
                            pageNumber = obj.getInt("pageNumber"),
                            order = obj.getInt("order")
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    return@withContext list
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val extracted = extractOutlineFromPdf()
        val finalOutline = if (!extracted.isNullOrEmpty() && extracted.size >= 100) {
            extracted
        } else {
            listOf(
                SurahOutlineItem("الفاتحة", 1, 1),
                SurahOutlineItem("البقرة", 2, 2),
                SurahOutlineItem("آل عمران", 50, 3),
                SurahOutlineItem("النساء", 77, 4),
                SurahOutlineItem("المائدة", 106, 5),
                SurahOutlineItem("الأنعام", 128, 6),
                SurahOutlineItem("الأعراف", 151, 7)
            )
        }

        try {
            val jsonArray = JSONArray()
            for (item in finalOutline) {
                val obj = JSONObject()
                obj.put("surahName", item.surahName)
                obj.put("pageNumber", item.pageNumber)
                obj.put("order", item.order)
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        finalOutline
    }

    private suspend fun extractOutlineFromPdf(): List<SurahOutlineItem>? = withContext(Dispatchers.IO) {
        val pdfFile = quranFile
        if (!pdfFile.exists()) return@withContext null
        try {
            val bytes = pdfFile.readBytes()
            var outlineCount = 0
            var i = 0
            while (i < bytes.size - 10) {
                if (bytes[i] == '/'.code.toByte() && 
                    bytes[i+1] == 'T'.code.toByte() && 
                    bytes[i+2] == 'i'.code.toByte() && 
                    bytes[i+3] == 't'.code.toByte() && 
                    bytes[i+4] == 'l'.code.toByte() && 
                    bytes[i+5] == 'e'.code.toByte()) {
                    outlineCount++
                }
                i++
            }
            if (outlineCount > 0) {
                // Return null to trigger the standard 114 Surahs fallback 
                // because standard Quran PDFs often contain unreadable encoded titles.
                return@withContext null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    // 2. Settings Management
    fun getSettingFlow(key: String, defaultValue: String): Flow<String> {
        return settingDao.getSetting(key).map { it?.value ?: defaultValue }
    }

    suspend fun getSettingValue(key: String, defaultValue: String): String {
        return settingDao.getSettingSync(key)?.value ?: defaultValue
    }

    suspend fun saveSetting(key: String, value: String) {
        settingDao.saveSetting(SettingEntity(key, value))
    }

    // 3. Favorites Management
    fun getFavoritesFlow(): Flow<List<Int>> {
        return favoriteThikrDao.getAllFavorites().map { list -> list.map { it.id } }
    }

    suspend fun addFavorite(id: Int) {
        favoriteThikrDao.addFavorite(FavoriteThikrEntity(id))
    }

    suspend fun removeFavorite(id: Int) {
        favoriteThikrDao.removeFavorite(id)
    }

    suspend fun isFavorite(id: Int): Boolean {
        return favoriteThikrDao.isFavorite(id)
    }

    // 4. Custom Category Order
    fun getCustomCategoryOrderFlow(): Flow<List<String>> {
        return customCategoryOrderDao.getAllCategoryOrders().map { list -> list.map { it.categoryName } }
    }

    suspend fun saveCustomCategoryOrder(categories: List<String>) {
        customCategoryOrderDao.clearCategoryOrders()
        val entities = categories.mapIndexed { index, name ->
            CustomCategoryOrderEntity(name, index)
        }
        customCategoryOrderDao.saveCategoryOrders(entities)
    }

    suspend fun clearCustomCategoryOrder() {
        customCategoryOrderDao.clearCategoryOrders()
    }

    // 5. Quran PDF File & Download
    val quranFile: File
        get() = File(context.filesDir, "quran.pdf")

    fun isQuranDownloaded(): Boolean {
        val file = quranFile
        return file.exists() && file.length() > 2 * 1024 * 1024 // Greater than 2MB to be valid
    }

    suspend fun deleteQuranPdf() = withContext(Dispatchers.IO) {
        val file = quranFile
        if (file.exists()) {
            file.delete()
        }
    }

    suspend fun downloadQuranPdf(urlStr: String = "https://dn760100.eu.archive.org/0/items/EQuran00003/E-Quran-00003.pdf") = withContext(Dispatchers.IO) {
        if (isQuranDownloaded()) {
            _downloadState.value = DownloadState.Success
            return@withContext
        }

        _downloadState.value = DownloadState.Downloading(0f)
        val tempFile = File(context.cacheDir, "quran_temp.pdf")
        if (tempFile.exists()) {
            tempFile.delete()
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                _downloadState.value = DownloadState.Error("فشل التنزيل: رمز الاستجابة ${connection.responseCode}")
                return@withContext
            }

            val fileLength = connection.contentLength
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(tempFile)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            val startTime = System.currentTimeMillis()

            while (inputStream.read(data).also { count = it } != -1) {
                total += count
                outputStream.write(data, 0, count)

                if (fileLength > 0) {
                    val progress = total.toFloat() / fileLength.toFloat()
                    val downloadedMb = total.toFloat() / (1024 * 1024)
                    val totalMb = fileLength.toFloat() / (1024 * 1024)
                    val elapsedTime = (System.currentTimeMillis() - startTime) / 1000f
                    val speed = if (elapsedTime > 0) (downloadedMb * 1024 / elapsedTime) else 0f
                    _downloadState.value = DownloadState.Downloading(
                        progress = progress,
                        speedKbS = speed,
                        downloadedMb = downloadedMb,
                        totalMb = totalMb
                    )
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // Verify and move temp to final file
            if (tempFile.exists() && tempFile.length() > 2 * 1024 * 1024) {
                val dest = quranFile
                if (dest.exists()) {
                    dest.delete()
                }
                tempFile.renameTo(dest)
                _downloadState.value = DownloadState.Success
                saveSetting("quran_downloaded", "true")
            } else {
                _downloadState.value = DownloadState.Error("الملف المنزل غير مكتمل أو تالف.")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            _downloadState.value = DownloadState.Error("حدث خطأ أثناء التنزيل: ${e.localizedMessage ?: "مشكلة في الاتصال بالإنترنت"}")
        } finally {
            connection?.disconnect()
        }
    }
}
