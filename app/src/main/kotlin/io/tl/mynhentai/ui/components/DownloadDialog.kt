package io.tl.mynhentai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

@Composable
fun DownloadDialog(
    detail: MangaDetail,
    onDismiss: () -> Unit,
    onDownload: (filename: String, path: String) -> Unit,
    onCache: () -> Unit
) {
    var filename by remember { mutableStateOf(detail.title.pretty ?: detail.title.english ?: "gallery_${detail.id}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.download_manga)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = filename,
                    onValueChange = { filename = it },
                    label = { Text(stringResource(R.string.filename)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.save_to, DownloadManager.defaultDownloadPath),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(R.string.download_bg_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    FilledTonalButton(
                        onClick = { onCache() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cache_download))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}
