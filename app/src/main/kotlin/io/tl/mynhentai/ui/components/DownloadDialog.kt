package io.tl.mynhentai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.tl.mynhentai.R
import io.tl.mynhentai.data.model.MangaDetail
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun DownloadDialog(
    detail: MangaDetail,
    archiveAvailable: Boolean,
    backdrop: Backdrop?,
    onDismiss: () -> Unit,
    onDownload: (filename: String, path: String) -> Unit,
    onArchive: (filename: String, format: String) -> Unit,
    onCache: () -> Unit
) {
    var filename by remember { mutableStateOf(detail.title.pretty ?: detail.title.english ?: "gallery_${detail.id}") }

    AppDialog(
        show = true,
        title = stringResource(R.string.download_manga),
        backdrop = backdrop,
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextField(
                value = filename,
                onValueChange = { filename = it },
                label = stringResource(R.string.filename),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.save_to, DownloadManager.defaultDownloadPath),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )

            Text(
                text = stringResource(R.string.download_bg_hint),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )

            if (archiveAvailable) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppDialogButton(
                        text = stringResource(R.string.download_as_cbz),
                        onClick = { onArchive(filename, "cbz") },
                        modifier = Modifier.weight(1f)
                    )
                    AppDialogButton(
                        text = stringResource(R.string.download_as_zip),
                        onClick = { onArchive(filename, "zip") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppDialogButton(
                    text = stringResource(R.string.download_pages_zip),
                    onClick = { onDownload(filename, DownloadManager.defaultDownloadPath) },
                    modifier = Modifier.weight(1f),
                    primary = true
                )
                AppDialogButton(
                    text = stringResource(R.string.cache_download),
                    onClick = { onCache() },
                    modifier = Modifier.weight(1f)
                )
            }

            AppDialogButton(
                text = stringResource(R.string.close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
