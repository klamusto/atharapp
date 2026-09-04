package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.MainActivity

class AzkarNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_AZKAR_TYPE = "extra_azkar_type"
        private const val CHANNEL_ID = "azkar_notification_channel"
        private const val NOTIFICATION_ID_BASE = 9600
    }

    override fun onReceive(context: Context, intent: Intent) {
        val azkarType = intent.getStringExtra(EXTRA_AZKAR_TYPE) ?: "morning"
        val (title, content, notificationId) = when (azkarType) {
            "morning" -> Triple(
                "أذكار الصباح",
                "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ... حان وقت أذكار الصباح لقراءة البركة في يومك.",
                NOTIFICATION_ID_BASE + 1
            )
            "evening" -> Triple(
                "أذكار المساء",
                "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ... حان وقت أذكار المساء لتحصين نفسك وراحتك.",
                NOTIFICATION_ID_BASE + 2
            )
            else -> Triple(
                "أذكار النوم",
                "بِاسْمِكَ رَبِّي وَضَعْتُ جَنْبِي... حان وقت أذكار النوم لنوم هادئ وتحصين مبارك.",
                NOTIFICATION_ID_BASE + 3
            )
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تنبيهات الأذكار اليومية",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تنبيهات يومية لقراءة أذكار الصباح والمساء والنوم"
            }
            manager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setColor(0xFF0F5132.toInt())
            .build()

        manager.notify(notificationId, notification)
    }
}
