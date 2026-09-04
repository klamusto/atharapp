package com.example.data

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.MainActivity
import java.io.File

class AthanService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    companion object {
        const val ACTION_PLAY_ATHAN = "com.example.ACTION_PLAY_ATHAN"
        const val ACTION_STOP_ATHAN = "com.example.ACTION_STOP_ATHAN"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_CITY_NAME = "extra_city_name"
        
        private const val CHANNEL_ID = "athan_notification_channel"
        private const val NOTIFICATION_ID = 9501
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_ATHAN -> {
                val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة القادمة"
                val cityName = intent.getStringExtra(EXTRA_CITY_NAME) ?: "موقعك الحالي"
                startPlayingAthan(prayerName, cityName)
            }
            ACTION_STOP_ATHAN -> {
                stopPlayingAthan()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تنبيهات الأذان والصلاة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات دخول وقت الصلاة وتشغيل صوت الأذان"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startPlayingAthan(prayerName: String, cityName: String) {
        val sharedPrefs = getSharedPreferences("athan_settings", Context.MODE_PRIVATE)
        val isAthanEnabled = sharedPrefs.getBoolean("is_enabled", true)
        val isAlertOnly = sharedPrefs.getBoolean("is_alert_only", false)
        val selectedSound = sharedPrefs.getString("selected_sound", "Muaiqly") ?: "Muaiqly"
        val volume = sharedPrefs.getFloat("volume", 0.8f)

        // Show Foreground Notification
        showAthanNotification(prayerName, cityName)

        if (!isAthanEnabled) {
            // Athan is disabled completely, do nothing or just show notification
            return
        }

        // Release old media player
        mediaPlayer?.release()
        mediaPlayer = null

        if (isAlertOnly) {
            // Play a simple alert ringtone
            try {
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, alarmUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )
                    setVolume(volume, volume)
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                Log.e("AthanService", "Failed to play default notification sound", e)
            }
        } else {
            // Play actual full Athan MP3
            try {
                mediaPlayer = MediaPlayer().apply {
                    val localAthanFile = File(filesDir, "athan_$selectedSound.mp3")
                    if (localAthanFile.exists()) {
                        setDataSource(localAthanFile.absolutePath)
                    } else {
                        // Stream it or use a default URL
                        val url = when (selectedSound) {
                            "Makkah" -> "https://download.tvquran.com/download/selections/3/5ea3ffc6b5419.mp3"
                            "Madinah" -> "https://download.tvquran.com/download/selections/3/5ea3ffcbd04f6.mp3"
                            else -> "https://download.tvquran.com/download/selections/3/5ea3ffbe077df.mp3" // Al-Muaiqly
                        }
                        setDataSource(url)
                    }
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )
                    setVolume(volume, volume)
                    setOnPreparedListener {
                        start()
                    }
                    setOnCompletionListener {
                        stopSelf()
                    }
                    setOnErrorListener { _, _, _ ->
                        // Fallback to ringtone
                        playFallbackRingtone()
                        true
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                Log.e("AthanService", "Failed to play Athan", e)
                playFallbackRingtone()
            }
        }
    }

    private fun playFallbackRingtone() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                prepare()
                start()
            }
        } catch (e2: Exception) {
            Log.e("AthanService", "Fallback ringtone failed", e2)
        }
    }

    private fun showAthanNotification(prayerName: String, cityName: String) {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingOpenIntent = PendingIntent.getActivity(
            this, 10, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AthanService::class.java).apply {
            action = ACTION_STOP_ATHAN
        }
        val pendingStopIntent = PendingIntent.getService(
            this, 11, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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
            .setContentTitle("حان الآن موعد أذان $prayerName")
            .setContentText("بتوقيت $cityName والمناطق المجاورة لها")
            .setSmallIcon(R.drawable.ic_app_logo)
            .setLargeIcon(largeIconBitmap)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingOpenIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف الأذان", pendingStopIntent)
            .addAction(android.R.drawable.ic_menu_view, "فتح التطبيق", pendingOpenIntent)
            .setColor(0xFF0F5132.toInt())
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopPlayingAthan() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
