package com.docufind.app.ui.screens.module

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docufind.app.domain.model.module.DocuFindModule
import com.docufind.app.domain.model.module.ModuleRecordItem
import com.docufind.app.domain.repository.VaultRecordRepository
import com.docufind.app.export.pdf.PdfExportRequest
import com.docufind.app.export.pdf.PdfExportResult
import com.docufind.app.security.crypto.SecureMemory
import com.docufind.app.security.file.SecureFileAccessManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ModuleListUiState(
    val module: DocuFindModule? = null,
    val categoryId: String = "",
    val searchQuery: String = "",
    val selectedFilter: String = "All",
    val items: List<ModuleRecordItem> = emptyList(),
    val filteredItems: List<ModuleRecordItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val showExportConfirmDialog: Boolean = false,
    val showExportPasswordDialog: Boolean = false,
    val isExportingPdf: Boolean = false,
    val exportIncludeOcr: Boolean = false,
    val showUnlockDialog: Boolean = false,
    val unlockError: String? = null,
    val pendingExportPin: CharArray? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ModuleListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: VaultRecordRepository,
    private val secureFileAccessManager: SecureFileAccessManager
) : ViewModel() {

    private val categoryId: String = savedStateHandle.get<String>("categoryId").orEmpty()
    private val module = DocuFindModule.fromId(categoryId)

    private val searchQuery = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow("All")
    private val retrySignal = MutableStateFlow(0)
    private val _exportState = MutableStateFlow(ModuleListUiState(module = module, categoryId = categoryId))
    private val recordsResult = retrySignal.flatMapLatest {
        repository.observeModuleRecords(categoryId)
            .map<List<ModuleRecordItem>, Result<List<ModuleRecordItem>>> { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }

    val uiState: StateFlow<ModuleListUiState> = combine(
        recordsResult,
        searchQuery,
        selectedFilter,
        _exportState
    ) { result, query, filter, export ->
        val items = result.getOrNull().orEmpty()
        val filtered = items.filter { item ->
            val matchesFilter = filter == "All" ||
                item.subCategory.equals(filter, ignoreCase = true)
            val trimmed = query.trim()
            val matchesSearch = trimmed.isBlank() ||
                item.title.contains(trimmed, ignoreCase = true) ||
                item.subCategory?.contains(trimmed, ignoreCase = true) == true
            matchesFilter && matchesSearch
        }
        export.copy(
            module = module,
            categoryId = categoryId,
            searchQuery = query,
            selectedFilter = filter,
            items = items,
            filteredItems = filtered,
            isLoading = false,
            errorMessage = export.errorMessage
                ?: result.exceptionOrNull()?.message
                ?: result.exceptionOrNull()?.javaClass?.simpleName
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ModuleListUiState(module = module, categoryId = categoryId)
    )

    fun onSearchChange(query: String) {
        searchQuery.update { query }
    }

    fun onFilterSelected(filter: String) {
        selectedFilter.update { filter }
    }

    fun retry() {
        retrySignal.update { it + 1 }
    }

    fun requestExportCategoryPdf() = _exportState.update { it.copy(showExportConfirmDialog = true) }

    fun dismissExportConfirmDialog() = _exportState.update { it.copy(showExportConfirmDialog = false) }

    fun confirmExportPdf(includeOcr: Boolean) = _exportState.update {
        it.copy(showExportConfirmDialog = false, showExportPasswordDialog = true, exportIncludeOcr = includeOcr)
    }

    fun dismissExportPasswordDialog() = _exportState.update { it.copy(showExportPasswordDialog = false) }

    fun clearError() = _exportState.update { it.copy(errorMessage = null) }

    fun clearActionMessage() = _exportState.update { it.copy(actionMessage = null) }

    fun dismissUnlockDialog() {
        _exportState.update {
            val pending = it.pendingExportPin
            if (pending != null) SecureMemory.wipe(pending)
            it.copy(showUnlockDialog = false, pendingExportPin = null, unlockError = null)
        }
    }

    fun performCategoryPdfExport(
        activity: androidx.fragment.app.FragmentActivity,
        exportPassword: CharArray,
        pin: CharArray? = null,
        onShare: (File) -> Unit
    ) {
        viewModelScope.launch {
            _exportState.update { it.copy(isExportingPdf = true, showExportPasswordDialog = false) }
            val request = PdfExportRequest.Category(
                categoryId = categoryId,
                includeOcr = _exportState.value.exportIncludeOcr,
                exportPassword = exportPassword.copyOf()
            )
            try {
                when (val result = secureFileAccessManager.exportPdf(activity, request, pin)) {
                    is PdfExportResult.Ready -> {
                        _exportState.update {
                            it.copy(isExportingPdf = false, actionMessage = "Secure category PDF ready to share.")
                        }
                        onShare(result.file)
                    }
                    is PdfExportResult.AuthRequired -> {
                        _exportState.update {
                            it.copy(
                                isExportingPdf = false,
                                showUnlockDialog = true,
                                pendingExportPin = exportPassword.copyOf(),
                                unlockError = result.message
                            )
                        }
                    }
                    is PdfExportResult.Error -> {
                        _exportState.update {
                            it.copy(isExportingPdf = false, errorMessage = result.message)
                        }
                    }
                }
            } finally {
                SecureMemory.wipe(exportPassword)
                SecureMemory.wipe(request.exportPassword)
            }
        }
    }

    fun retryCategoryPdfAfterUnlock(
        activity: androidx.fragment.app.FragmentActivity,
        pin: CharArray?,
        onShare: (File) -> Unit
    ) {
        val pending = _exportState.value.pendingExportPin ?: return
        _exportState.update { it.copy(pendingExportPin = null, showUnlockDialog = false) }
        performCategoryPdfExport(activity, pending, pin, onShare)
    }
}
