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
import androidx.core.content.FileProvider
import java.io.*
import java.net.URL
import com.resourcedl.DownloadResult



class DownloadFileFromURL(val ctx: Context, val textView: TextView?) : AsyncTask<String, Int, DownloadResult>() {

    lateinit var mNotifyManager: NotificationManager
    lateinit var statusBuilder: NotificationCompat.Builder
    lateinit var completeBuilder: NotificationCompat.Builder

    private fun checkWifiEnabled() : Boolean {
        val connMgr = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var isWifiConn = false
        connMgr.allNetworks.forEach { network ->
            connMgr.getNetworkInfo(network).apply {
                if (type == ConnectivityManager.TYPE_WIFI) {
                    isWifiConn = isWifiConn or isConnected
                }
            }
        }
        return isWifiConn
    }

    override fun onPreExecute() {
        super.onPreExecute()

        mNotifyManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        statusBuilder = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Download in Progress")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)

        completeBuilder = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Download Completed")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

    }


    override fun doInBackground(vararg args: String?): DownloadResult {
        if(!checkWifiEnabled())
            return FailedResult("Wifi is not enabled")
        var output : FileOutputStream? = null
        var input : BufferedInputStream? = null
        try {
            val url = URL(args[0] + args[1] + ".mp3")
            val conection = url.openConnection()
            conection.connect()

            val lenghtOfFile = conection.contentLength

            input = BufferedInputStream(url.openStream(), 8192)

            val downloadDir  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS )
            var parentDir = File(downloadDir, "otinanai/")
            if(!parentDir.exists()) {
                parentDir.mkdir()
            }
            var file = File(parentDir, args[1] + ".mp3")
            output = FileOutputStream(file)

            val data = ByteArray(1024)
            var total: Long = 0
            var count = input.read(data)

            while (count != -1) {
                total += count;
                // publishing the progress....
                // After this onProgressUpdate will be called
                publishProgress(((total * 100) / lenghtOfFile).toInt())

                // writing data to file
                output.write(data, 0, count)

                count = input.read(data)
            }


            return SuccessResult(args[1] + ".mp3", file.absolutePath)
        } catch (e : Exception) {
            e.printStackTrace()
            return FailedResult(getStackTrace(e))
        } finally {
            output?.flush()

            // closing streams
            output?.close()
            input?.close()
        }
    }

    private fun getStackTrace(throwable : Throwable) : String {
        val result = StringWriter()
        val printWriter = PrintWriter(result)
        throwable.printStackTrace(printWriter)
        return result.toString()
    }

    override fun onProgressUpdate(vararg progress: Int?) {
        with(NotificationManagerCompat.from(ctx)) {
            val progressInt = progress[0] ?: 0
            statusBuilder.setProgress(100, progressInt, false)
            statusBuilder.setContentText("Downloading... " + progressInt + "%")
            // notificationId is a unique int for each notification that you must define
            notify(0, statusBuilder.build())
        }
    }

    override fun onPostExecute(dlResult: DownloadResult) {
        mNotifyManager.cancel(0)

        if(dlResult is SuccessResult) {
            with(NotificationManagerCompat.from(ctx)) {
                completeBuilder.setContentText("Download completed " + dlResult.filename)
                Log.i("DownloadFileFromURL", "FilePath: " + dlResult.filePath)

                val file = FileProvider.getUriForFile(ctx, "com.resourcedl", File(dlResult.filePath))
                // notificationId is a unique int for each notification that you must define
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(file, "audio/*")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                val pendingIntent: PendingIntent = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
                completeBuilder.setContentIntent(pendingIntent)
                notify(1, completeBuilder.build())
            }
        } else if(dlResult is FailedResult) {
            textView?.setText(dlResult.errorMessage)

            with(NotificationManagerCompat.from(ctx)) {
                completeBuilder.setContentText("Download failed " + dlResult.errorMessage)
                // notificationId is a unique int for each notification that you must define
                notify(1, completeBuilder.build())
            }
        }

    }
}

open class DownloadResult

class SuccessResult(val filename: String, val filePath: String) : DownloadResult()

class FailedResult(val errorMessage: String) : DownloadResult()