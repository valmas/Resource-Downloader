package com.resourcedl

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Environment
import android.util.Log
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.*
import java.net.URL
import com.resourcedl.DownloadResult
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider


class TestNotification(val ctx: Context) : AsyncTask<String, Int, String>() {

    lateinit var completeBuilder: NotificationCompat.Builder


    override fun onPreExecute() {
        super.onPreExecute()

        completeBuilder = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Download Completed")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

    }


    override fun doInBackground(vararg args: String?): String {
        return ""
    }


    override fun onPostExecute(dlResult: String) {
        val filePath = "/storage/emulated/0/Download/otinanai/20190930.mp3"

        with(NotificationManagerCompat.from(ctx)) {
            completeBuilder.setContentText("Download completed")
            val file = FileProvider.getUriForFile(ctx, "com.resourcedl", File(filePath))
            // notificationId is a unique int for each notification that you must define
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(file, "audio/*")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val pendingIntent: PendingIntent = PendingIntent.getActivity(ctx, 0, intent, 0)
            completeBuilder.setContentIntent(pendingIntent)
            notify(1, completeBuilder.build())
        }
    }
}