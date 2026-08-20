package io.tl.mynhentai.ui.components

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import io.tl.mynhentai.R
import io.tl.mynhentai.data.local.DownloadKind

object NotificationHelper {
    const val CHANNEL_ID = "mynhentai_download"
    const val CHANNEL_RESULT_ID = "mynhentai_download_results"
    const val FOREGROUND_NOTIFICATION_ID = 2001
    const val TASK_NOTIFICATION_BASE = 3000

    fun taskNotificationId(taskId: Int): Int = TASK_NOTIFICATION_BASE + taskId

    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_progress),
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = context.getString(R.string.notification_channel_progress_desc) }
        val resultChannel = NotificationChannel(
            CHANNEL_RESULT_ID,
            context.getString(R.string.notification_channel_result),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.notification_channel_result_desc) }
        manager.createNotificationChannel(channel)
        manager.createNotificationChannel(resultChannel)
    }

    fun buildForegroundNotification(
        context: Context,
        activeCount: Int,
        titles: List<String>,
    ): Notification {
        val preview = titles.take(2).joinToString("、")
        val text = if (titles.size > 2) {
            context.getString(R.string.notification_foreground_text_more, preview, titles.size - 2)
        } else {
            context.getString(R.string.notification_foreground_text, preview)
        }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_foreground_title, activeCount))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    fun buildProgressNotification(
        context: Context,
        title: String,
        progress: Int,
        total: Int,
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_downloading_title))
            .setContentText(context.getString(R.string.notification_progress_text, title, progress, total))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setProgress(total, progress, total == 0)
            .build()
    }

    fun buildCompletionNotification(
        context: Context,
        kind: DownloadKind,
        title: String,
        extraLine: String?,
    ): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_RESULT_ID)
            .setContentTitle(context.getString(
                if (kind == DownloadKind.DOWNLOAD) {
                    R.string.notification_download_done_title
                } else {
                    R.string.notification_cache_done_title
                }
            ))
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        if (!TextUtils.isEmpty(title)) {
            builder.setContentText(title)
        }
        if (!extraLine.isNullOrEmpty()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(extraLine))
        }
        return builder.build()
    }

    fun buildFailedNotification(
        context: Context,
        kind: DownloadKind,
        title: String,
        error: String,
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_RESULT_ID)
            .setContentTitle(context.getString(
                if (kind == DownloadKind.DOWNLOAD) {
                    R.string.notification_download_failed_title
                } else {
                    R.string.notification_cache_failed_title
                }
            ))
            .setContentText(context.getString(R.string.notification_failed_text, title, error))
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
    }
}