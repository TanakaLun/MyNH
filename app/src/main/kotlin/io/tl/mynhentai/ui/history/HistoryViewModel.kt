package io.tl.mynhentai.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.tl.mynhentai.data.local.HistoryEntity
import io.tl.mynhentai.data.repository.MangaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: MangaRepository
) : ViewModel() {

    fun resolveThumbnailUrl(path: String): String = repository.resolveThumbnailUrl(path)

    val history: StateFlow<List<HistoryEntity>> = repository.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun removeHistory(id: Long) {
        viewModelScope.launch {
            repository.removeHistory(id)
        }
    }

    fun removeHistoryByIds(ids: List<Long>) {
        viewModelScope.launch {
            repository.removeHistoryByIds(ids)
        }
    }
}
