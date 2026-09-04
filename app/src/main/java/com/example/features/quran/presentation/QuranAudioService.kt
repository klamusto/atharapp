package com.example.features.quran.presentation

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.MainActivity
import com.example.features.quran.data.QuranDatabase
import com.example.features.quran.data.DbSurah
import com.example.features.quran.domain.SurahModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

enum class RepeatMode {
    OFF, ONE, ALL
}

data class Reciter(
    val id: String,
    val nameArabic: String,
    val nameEnglish: String,
    val serverUrl: String,
    val quality: String = "عالية (128kbps)"
)

val RECITERS_LIST = listOf(
    Reciter("Alafasy", "مشاري العفاسي", "Mishary Alafasy", "https://server8.mp3quran.net/afs/"),
    Reciter("AbdulBasit", "عبد الباسط عبد الصمد (مرتل)", "Abdul Basit (Murattal)", "https://server7.mp3quran.net/basit/"),
    Reciter("AbdulBasit_Mujawad", "عبد الباسط عبد الصمد (مجود)", "Abdul Basit (Tajweed)", "https://server7.mp3quran.net/basit_mjwd/"),
    Reciter("Maher", "ماهر المعيقلي", "Maher Al-Muaiqly", "https://server12.mp3quran.net/maher/"),
    Reciter("Ghamdi", "سعد الغامدي", "Saad Al-Ghamdi", "https://server7.mp3quran.net/s_gmd/"),
    Reciter("Dossari", "ياسر الدوسري", "Yasser Al-Dossari", "https://server11.mp3quran.net/yasser/"),
    Reciter("Ajmy", "أحمد العجمي", "Ahmed Al-Ajmy", "https://server10.mp3quran.net/ajm/"),
    Reciter("Sudais", "عبد الرحمن السديس", "Abderrahman Al-Soudais", "https://server11.mp3quran.net/sds/")
)

object SurahPlaybackManager {
    val isPlaying = MutableStateFlow(false)
    val currentSurah = MutableStateFlow<SurahModel?>(null)
    val currentDuration = MutableStateFlow(0L) // ms
    val currentPosition = MutableStateFlow(0L) // ms
    val isBuffering = MutableStateFlow(false)
    
    // Multi-reciter, speed, repeat, shuffle states
    val currentReciter = MutableStateFlow<Reciter>(RECITERS_LIST[0])
    val playbackSpeed = MutableStateFlow(1.0f)
    val repeatMode = MutableStateFlow(RepeatMode.OFF)
    val isShuffled = MutableStateFlow(false)
    
    val downloadProgress = MutableStateFlow<Map<Int, Float>>(emptyMap())
    val downloadedSurahs = MutableStateFlow<Set<Int>>(emptySet())

    private const val PREFS_NAME = "athar_audio_prefs"
    private const val KEY_RECITER = "selected_reciter_id"

    fun getAudioDir(context: Context): File {
        return File(context.filesDir, "Audio")
    }

    fun getReciterDir(context: Context, reciter: String): File {
        return File(getAudioDir(context), reciter)
    }

    fun getSurahFile(context: Context, surahNumber: Int, reciter: String): File {
        return File(getReciterDir(context, reciter), String.format("%03d.mp3", surahNumber))
    }

    fun isSurahDownloaded(context: Context, surahNumber: Int, reciter: String): Boolean {
        val file = getSurahFile(context, surahNumber, reciter)
        return file.exists() && file.length() > 100000
    }

    fun loadSavedReciter(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val reciterId = prefs.getString(KEY_RECITER, "Alafasy") ?: "Alafasy"
        val reciter = RECITERS_LIST.find { it.id == reciterId } ?: RECITERS_LIST[0]
        currentReciter.value = reciter
        updateDownloadedList(context, reciter.id)
    }

