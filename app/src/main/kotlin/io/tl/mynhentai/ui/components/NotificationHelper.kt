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
import java.util.Locale

object NotificationHelper {
    const val CHANNEL_ID = "mynhentai_download"
    const val CHANNEL_RESULT_ID = "mynhentai_download_results"
    const val DOWNLOAD_NOTIFICATION_ID = 2001
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

    /**
     * One ongoing notification for the whole download queue.
     *
     * @param percent 0..100, or null when the size is unknown.
     * @param showProgress false for the "disabled" mode: the notification still exists because
     *   Android requires one for a foreground service, but carries no progress information.
     * @param promoted request a Live Update on Android 16+; ignored on older versions.
     */
    fun buildDownloadNotification(
        context: Context,
        title: String,
        text: String,
        percent: Int?,
        showProgress: Boolean,
        promoted: Boolean,
        shortText: String?,
    ): Notification {
        if (promoted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            val builder = Notification.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
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

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
        if (showProgress) {
            builder.setProgress(100, percent ?: 0, percent == null)
        }
        return builder.build()
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

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var value = bytes.toDouble()
        var index = 0
        while (value >= 1024 && index < units.lastIndex) {
            value /= 1024
            index++
        }
        return String.format(
            Locale.getDefault(),
            if (index == 0) "%.0f %s" else "%.1f %s",
            value,
            units[index]
        )
    }
}
