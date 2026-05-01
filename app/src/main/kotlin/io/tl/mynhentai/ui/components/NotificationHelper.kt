package io.tl.mynhentai.ui.components

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    const val CHANNEL_ID = "mynhentai_download"
    const val PROGRESS_NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "下载进度",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "漫画下载和缓存任务进度"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun buildProgressNotification(
        context: Context,
        title: String,
        progress: Int,
        total: Int
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("下载中")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setProgress(total, progress, false)
            .build()
    }

    fun buildSuccessNotification(context: Context, title: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("下载完成")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
    }

    fun buildFailedNotification(context: Context, title: String, error: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("下载失败")
            .setContentText("$title: $error")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
    }
}
