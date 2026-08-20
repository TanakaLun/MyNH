package io.tl.mynhentai.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.tl.mynhentai.R
import io.tl.mynhentai.data.local.DownloadError
import io.tl.mynhentai.data.local.DownloadKind
import io.tl.mynhentai.data.local.DownloadStateHolder
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.MiuixPopupHost

/**
 * App-wide download overlays: hosts root dialogs ([MiuixPopupHost]) and the Snackbar that
 * reports finished tasks while the app is in the foreground. Wraps the whole navigation graph.
 */
@Composable
fun DownloadOverlayRoot(content: @Composable () -> Unit) {
    val holder: DownloadStateHolder = koinInject()
    val context = LocalContext.current.applicationContext
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(holder) {
        holder.completions.collect { event ->
            val message = context.getString(
                if (event.kind == DownloadKind.DOWNLOAD) {
                    R.string.toast_download_finished
                } else {
                    R.string.toast_cache_finished
                },
                event.title,
            )
            snackbarHostState.showSnackbar(message = message)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()
        MiuixPopupHost()
        SnackbarHost(
            state = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 84.dp),
        )
    }
    DownloadRetryDialog(holder = holder, onRetry = { retryTask(context, it) })
}

@Composable
private fun DownloadRetryDialog(holder: DownloadStateHolder, onRetry: (DownloadError) -> Unit) {
    val errors by holder.errors.collectAsState()
    val error = errors.firstOrNull() ?: return

    OverlayDialog(
        show = true,
        title = stringResource(
            if (error.kind == DownloadKind.DOWNLOAD) {
                R.string.download_failed_title
            } else {
                R.string.cache_failed_title
            }
        ),
        summary = stringResource(R.string.download_failed_body, error.message),
        onDismissRequest = { holder.dismissError(error.taskId) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (error.title.isNotBlank()) {
                Text(
                    text = error.title,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 2,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onRetry(error)
                        holder.dismissError(error.taskId)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.retry))
                }
                TextButton(
                    text = stringResource(R.string.cancel),
                    onClick = { holder.dismissError(error.taskId) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun retryTask(context: Context, error: DownloadError) {
    when (error.kind) {
        DownloadKind.DOWNLOAD -> {
            val targetDir = error.targetDir ?: return
            DownloadService.startDownload(context, error.pages, error.galleryId, error.title, targetDir)
        }

        DownloadKind.CACHE -> {
            DownloadService.startCache(context, error.pages, error.galleryId, error.title)
        }
    }
}