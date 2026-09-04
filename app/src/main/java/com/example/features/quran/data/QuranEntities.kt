package com.example.features.quran.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quran_surahs")
data class DbSurah(
    @PrimaryKey val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val revelationType: String,
    val numberOfVerses: Int
)

@Entity(tableName = "quran_ayahs")
data class DbAyah(
    @PrimaryKey val number: Int, // Global number (1 to 6236)
    val surahNumber: Int,
    val numberInSurah: Int,
    val text: String,
    val juz: Int,
    val manzil: Int,
    val page: Int,
    val ruku: Int,
    val hizbQuarter: Int,
    val sajda: Boolean,
    val isBookmarked: Boolean = false,
    val isFavorite: Boolean = false,
    val tafsir: String? = null
)
