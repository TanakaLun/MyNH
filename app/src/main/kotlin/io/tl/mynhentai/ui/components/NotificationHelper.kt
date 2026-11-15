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
        val percent = if (total > 0) (progress * 100 / total).coerceIn(0, 100) else null
        return buildProgressNotificationInternal(
            context = context,
            title = context.getString(R.string.notification_downloading_title),
            text = context.getString(R.string.notification_progress_text, title, progress, total),
            percent = percent,
            shortText = percent?.let { "$it%" },
        )
    }

    /** Progress notification for a server archive whose size may be unknown. */
    fun buildArchiveProgressNotification(
        context: Context,
        title: String,
        downloadedBytes: Long,
        totalBytes: Long,
    ): Notification {
        val downloaded = formatBytes(downloadedBytes)
        val percent = if (totalBytes > 0L) {
            (downloadedBytes * 100 / totalBytes).toInt().coerceIn(0, 100)
        } else {
            null
        }
        val text = if (totalBytes > 0L) {
            context.getString(
                R.string.notification_archive_progress,
                downloaded,
                formatBytes(totalBytes)
            )
        } else {
            context.getString(R.string.notification_archive_downloaded, downloaded)
        }
        return buildProgressNotificationInternal(
            context = context,
            title = context.getString(R.string.notification_downloading_title),
            text = text,
            percent = percent,
            shortText = percent?.let { "$it%" } ?: downloaded,
        )
    }

    /**
     * Builds an ongoing progress notification. On Android 16+ it requests promotion so the
     * download shows up as a Live Update (status chip, lock screen, promoted card).
     */
    private fun buildProgressNotificationInternal(
        context: Context,
        title: String,
        text: String,
        percent: Int?,
        shortText: String?,
    ): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            val builder = Notification.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setRequestPromotedOngoing(true)
                .setStyle(
                    Notification.ProgressStyle().apply {
                        if (percent == null) {
                            setProgressIndeterminate(true)
                        } else {
                            setProgress(percent)
                            setStyledByProgress(true)
                        }
                    }
                )
            if (shortText != null) builder.setShortCriticalText(shortText)
            return builder.build()
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setProgress(100, percent ?: 0, percent == null)
            .build()
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var value = bytes.toDouble()
        var index = 0
        while (value >= 1024 && index < units.lastIndex) {
            value /= 1024
            index++
        }
        return String.format(
            java.util.Locale.getDefault(),
            if (index == 0) "%.0f %s" else "%.1f %s",
            value,
            units[index]
        )
    }

    fun buildCompletionNotification(
        context: Context,
        kind: DownloadKind,
        title: String,
        extraLine: String?,
    ): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_RESULT_ID)
            .setContentTitle(context.getString(
                if (kind == DownloadKind.CACHE) {
                    R.string.notification_cache_done_title
                } else {
                    R.string.notification_download_done_title
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
                if (kind == DownloadKind.CACHE) {
                    R.string.notification_cache_failed_title
                } else {
                    R.string.notification_download_failed_title
                }
            ))
            .setContentText(context.getString(R.string.notification_failed_text, title, error))
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
    }
}