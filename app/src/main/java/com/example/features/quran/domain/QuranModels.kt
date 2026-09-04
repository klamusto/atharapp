package com.example.features.quran.domain

data class SurahModel(
    val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val revelationType: String,
    val numberOfVerses: Int
)

data class AyahModel(
    val number: Int,
    val surahNumber: Int,
    val numberInSurah: Int,
    val text: String,
    val juz: Int,
    val manzil: Int,
    val page: Int,
    val ruku: Int,
    val hizbQuarter: Int,
    val sajda: Boolean,
    val isBookmarked: Boolean,
    val isFavorite: Boolean,
    val tafsir: String? = null
)
