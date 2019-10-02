package com.resourcedl

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.util.Log
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*


class DownloaderAlarm : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("DownloaderAlarm", "onReceive")
        if(shouldAlarmFireToday(context)) {
            val pm: PowerManager = context?.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DownloaderAlarm:wake_lock")
            wl.acquire(10 * 60 * 1000L /*10 minutes*/)

            val day = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            DownloadFileFromURL(context, null).execute(file_url, day)

            wl.release()
        }
    }

    private fun shouldAlarmFireToday(context: Context?) : Boolean {
        val sharedPref = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return false
        val days = sharedPref.getString("days", "0000000") ?: "0000000"
        val c = Calendar.getInstance()
        val dayOfWeek = c.get(Calendar.DAY_OF_WEEK)
        Log.i("DownloaderAlarm", "dayOfWeek:$dayOfWeek")
        return days.get((dayOfWeek + 5) % 7) == '1'
    }

}