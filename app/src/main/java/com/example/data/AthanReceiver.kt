package com.example.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class AthanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(AthanService.EXTRA_PRAYER_NAME) ?: "الصلاة"
        val cityName = intent.getStringExtra(AthanService.EXTRA_CITY_NAME) ?: "موقعك الحالي"
        
        Log.d("AthanReceiver", "Received Athan alarm for $prayerName in $cityName")
        
        val serviceIntent = Intent(context, AthanService::class.java).apply {
            action = AthanService.ACTION_PLAY_ATHAN
            putExtra(AthanService.EXTRA_PRAYER_NAME, prayerName)
            putExtra(AthanService.EXTRA_CITY_NAME, cityName)
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("AthanReceiver", "Failed to start Athan Foreground Service", e)
        }
    }
}