    fun saveReciter(context: Context, reciter: Reciter) {
        currentReciter.value = reciter
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_RECITER, reciter.id).apply()
        updateDownloadedList(context, reciter.id)
    }

    fun updateDownloadedList(context: Context, reciter: String) {
        try {
            val dir = getReciterDir(context, reciter)
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles()
                val numbers = files?.filter { it.name.endsWith(".mp3") && it.length() > 100000 }
                    ?.mapNotNull { it.name.substringBefore(".mp3").toIntOrNull() }
                    ?.toSet() ?: emptySet()
                downloadedSurahs.value = numbers
            } else {
                downloadedSurahs.value = emptySet()
            }
        } catch (e: Exception) {
            Log.e("SurahPlaybackManager", "Error updating downloaded list", e)
        }
    }

    fun deleteDownload(context: Context, surahNumber: Int, reciter: String) {
        try {
            val file = getSurahFile(context, surahNumber, reciter)
            if (file.exists()) {
                file.delete()
            }
            updateDownloadedList(context, reciter)
        } catch (e: Exception) {
            Log.e("SurahPlaybackManager", "Error deleting download", e)
        }
    }

    fun startDownload(context: Context, surahNumber: Int, reciter: String, scope: CoroutineScope) {
        if (isSurahDownloaded(context, surahNumber, reciter)) return
        val serverUrlPrefix = RECITERS_LIST.find { it.id == reciter }?.serverUrl ?: RECITERS_LIST[0].serverUrl
        
        scope.launch(Dispatchers.IO) {
            try {
                downloadProgress.value = downloadProgress.value + (surahNumber to 0f)
                
                val url = URL("$serverUrlPrefix${String.format("%03d", surahNumber)}.mp3")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()
                
                if (connection.responseCode != 200) {
                    throw Exception("HTTP ${connection.responseCode}")
                }
                
                val fileLength = connection.contentLengthLong
                val reciterDir = getReciterDir(context, reciter)
                if (!reciterDir.exists()) {
                    reciterDir.mkdirs()
                }
                
                val tempFile = File(reciterDir, String.format("%03d.tmp", surahNumber))
                val targetFile = getSurahFile(context, surahNumber, reciter)
                
                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val data = ByteArray(4096)
                        var total: Long = 0
                        var count: Int
                        while (input.read(data).also { count = it } != -1) {
                            total += count
                            output.write(data, 0, count)
                            if (fileLength > 0) {
                                val progress = total.toFloat() / fileLength
                                downloadProgress.value = downloadProgress.value + (surahNumber to progress)
                            }
                        }
                    }
                }
                
                if (fileLength > 0 && tempFile.length() != fileLength) {
                    tempFile.delete()
                    throw Exception("Size mismatch")
                }
                
                if (tempFile.renameTo(targetFile)) {
                    downloadProgress.value = downloadProgress.value - surahNumber
                    updateDownloadedList(context, reciter)
                } else {
                    tempFile.delete()
                    throw Exception("Rename failed")
                }
            } catch (e: Exception) {
                Log.e("SurahPlaybackManager", "Download failed for Surah $surahNumber", e)
                downloadProgress.value = downloadProgress.value + (surahNumber to -1f)
            }
        }
    }
}

