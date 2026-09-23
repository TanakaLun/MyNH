package io.tl.mynhentai.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.tl.mynhentai.R
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@Composable
fun NetworkErrorDialog(
    error: Throwable,
    onRetry: () -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    OverlayDialog(
        show = true,
        title = stringResource(R.string.error_load_failed),
        summary = remember(error) { error.toUserMessage(context) },
        onDismissRequest = onExit,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.retry))
            }
            TextButton(
                text = stringResource(R.string.error_exit),
                onClick = onExit,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

internal fun Throwable.toUserMessage(context: Context): String = when (this) {
    is UnknownHostException, is ConnectException, is SocketTimeoutException ->
        context.getString(R.string.error_network)

    is HttpException -> when (code()) {
        429 -> context.getString(R.string.error_http_429)
        else -> context.getString(R.string.error_http_status, code())
    }

    is SerializationException -> context.getString(R.string.error_parse_failed)
    is IOException -> context.getString(R.string.error_network)
    else -> context.getString(R.string.error_unknown)
}
