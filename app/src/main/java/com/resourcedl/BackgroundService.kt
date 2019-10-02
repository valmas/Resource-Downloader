package com.resourcedl

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.time.Instant
import java.util.*

class BackgroundService : Service() {

    private val REQUEST_CODE = 501

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        Log.i("BackgroundService", "set alarm")

        val alarmMgr = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this, DownloaderAlarm::class.java).let { intent ->
            PendingIntent.getBroadcast(this, REQUEST_CODE, intent, 0)
        }

        val sharedPref = this.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val time = sharedPref.getString("time", "00:00") ?: "00:00"
        val timeArr = time.split(":")
        val hours = timeArr[0].toInt()
        val minutes = timeArr[1].toInt()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hours)
        cal.set(Calendar.MINUTE, minutes)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val date = cal.timeInMillis

        alarmMgr.setRepeating(
            AlarmManager.RTC_WAKEUP,
            date,
            AlarmManager.INTERVAL_DAY,
            alarmIntent
        )

    }

    override fun onDestroy() {

        Log.i("BackgroundService", "cancel alarm")
        val intent = Intent(this, DownloaderAlarm::class.java)
        val sender = PendingIntent.getBroadcast(this, REQUEST_CODE, intent, PendingIntent.FLAG_NO_CREATE)
        val alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(sender)

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


}