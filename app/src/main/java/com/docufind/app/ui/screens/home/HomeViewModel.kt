package com.docufind.app.ui.screens.home

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docufind.app.domain.model.HomeTaglines
import com.docufind.app.domain.model.QuickAccessItem
import com.docufind.app.domain.model.QuickAccessSummary
import com.docufind.app.domain.model.RecentDocument
import com.docufind.app.domain.model.module.ModuleRecordItem
import com.docufind.app.domain.repository.DocumentRepository
import com.docufind.app.domain.repository.PreferencesRepository
import com.docufind.app.domain.repository.VaultRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val howToUseExpanded: Boolean = false,
    val userName: String = "",
    val tagline: String = "",
    val taglineAccentColor: Color = Color(0xFF1E88E5)
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    documentRepository: DocumentRepository,
    vaultRecordRepository: VaultRecordRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val quickAccess: StateFlow<List<QuickAccessSummary>> = documentRepository
        .observeQuickAccessSummaries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = QuickAccessItem.homeGridOrder.map { QuickAccessSummary(it, 0) }
        )

    val recentItems: StateFlow<List<RecentDocument>> = documentRepository
        .observeRecentDocuments(limit = 5)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val expiringSoon: StateFlow<List<ModuleRecordItem>> = vaultRecordRepository
        .observeExpiringSoon(limit = 5)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                _uiState.update { it.copy(userName = prefs.userName) }
            }
        }
        refreshGreeting()
    }

    fun toggleHowToUseExpanded() = _uiState.update { it.copy(howToUseExpanded = !it.howToUseExpanded) }

    fun refreshGreeting() {
        val (tagline, accent) = HomeTaglines.pickRandom()
        _uiState.update {
            it.copy(tagline = tagline, taglineAccentColor = accent)
        }
    }
}
