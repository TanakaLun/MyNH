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
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun DownloadDialog(
    detail: MangaDetail,
    onDismiss: () -> Unit,
    onDownload: (filename: String, path: String) -> Unit,
    onCache: () -> Unit
) {
    var filename by remember { mutableStateOf(detail.title.pretty ?: detail.title.english ?: "gallery_${detail.id}") }

    OverlayDialog(
        show = true,
        title = stringResource(R.string.download_manga),
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onDownload(filename, DownloadManager.defaultDownloadPath) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.download_zip))
                }
                Button(
                    onClick = { onCache() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.cache_download))
                }
            }

            TextButton(
                text = stringResource(R.string.close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
