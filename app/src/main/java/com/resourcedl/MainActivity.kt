package com.resourcedl

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.*
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.net.URL
import android.os.Build
import android.text.method.ScrollingMovementMethod
import androidx.core.app.NotificationManagerCompat
import java.io.*
import android.widget.TimePicker
import android.content.Intent
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.util.Log
import android.widget.Switch
import android.widget.ToggleButton
import java.util.*


const val CHANNEL_ID = "SOME_RANDOM_ID"
const val file_url = "http://download.skai.gr/radio/otinanai/otinanai"
const val PREFS_NAME = "DownloaderPrefs"

class MainActivity : AppCompatActivity() {

    // Storage Permissions
    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf(WRITE_EXTERNAL_STORAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        verifyStoragePermissions(this)
        createNotificationChannel()

        Log.i("MainActivity", "Is service Running? ${isMyServiceRunning(BackgroundService::class.java)}")

        updateFromPreferences()
    }

    fun updateFromPreferences() {
        val sharedPref = this.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return

        val enableAutoDownloading = sharedPref.getBoolean("auto", false)
        val switch = findViewById<Switch>(R.id.enableAutoDownloading)
        switch.isChecked = isMyServiceRunning(BackgroundService::class.java)
        switch.setOnCheckedChangeListener { _, isChecked ->
            val intent = Intent(this, BackgroundService::class.java)
            if(isChecked) {
                this.startService(intent)
            } else {
                this.stopService(intent)
            }
        }

        val time = sharedPref.getString("time", "00:00")
        val timeView = findViewById<TextView>(R.id.time)
        timeView.text = time

        val days = sharedPref.getString("days", "0000000") ?: "0000000"
        val daylslist = listOf(R.id.monToggle, R.id.tueToggle, R.id.wedToggle, R.id.thuToggle, R.id.friToggle, R.id.satToggle, R.id.sunToggle)

        daylslist.forEachIndexed {index, toggleId ->
            val toggleValue = days[index]
            val toggle = findViewById<ToggleButton>(toggleId)
            toggle.isChecked = toggleValue == '1'

            toggle.setOnCheckedChangeListener { _, isChecked ->
                val days2 = sharedPref.getString("days", "0000000") ?: "0000000"
                val charArray = days2.toCharArray()
                if(isChecked)
                    charArray[index] = '1'
                else
                    charArray[index] = '0'
                writeToPreferences("days", String(charArray))
            }
        }

    }

    fun download(view: View) {
        val filename = findViewById<TextView>(R.id.filename).text.toString()
        val textView = findViewById<TextView>(R.id.errorArea)
        textView.movementMethod = ScrollingMovementMethod()

        if(!filename.equals("") )
            DownloadFileFromURL(applicationContext, textView).execute(file_url, filename)
        else
            TestNotification(applicationContext).execute()
    }

    private fun verifyStoragePermissions(activity: Activity) {
        // Check if we have write permission
        val permission = ActivityCompat.checkSelfPermission(activity, WRITE_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun openTimePicker(v: View) {
        val mcurrentTime = Calendar.getInstance()
        val hour = mcurrentTime.get(Calendar.HOUR_OF_DAY)
        val minute = mcurrentTime.get(Calendar.MINUTE)
        val mTimePicker: TimePickerDialog
        mTimePicker = TimePickerDialog(this,
            TimePickerDialog.OnTimeSetListener { timePicker, selectedHour, selectedMinute ->
                (v as TextView).text = "$selectedHour:$selectedMinute"
                writeToPreferences("time", "$selectedHour:$selectedMinute")
            }, hour, minute, true
        )//Yes 24 hour time
        mTimePicker.setTitle("Select Time")
        mTimePicker.show()

    }

    fun writeToPreferences(prefId: String, prefValue: String) {
        val sharedPref = this.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            putString(prefId, prefValue)
            commit()
        }
    }

    fun writeToPreferences(prefId: String, prefValue: Boolean) {
        val sharedPref = this.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            putBoolean(prefId, prefValue)
            commit()
        }
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

}
