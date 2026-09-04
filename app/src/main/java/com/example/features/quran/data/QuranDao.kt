package com.example.features.quran.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {
    @Query("SELECT COUNT(*) FROM quran_ayahs")
    suspend fun getAyahCount(): Int

    @Query("SELECT * FROM quran_surahs ORDER BY number ASC")
    fun getAllSurahsFlow(): Flow<List<DbSurah>>

    @Query("SELECT * FROM quran_surahs ORDER BY number ASC")
    suspend fun getAllSurahs(): List<DbSurah>

    @Query("SELECT * FROM quran_surahs WHERE number = :surahNumber")
    suspend fun getSurahByNumber(surahNumber: Int): DbSurah?

    @Query("SELECT * FROM quran_ayahs WHERE surahNumber = :surahNumber ORDER BY numberInSurah ASC")
    fun getAyahsBySurahFlow(surahNumber: Int): Flow<List<DbAyah>>

    @Query("SELECT * FROM quran_ayahs WHERE page = :page ORDER BY number ASC")
    fun getAyahsByPageFlow(page: Int): Flow<List<DbAyah>>

    @Query("SELECT * FROM quran_ayahs WHERE page = :page ORDER BY number ASC")
    suspend fun getAyahsByPage(page: Int): List<DbAyah>

    @Query("SELECT DISTINCT page FROM quran_ayahs WHERE surahNumber = :surahNumber ORDER BY page ASC")
    suspend fun getPagesInSurah(surahNumber: Int): List<Int>

    @Query("SELECT * FROM quran_ayahs WHERE number = :number")
    suspend fun getAyahByNumber(number: Int): DbAyah?

    @Query("SELECT * FROM quran_ayahs WHERE isBookmarked = 1 ORDER BY number ASC")
    fun getBookmarkedAyahsFlow(): Flow<List<DbAyah>>

    @Query("SELECT * FROM quran_ayahs WHERE isFavorite = 1 ORDER BY number ASC")
    fun getFavoriteAyahsFlow(): Flow<List<DbAyah>>

    @Query("SELECT * FROM quran_ayahs ORDER BY number ASC")
    suspend fun getAllAyahs(): List<DbAyah>

    @Query("SELECT COUNT(*) FROM quran_ayahs WHERE tafsir IS NOT NULL AND tafsir != ''")
    suspend fun getDownloadedTafsirCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurahs(surahs: List<DbSurah>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahs(ayahs: List<DbAyah>)

    @Query("UPDATE quran_ayahs SET tafsir = :tafsir WHERE number = :number")
    suspend fun updateTafsir(number: Int, tafsir: String)

    @Query("UPDATE quran_ayahs SET isBookmarked = :isBookmarked WHERE number = :number")
    suspend fun updateBookmark(number: Int, isBookmarked: Boolean)

    @Query("UPDATE quran_ayahs SET isFavorite = :isFavorite WHERE number = :number")
    suspend fun updateFavorite(number: Int, isFavorite: Boolean)
}
