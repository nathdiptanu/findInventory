package com.docufind.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docufind.app.data.repository.ActivityInsightsRepository
import com.docufind.app.domain.model.ActivityInsights
import com.docufind.app.domain.model.InsightPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActivityInsightsViewModel @Inject constructor(
    private val repository: ActivityInsightsRepository
) : ViewModel() {
    private val selectedPeriod = MutableStateFlow(InsightPeriod.DAILY)

    val period: StateFlow<InsightPeriod> = selectedPeriod

    val insights: StateFlow<ActivityInsights> = selectedPeriod
        .flatMapLatest(repository::observeInsights)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ActivityInsights.empty()
        )

    fun selectPeriod(period: InsightPeriod) {
        selectedPeriod.update { period }
    }
}