class QuranAudioService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var positionJob: Job? = null
    
    private var currentSurahObj: DbSurah? = null

    companion object {
        const val ACTION_PLAY_SURAH = "com.example.ACTION_PLAY_SURAH"
        const val ACTION_PAUSE = "com.example.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.ACTION_RESUME"
        const val ACTION_STOP = "com.example.ACTION_STOP"
        const val ACTION_NEXT_SURAH = "com.example.ACTION_NEXT_SURAH"
        const val ACTION_PREV_SURAH = "com.example.ACTION_PREV_SURAH"
        const val ACTION_SEEK_TO = "com.example.ACTION_SEEK_TO"

        const val EXTRA_SURAH_NUMBER = "extra_surah_number"
        const val EXTRA_SEEK_POSITION = "extra_seek_position"
        private const val CHANNEL_ID = "quran_surah_audio_channel"
        private const val NOTIFICATION_ID = 9001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        SurahPlaybackManager.loadSavedReciter(applicationContext)

        // Observe speed changes
        serviceScope.launch {
            SurahPlaybackManager.playbackSpeed.collect { speed ->
                applyPlaybackSpeed()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_SURAH -> {
                val surahNum = intent.getIntExtra(EXTRA_SURAH_NUMBER, 1)
                startPlayingSurah(surahNum)
            }
            ACTION_PAUSE -> {
                pausePlayback()
            }
            ACTION_RESUME -> {
                resumePlayback()
            }
            ACTION_STOP -> {
                stopPlayback()
            }
            ACTION_NEXT_SURAH -> {
                playNextSurah()
            }
            ACTION_PREV_SURAH -> {
                playPrevSurah()
            }
            ACTION_SEEK_TO -> {
                val seekPos = intent.getLongExtra(EXTRA_SEEK_POSITION, 0L)
                mediaPlayer?.seekTo(seekPos.toInt())
                SurahPlaybackManager.currentPosition.value = seekPos
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تشغيل السور في الخلفية",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار تشغيل سورة كاملة من القرآن الكريم"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startPlayingSurah(surahNumber: Int) {
        serviceScope.launch {
            try {
                val quranDao = QuranDatabase.getDatabase(applicationContext).quranDao()
                val dbSurah = quranDao.getSurahByNumber(surahNumber)
                if (dbSurah == null) {
                    stopSelf()
                    return@launch
                }
                
                val surah = SurahModel(
                    number = dbSurah.number,
                    name = dbSurah.name,
                    englishName = dbSurah.englishName,
                    englishNameTranslation = dbSurah.englishNameTranslation,
                    revelationType = dbSurah.revelationType,
                    numberOfVerses = dbSurah.numberOfVerses
                )

                currentSurahObj = dbSurah
                SurahPlaybackManager.currentSurah.value = surah
                SurahPlaybackManager.isBuffering.value = true
                SurahPlaybackManager.isPlaying.value = false
                SurahPlaybackManager.currentPosition.value = 0L
                SurahPlaybackManager.currentDuration.value = 0L

                stopPositionUpdater()
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null

                val reciter = SurahPlaybackManager.currentReciter.value

                mediaPlayer = MediaPlayer().apply {
                    if (SurahPlaybackManager.isSurahDownloaded(applicationContext, surahNumber, reciter.id)) {
                        val file = SurahPlaybackManager.getSurahFile(applicationContext, surahNumber, reciter.id)
                        setDataSource(file.absolutePath)
                    } else {
                        val formattedNum = String.format("%03d", surahNumber)
                        setDataSource("${reciter.serverUrl}$formattedNum.mp3")
                    }

                    setOnPreparedListener {
                        SurahPlaybackManager.isBuffering.value = false
                        SurahPlaybackManager.isPlaying.value = true
                        SurahPlaybackManager.currentDuration.value = duration.toLong()
                        start()
                        applyPlaybackSpeed()
                        startPositionUpdater()
                        updateNotification()
                    }

                    setOnCompletionListener {
                        stopPositionUpdater()
                        when (SurahPlaybackManager.repeatMode.value) {
                            RepeatMode.ONE -> {
                                startPlayingSurah(surahNumber)
                            }
                            else -> {
                                playNextSurah()
                            }
                        }
                    }

                    setOnErrorListener { _, _, _ ->
                        SurahPlaybackManager.isBuffering.value = false
                        Toast.makeText(applicationContext, "فشل تشغيل السورة الكريمة", Toast.LENGTH_SHORT).show()
                        stopSelf()
                        true
                    }

                    prepareAsync()
                }
            } catch (e: Exception) {
                Log.e("QuranAudioService", "Error starting surah", e)
                SurahPlaybackManager.isBuffering.value = false
                stopSelf()
            }
        }
    }

    private fun startPositionUpdater() {
        positionJob?.cancel()
        positionJob = serviceScope.launch {
            while (isActive) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        SurahPlaybackManager.currentPosition.value = it.currentPosition.toLong()
                        SurahPlaybackManager.currentDuration.value = it.duration.toLong()
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopPositionUpdater() {
        positionJob?.cancel()
        positionJob = null
    }

    private fun pausePlayback() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                SurahPlaybackManager.isPlaying.value = false
                stopPositionUpdater()
                updateNotification()
            }
        }
    }

    private fun resumePlayback() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                SurahPlaybackManager.isPlaying.value = true
                applyPlaybackSpeed()
                startPositionUpdater()
                updateNotification()
            }
        }
    }

    private fun applyPlaybackSpeed() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaPlayer?.let { player ->
                    val speed = SurahPlaybackManager.playbackSpeed.value
                    if (player.isPlaying || SurahPlaybackManager.isPlaying.value) {
                        player.playbackParams = player.playbackParams.setSpeed(speed)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("QuranAudioService", "Error applying speed", e)
        }
    }

    private fun stopPlayback() {
        stopPositionUpdater()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        
        SurahPlaybackManager.isPlaying.value = false
        SurahPlaybackManager.currentSurah.value = null
        SurahPlaybackManager.currentPosition.value = 0L
        SurahPlaybackManager.currentDuration.value = 0L
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun playNextSurah() {
        val currentNum = currentSurahObj?.number ?: return
        val nextNum = if (SurahPlaybackManager.isShuffled.value) {
            (1..114).random()
        } else {
            if (currentNum >= 114) {
                if (SurahPlaybackManager.repeatMode.value == RepeatMode.ALL) 1 else {
                    stopPlayback()
                    return
                }
            } else {
                currentNum + 1
            }
        }
        startPlayingSurah(nextNum)
    }

    private fun playPrevSurah() {
        val currentNum = currentSurahObj?.number ?: return
        val prevNum = if (SurahPlaybackManager.isShuffled.value) {
            (1..114).random()
        } else {
            if (currentNum <= 1) {
                if (SurahPlaybackManager.repeatMode.value == RepeatMode.ALL) 114 else 1
            } else {
                currentNum - 1
            }
        }
        startPlayingSurah(prevNum)
    }

    private fun updateNotification() {
        val surah = currentSurahObj ?: return
        val isPlayingState = SurahPlaybackManager.isPlaying.value
        val reciter = SurahPlaybackManager.currentReciter.value

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, QuranAudioService::class.java).apply { action = ACTION_PAUSE }
        val resumeIntent = Intent(this, QuranAudioService::class.java).apply { action = ACTION_RESUME }
        val nextIntent = Intent(this, QuranAudioService::class.java).apply { action = ACTION_NEXT_SURAH }
        val prevIntent = Intent(this, QuranAudioService::class.java).apply { action = ACTION_PREV_SURAH }
        val stopIntent = Intent(this, QuranAudioService::class.java).apply { action = ACTION_STOP }

        val pendingPause = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val pendingResume = PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val pendingNext = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val pendingPrev = PendingIntent.getService(this, 4, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val pendingStop = PendingIntent.getService(this, 5, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val largeIconBitmap = try {
            val drawable = getDrawable(R.drawable.ic_app_logo)
            if (drawable is android.graphics.drawable.BitmapDrawable) {
                drawable.bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("سورة ${surah.name}")
            .setContentText("القارئ الشيخ ${reciter.nameArabic}")
            .setSmallIcon(R.drawable.ic_app_logo)
            .setLargeIcon(largeIconBitmap)
            .setContentIntent(pendingIntent)
            .setOngoing(isPlayingState)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(android.R.drawable.ic_media_previous, "السابق", pendingPrev)
            .addAction(
                if (isPlayingState) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlayingState) "إيقاف مؤقت" else "تشغيل",
                if (isPlayingState) pendingPause else pendingResume
            )
            .addAction(android.R.drawable.ic_media_next, "التالي", pendingNext)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إغلاق", pendingStop)
            .setColor(0xFF0F5132.toInt())
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPositionUpdater()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        serviceJob.cancel()
    }
}
