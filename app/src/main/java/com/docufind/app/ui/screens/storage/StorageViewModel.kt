package com.docufind.app.ui.screens.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docufind.app.domain.model.backup.StorageInfo
import com.docufind.app.domain.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class StorageViewModel @Inject constructor(
    backupRepository: BackupRepository
) : ViewModel() {
    val storageInfo: StateFlow<StorageInfo> = backupRepository.observeStorageInfo()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StorageInfo(0, 0, 0, null, com.docufind.app.domain.model.backup.BackupStatus.NEVER))
}
