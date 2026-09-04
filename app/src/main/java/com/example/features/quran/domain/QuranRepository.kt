package com.example.features.quran.domain

import android.content.Context
import android.util.Log
import com.example.features.quran.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class QuranRepository(private val quranDao: QuranDao) {

    fun getAllSurahsFlow(): Flow<List<SurahModel>> = quranDao.getAllSurahsFlow().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getSurahByNumber(surahNumber: Int): SurahModel? = withContext(Dispatchers.IO) {
        quranDao.getSurahByNumber(surahNumber)?.toDomain()
    }

    fun getAyahsBySurahFlow(surahNumber: Int): Flow<List<AyahModel>> = quranDao.getAyahsBySurahFlow(surahNumber).map { list ->
        list.map { it.toDomain() }
    }

    fun getAyahsByPageFlow(page: Int): Flow<List<AyahModel>> = quranDao.getAyahsByPageFlow(page).map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getAyahsByPage(page: Int): List<AyahModel> = withContext(Dispatchers.IO) {
        quranDao.getAyahsByPage(page).map { it.toDomain() }
    }

    suspend fun getPagesInSurah(surahNumber: Int): List<Int> = withContext(Dispatchers.IO) {
        quranDao.getPagesInSurah(surahNumber)
    }

    fun getBookmarkedAyahsFlow(): Flow<List<AyahModel>> = quranDao.getBookmarkedAyahsFlow().map { list ->
        list.map { it.toDomain() }
    }

    fun getFavoriteAyahsFlow(): Flow<List<AyahModel>> = quranDao.getFavoriteAyahsFlow().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun toggleBookmark(ayahNumber: Int, isBookmarked: Boolean) = withContext(Dispatchers.IO) {
        quranDao.updateBookmark(ayahNumber, isBookmarked)
    }

    suspend fun toggleFavorite(ayahNumber: Int, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        quranDao.updateFavorite(ayahNumber, isFavorite)
    }

    suspend fun getAyahCount(): Int = withContext(Dispatchers.IO) {
        quranDao.getAyahCount()
    }

    suspend fun getAllAyahs(): List<AyahModel> = withContext(Dispatchers.IO) {
        quranDao.getAllAyahs().map { it.toDomain() }
    }

    suspend fun getAyahByNumber(number: Int): AyahModel? = withContext(Dispatchers.IO) {
        quranDao.getAyahByNumber(number)?.toDomain()
    }

    suspend fun getDownloadedTafsirCount(): Int = withContext(Dispatchers.IO) {
        quranDao.getDownloadedTafsirCount()
    }

    suspend fun updateAyahTafsir(ayahNumber: Int, tafsirText: String) = withContext(Dispatchers.IO) {
        quranDao.updateTafsir(ayahNumber, tafsirText)
    }

    suspend fun downloadAndPopulateTafsir(onProgress: (Float) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.05f)
            val urlString = "https://api.alquran.cloud/v1/quran/ar.muyassar"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("HTTP error code: ${connection.responseCode}"))
            }

            onProgress(0.3f)
            val inputStream = connection.inputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonBuilder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                jsonBuilder.append(line)
            }
            reader.close()
            connection.disconnect()

            onProgress(0.6f)
            val jsonString = jsonBuilder.toString()
            val rootObject = JSONObject(jsonString)
            if (rootObject.getInt("code") != 200) {
                return@withContext Result.failure(Exception("API returned non-200 code"))
            }

            val dataObject = rootObject.getJSONObject("data")
            val surahsArray = dataObject.getJSONArray("surahs")

            val tafsirMap = mutableMapOf<Int, String>()
            for (i in 0 until surahsArray.length()) {
                val surahObj = surahsArray.getJSONObject(i)
                val ayahsArray = surahObj.getJSONArray("ayahs")
                for (j in 0 until ayahsArray.length()) {
                    val ayahObj = ayahsArray.getJSONObject(j)
                    val ayahNum = ayahObj.getInt("number")
                    val tafsirText = ayahObj.getString("text")
                    tafsirMap[ayahNum] = tafsirText
                }
            }

            onProgress(0.8f)
            val existingAyahs = quranDao.getAllAyahs()
            val updatedAyahs = existingAyahs.map { dbAyah ->
                dbAyah.copy(tafsir = tafsirMap[dbAyah.number] ?: dbAyah.tafsir)
            }

            onProgress(0.9f)
            quranDao.insertAyahs(updatedAyahs)

            onProgress(1.0f)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("QuranRepository", "Error populating Tafsir", e)
            Result.failure(e)
        }
    }

    suspend fun downloadAndPopulateDatabase(onProgress: (Float) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.1f)
            val urlString = "https://api.alquran.cloud/v1/quran/quran-uthmani"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 20000
            connection.readTimeout = 20000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("HTTP error code: ${connection.responseCode}"))
            }

            onProgress(0.3f)
            val inputStream = connection.inputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonBuilder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                jsonBuilder.append(line)
            }
            reader.close()
            connection.disconnect()

            onProgress(0.6f)
            val jsonString = jsonBuilder.toString()
            val rootObject = JSONObject(jsonString)
            if (rootObject.getInt("code") != 200) {
                return@withContext Result.failure(Exception("API returned non-200 code"))
            }

            val dataObject = rootObject.getJSONObject("data")
            val surahsArray = dataObject.getJSONArray("surahs")

            val dbSurahs = mutableListOf<DbSurah>()
            val dbAyahs = mutableListOf<DbAyah>()

            for (i in 0 until surahsArray.length()) {
                val surahObj = surahsArray.getJSONObject(i)
                val surahNum = surahObj.getInt("number")
                val surahName = surahObj.getString("name")
                val englishName = surahObj.getString("englishName")
                val englishNameTranslation = surahObj.getString("englishNameTranslation")
                val revelationType = surahObj.getString("revelationType")
                val ayahsArray = surahObj.getJSONArray("ayahs")

                dbSurahs.add(
                    DbSurah(
                        number = surahNum,
                        name = surahName,
                        englishName = englishName,
                        englishNameTranslation = englishNameTranslation,
                        revelationType = if (revelationType == "Meccan") "مكية" else "مدنية",
                        numberOfVerses = ayahsArray.length()
                    )
                )

                for (j in 0 until ayahsArray.length()) {
                    val ayahObj = ayahsArray.getJSONObject(j)
                    val ayahNum = ayahObj.getInt("number")
                    val ayahText = ayahObj.getString("text")
                    val numInSurah = ayahObj.getInt("numberInSurah")
                    val juz = ayahObj.getInt("juz")
                    val manzil = ayahObj.getInt("manzil")
                    val page = ayahObj.getInt("page")
                    val ruku = ayahObj.getInt("ruku")
                    val hizbQuarter = ayahObj.getInt("hizbQuarter")
                    val sajda = ayahObj.optBoolean("sajda", false)

                    dbAyahs.add(
                        DbAyah(
                            number = ayahNum,
                            surahNumber = surahNum,
                            numberInSurah = numInSurah,
                            text = ayahText,
                            juz = juz,
                            manzil = manzil,
                            page = page,
                            ruku = ruku,
                            hizbQuarter = hizbQuarter,
                            sajda = sajda
                        )
                    )
                }
            }

            onProgress(0.8f)
            quranDao.insertSurahs(dbSurahs)
            quranDao.insertAyahs(dbAyahs)

            onProgress(1.0f)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("QuranRepository", "Error populating database", e)
            Result.failure(e)
        }
    }
}

fun DbSurah.toDomain() = SurahModel(
    number = number,
    name = name,
    englishName = englishName,
    englishNameTranslation = englishNameTranslation,
    revelationType = revelationType,
    numberOfVerses = numberOfVerses
)

fun DbAyah.toDomain() = AyahModel(
    number = number,
    surahNumber = surahNumber,
    numberInSurah = numberInSurah,
    text = text,
    juz = juz,
    manzil = manzil,
    page = page,
    ruku = ruku,
    hizbQuarter = hizbQuarter,
    sajda = sajda,
    isBookmarked = isBookmarked,
    isFavorite = isFavorite,
    tafsir = tafsir
)
