package io.tl.mynhentai.data.local

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class DownloadKind {
    DOWNLOAD,
    CACHE,
}

data class DownloadError(
    val taskId: Int,
    val kind: DownloadKind,
    val galleryId: Long,
    val title: String,
    val targetDir: String?,
    val pages: List<Pair<Int, String>>,
    val message: String,
)

data class DownloadCompleted(
    val kind: DownloadKind,
    val title: String,
)

/**
 * Shared inflight state between [io.tl.mynhentai.ui.components.DownloadService] and the UI.
 *
 * - [errors] are surfaced to an in-app OverlayDialog (with a retry action).
 * - [completions] are surfaced as an in-app Snackbar when the app is foregrounded.
 */
class DownloadStateHolder {

    private val _errors = MutableStateFlow<List<DownloadError>>(emptyList())
    val errors: StateFlow<List<DownloadError>> = _errors.asStateFlow()

    private val _completions = MutableSharedFlow<DownloadCompleted>(extraBufferCapacity = 8)
    val completions: SharedFlow<DownloadCompleted> = _completions.asSharedFlow()

    fun reportError(error: DownloadError) {
        _errors.update { it + error }
    }

    fun dismissError(taskId: Int) {
        _errors.update { list -> list.filterNot { it.taskId == taskId } }
    }

    fun clearErrors() {
        _errors.value = emptyList()
    }

    fun reportCompletion(event: DownloadCompleted) {
        _completions.tryEmit(event)
    }
}