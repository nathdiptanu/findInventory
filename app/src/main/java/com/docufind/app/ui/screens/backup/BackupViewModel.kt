package com.docufind.app.ui.screens.backup

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docufind.app.R
import com.docufind.app.cloud.GoogleAccountSession
import com.docufind.app.cloud.GoogleDriveBackupClient
import com.docufind.app.domain.model.backup.BackupStatus
import com.docufind.app.domain.repository.BackupRepository
import com.docufind.app.security.auth.AuthGate
import com.docufind.app.security.auth.AuthPurpose
import com.docufind.app.security.auth.AuthResult
import com.docufind.app.security.backup.BackupPreview
import com.docufind.app.security.backup.RestoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupUiState(
    val backupPassword: String = "",
    val confirmPassword: String = "",
    val restorePassword: String = "",
    val isCreating: Boolean = false,
    val isRestoring: Boolean = false,
    val isDriveBusy: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val pendingBackupBytes: ByteArray? = null,
    val restorePreview: BackupPreview? = null,
    val showRestoreConfirm: Boolean = false,
    val showRestoreComplete: Boolean = false,
    val pendingPreview: BackupPreview? = null,
    val backupStatus: BackupStatus = BackupStatus.NEVER,
    val googleEmail: String? = null,
    val hasDriveBackup: Boolean = false
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository,
    private val authGate: AuthGate,
    private val googleAccountSession: GoogleAccountSession,
    private val googleDriveBackupClient: GoogleDriveBackupClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private var pendingRestoreBytes: ByteArray? = null

    init {
        refreshGoogleAccount()
        viewModelScope.launch {
            backupRepository.observeStorageInfo().collect { info ->
                _uiState.update { it.copy(backupStatus = info.backupStatus) }
            }
        }
    }

    fun refreshGoogleAccount() {
        val account = googleAccountSession.getSignedInAccount()
        _uiState.update {
            it.copy(
                googleEmail = account?.email,
                hasDriveBackup = account != null && it.backupStatus != BackupStatus.NEVER
            )
        }
    }

    fun getGoogleSignInIntent() = googleAccountSession.getAccountPickerIntent()

    fun onGoogleSignInResult(data: Intent?) {
        val email = data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (email.isNullOrBlank()) {
            _uiState.update {
                it.copy(errorMessage = context.getString(R.string.backup_google_sign_in_failed))
            }
            return
        }
        googleAccountSession.onAccountPicked(email)
        refreshGoogleAccount()
        _uiState.update {
            it.copy(successMessage = context.getString(R.string.backup_google_signed_in))
        }
    }

    fun switchGoogleAccount(onIntent: (Intent) -> Unit) {
        onIntent(googleAccountSession.switchAccountIntent())
        refreshGoogleAccount()
    }

    fun signOutGoogle() {
        googleAccountSession.clearAccount()
        refreshGoogleAccount()
        _uiState.update {
            it.copy(successMessage = context.getString(R.string.backup_google_signed_out))
        }
    }

    fun uploadEncryptedBackupToDrive(onShareIntent: (Intent) -> Unit) {
        val state = _uiState.value
        when {
            state.googleEmail.isNullOrBlank() ->
                _uiState.update { it.copy(errorMessage = context.getString(R.string.backup_google_sign_in_required)) }
            state.backupPassword.length < 8 ->
                _uiState.update { it.copy(errorMessage = context.getString(R.string.backup_password_min)) }
            state.backupPassword != state.confirmPassword ->
                _uiState.update { it.copy(errorMessage = context.getString(R.string.backup_password_mismatch)) }
            else -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isDriveBusy = true, errorMessage = null) }
                    backupRepository.createBackup(state.backupPassword.toCharArray())
                        .onSuccess { result ->
                            googleDriveBackupClient.createDriveShareIntent(result.encryptedBytes)
                                .onSuccess { intent ->
                                    backupRepository.recordBackupExport(
                                        fileName = GoogleDriveBackupClient.BACKUP_FILE_NAME,
                                        localPath = "drive:share:${state.googleEmail}",
                                        fileSize = result.encryptedBytes.size.toLong(),
                                        preview = result.preview
                                    )
                                    _uiState.update {
                                        it.copy(
                                            isDriveBusy = false,
                                            hasDriveBackup = true,
                                            backupPassword = "",
                                            confirmPassword = "",
                                            successMessage = context.getString(R.string.backup_drive_upload_success)
                                        )
                                    }
                                    onShareIntent(intent)
                                }
                                .onFailure {
                                    _uiState.update {
                                        it.copy(
                                            isDriveBusy = false,
                                            errorMessage = context.getString(R.string.backup_drive_upload_failed)
                                        )
                                    }
                                }
                        }
                        .onFailure {
                            _uiState.update {
                                it.copy(
                                    isDriveBusy = false,
                                    errorMessage = context.getString(R.string.backup_create_failed)
                                )
                            }
                        }
                }
            }
        }
    }

    fun downloadEncryptedBackupFromDrive() {
        // Restore from Drive uses the same SAF file picker (user opens .dfbackup from Drive).
        _uiState.update {
            it.copy(successMessage = context.getString(R.string.backup_drive_download_hint))
        }
    }

    fun onBackupPasswordChange(value: String) = _uiState.update { it.copy(backupPassword = value) }

    fun onConfirmPasswordChange(value: String) = _uiState.update { it.copy(confirmPassword = value) }

    fun onRestorePasswordChange(value: String) = _uiState.update { it.copy(restorePassword = value) }

    fun clearMessages() = _uiState.update { it.copy(errorMessage = null, successMessage = null) }

    fun prepareCreateBackup(onReady: (ByteArray, String) -> Unit) {
        val state = _uiState.value
        when {
            state.backupPassword.length < 8 ->
                _uiState.update { it.copy(errorMessage = context.getString(R.string.backup_password_min)) }
            state.backupPassword != state.confirmPassword ->
                _uiState.update { it.copy(errorMessage = context.getString(R.string.backup_password_mismatch)) }
            else -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isCreating = true, errorMessage = null) }
                    backupRepository.createBackup(state.backupPassword.toCharArray())
                        .onSuccess { result ->
                            _uiState.update {
                                it.copy(
                                    isCreating = false,
                                    pendingPreview = result.preview
                                )
                            }
                            onReady(result.encryptedBytes, defaultBackupFileName())
                        }
                        .onFailure {
                            backupRepository.markBackupFailed()
                            _uiState.update {
                                it.copy(isCreating = false, errorMessage = context.getString(R.string.backup_create_failed))
                            }
                        }
                }
            }
        }
    }

    fun onBackupSaved(fileName: String, uriPath: String, bytes: ByteArray) {
        val preview = _uiState.value.pendingPreview ?: return
        viewModelScope.launch {
            backupRepository.recordBackupExport(fileName, uriPath, bytes.size.toLong(), preview)
            _uiState.update {
                it.copy(
                    successMessage = context.getString(R.string.backup_saved_success),
                    backupPassword = "",
                    confirmPassword = "",
                    pendingPreview = null
                )
            }
        }
    }

    fun onBackupFileSelected(bytes: ByteArray) {
        pendingRestoreBytes = bytes
        _uiState.update { it.copy(restorePreview = null, showRestoreConfirm = false, errorMessage = null) }
    }

    fun validateRestoreFile() {
        val bytes = pendingRestoreBytes ?: return
        val password = _uiState.value.restorePassword.toCharArray()
        if (password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.backup_password_required)) }
            return
        }
        viewModelScope.launch {
            when (val result = backupRepository.validateRestore(bytes, password)) {
                is RestoreResult.PreviewReady ->
                    _uiState.update { it.copy(restorePreview = result.preview, showRestoreConfirm = true) }
                is RestoreResult.Error ->
                    _uiState.update { it.copy(errorMessage = result.message) }
                else -> Unit
            }
        }
    }

    fun dismissRestoreConfirm() = _uiState.update { it.copy(showRestoreConfirm = false) }

    fun confirmRestore(activity: FragmentActivity) {
        val bytes = pendingRestoreBytes ?: return
        val password = _uiState.value.restorePassword.toCharArray()
        viewModelScope.launch {
            when (
                val auth = authGate.authenticateForPurpose(activity, AuthPurpose.BACKUP_RESTORE)
            ) {
                is AuthResult.Cancelled, AuthResult.Failed ->
                    _uiState.update { it.copy(errorMessage = context.getString(R.string.auth_required_restore)) }
                is AuthResult.Error ->
                    _uiState.update { it.copy(errorMessage = auth.message) }
                is AuthResult.Success -> performRestore(bytes, password)
            }
        }
    }

    private fun performRestore(bytes: ByteArray, password: CharArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true, showRestoreConfirm = false) }
            when (val result = backupRepository.restoreBackup(bytes, password)) {
                is RestoreResult.Success ->
                    _uiState.update {
                        it.copy(isRestoring = false, showRestoreComplete = true, restorePreview = result.preview)
                    }
                is RestoreResult.Error ->
                    _uiState.update { it.copy(isRestoring = false, errorMessage = result.message) }
                else -> _uiState.update { it.copy(isRestoring = false) }
            }
        }
    }

    fun dismissRestoreComplete() = _uiState.update { it.copy(showRestoreComplete = false) }

    fun markBackupFailedPublic() {
        viewModelScope.launch { backupRepository.markBackupFailed() }
    }

    private fun defaultBackupFileName(): String =
        "docufind_backup_${System.currentTimeMillis()}.dfbackup"
}
