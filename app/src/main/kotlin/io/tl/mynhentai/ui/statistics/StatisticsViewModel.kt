package io.tl.mynhentai.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.tl.mynhentai.data.local.SettingsHelper
import io.tl.mynhentai.data.model.UsageStats
import io.tl.mynhentai.data.repository.MangaRepository
import io.tl.mynhentai.data.repository.UsageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StatisticsUiState(
    val stats: UsageStats = UsageStats(),
    val recordQueries: Boolean = true,
    val rangeDays: Int = 30,
)

class StatisticsViewModel(
    repository: UsageRepository,
    settings: SettingsHelper,
    private val mangaRepository: MangaRepository,
) : ViewModel() {

    private val rangeDays = MutableStateFlow(30)

    val uiState: StateFlow<StatisticsUiState> = combine(
        repository.observeStats(),
        settings.statisticsRecordQueriesFlow,
        rangeDays,
    ) { stats, recordQueries, days ->
        StatisticsUiState(stats = stats, recordQueries = recordQueries, rangeDays = days)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatisticsUiState(),
    )

    fun setRangeDays(days: Int) {
        rangeDays.value = days.coerceIn(7, 30)
    }

    fun resolveCover(path: String): String = mangaRepository.resolveThumbnailUrl(path)
}
