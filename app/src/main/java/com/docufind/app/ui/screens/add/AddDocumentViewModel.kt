package com.docufind.app.ui.screens.add



import android.net.Uri

import androidx.annotation.StringRes

import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope

import com.docufind.app.R
import com.docufind.app.di.IoDispatcher

import com.docufind.app.domain.model.CategoryFieldRegistry

import com.docufind.app.domain.model.PendingAttachment

import com.docufind.app.domain.model.PendingAttachmentEntry

import com.docufind.app.domain.model.SaveDocumentRequest

import com.docufind.app.domain.model.SaveDocumentResult

import com.docufind.app.domain.model.VaultCategory

import com.docufind.app.domain.repository.PreferencesRepository

import com.docufind.app.domain.repository.VaultRecordRepository

import com.docufind.app.ocr.OcrEngine

import com.docufind.app.ocr.OcrFieldSuggestions

import com.docufind.app.ocr.OcrInputValidator

import com.docufind.app.ocr.OcrProgress

import com.docufind.app.ocr.OcrResult

import com.docufind.app.ocr.OcrFailureReason
import com.docufind.app.ocr.OcrResultMapper

import com.docufind.app.ocr.OcrTempStore

import com.docufind.app.ocr.OcrValidationResult

import com.docufind.app.ocr.pdfExtensionForMime

import com.docufind.app.security.pin.PinManager

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.CancellationException

import kotlinx.coroutines.CoroutineDispatcher

import kotlinx.coroutines.Job

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.update

import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext

import java.io.File

import java.time.Instant

import java.time.ZoneId

import java.util.UUID

import javax.inject.Inject



data class AddDocumentUiState(

    val title: String = "",

    val categoryId: String = "",

    val subCategory: String = "",

    val familyMemberId: String? = null,

    val petId: String? = null,

    val issueDate: Long? = null,

    val expiryDate: Long? = null,

    val notes: String = "",

    val tagsText: String = "",

    val reminderEnabled: Boolean = false,

    val defaultReminderTimeMinutes: Int = 9 * 60,

    val attachments: List<PendingAttachmentEntry> = emptyList(),

    val categoryFieldValues: Map<String, String> = emptyMap(),

    val familyMembers: List<com.docufind.app.domain.model.FamilyMemberOption> = emptyList(),

    val pets: List<com.docufind.app.domain.model.PetOption> = emptyList(),

    val isSaving: Boolean = false,

    val showConfirmDialog: Boolean = false,

    val showExpiryBeforeIssueDialog: Boolean = false,

    val pendingExpiryDate: Long? = null,

    val pendingSaveAfterExpiryConfirm: Boolean = false,

    val showSuccessMessage: Boolean = false,

    val savedRecordId: String? = null,

    val showPinSetupGate: Boolean = false,

    val pendingSaveAfterPinSetup: Boolean = false,

    val errorMessage: String? = null,

    val isOcrRunning: Boolean = false,

    val ocrProgress: OcrProgress? = null,

    val showOcrReview: Boolean = false,

    val ocrReviewText: String = "",

    @StringRes val ocrStatusMessageResId: Int? = null,

    val showDuplicateDialog: Boolean = false,

    val duplicateTitles: List<String> = emptyList()

) {

    val selectedCategory: VaultCategory?

        get() = VaultCategory.fromId(categoryId)



    val subCategoryOptions: List<String>

        get() = selectedCategory?.subCategories.orEmpty()



    val showIssueDate: Boolean

        get() = categoryId.isBlank() || CategoryFieldRegistry.usesIssueDate(categoryId)



    val showExpiryDate: Boolean

        get() = categoryId.isBlank() || CategoryFieldRegistry.usesExpiryDate(categoryId)

}



@HiltViewModel

class AddDocumentViewModel @Inject constructor(

    private val vaultRecordRepository: VaultRecordRepository,

    private val preferencesRepository: PreferencesRepository,

    private val pinManager: PinManager,

    private val ocrEngine: OcrEngine,

    private val ocrTempStore: OcrTempStore,

    @IoDispatcher private val ioDispatcher: CoroutineDispatcher

) : ViewModel() {



    private val _uiState = MutableStateFlow(AddDocumentUiState())

    val uiState: StateFlow<AddDocumentUiState> = _uiState.asStateFlow()



    private var ocrJob: Job? = null



    init {

        viewModelScope.launch {

            vaultRecordRepository.observeFamilyMembers().collect { members ->

                _uiState.update { it.copy(familyMembers = members) }

            }

        }

        viewModelScope.launch {

            vaultRecordRepository.observePets().collect { pets ->

                _uiState.update { it.copy(pets = pets) }

            }

        }

        viewModelScope.launch {

            preferencesRepository.preferences.collect { preferences ->

                _uiState.update { it.copy(defaultReminderTimeMinutes = preferences.defaultReminderTimeMinutes) }

            }

        }

    }



    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value, errorMessage = null) }



    fun onCategoryChange(categoryId: String) = _uiState.update {

        it.copy(

            categoryId = categoryId,

            subCategory = "",

            categoryFieldValues = emptyMap(),

            reminderEnabled = CategoryFieldRegistry.showReminderByDefault(categoryId),

            errorMessage = null

        )

    }



    fun onSubCategoryChange(value: String) = _uiState.update { it.copy(subCategory = value) }



    fun onFamilyMemberChange(id: String?) = _uiState.update { it.copy(familyMemberId = id) }



    fun onPetChange(id: String?) = _uiState.update { it.copy(petId = id) }



    fun onIssueDateChange(epochMillis: Long?) = _uiState.update { it.copy(issueDate = epochMillis) }



    fun onExpiryDateChange(epochMillis: Long?) {

        if (epochMillis == null) {

            _uiState.update { it.copy(expiryDate = null) }

            return

        }

        val issue = _uiState.value.issueDate

        if (issue != null && isDateBefore(epochMillis, issue)) {

            _uiState.update {

                it.copy(

                    pendingExpiryDate = epochMillis,

                    showExpiryBeforeIssueDialog = true,

                    pendingSaveAfterExpiryConfirm = false

                )

            }

        } else {

            _uiState.update { it.copy(expiryDate = epochMillis) }

        }

    }



    fun onCategoryFieldChange(key: String, value: String) = _uiState.update {

        it.copy(categoryFieldValues = it.categoryFieldValues + (key to value))

    }



    fun confirmExpiryBeforeIssue() {

        val pendingSave = _uiState.value.pendingSaveAfterExpiryConfirm

        _uiState.update {

            it.copy(

                expiryDate = it.pendingExpiryDate ?: it.expiryDate,

                pendingExpiryDate = null,

                showExpiryBeforeIssueDialog = false,

                pendingSaveAfterExpiryConfirm = false

            )

        }

        if (pendingSave) proceedToSaveConfirmation()

    }



    fun dismissExpiryBeforeIssueDialog() {

        _uiState.update {

            it.copy(

                pendingExpiryDate = null,

                showExpiryBeforeIssueDialog = false,

                pendingSaveAfterExpiryConfirm = false

            )

        }

    }



    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }



    fun onTagsChange(value: String) = _uiState.update { it.copy(tagsText = value) }



    fun onReminderToggle(enabled: Boolean) = _uiState.update { it.copy(reminderEnabled = enabled) }



    fun onAttachmentSelected(info: PendingAttachmentInfo) {

        val entry = PendingAttachmentEntry(

            id = UUID.randomUUID().toString(),

            uri = info.uri,

            attachment = PendingAttachment(

                displayName = info.displayName,

                mimeType = info.mimeType,

                sizeBytes = info.sizeBytes,

                localPreviewPath = info.localPreviewPath

            )

        )

        _uiState.update {

            it.copy(attachments = it.attachments + entry, errorMessage = null)

        }

        maybeStartOcr(info)

    }



    fun cancelOcr() {

        ocrJob?.cancel()

    }



    fun acceptOcrReview(text: String) {

        applyAcceptedOcrText(text)

        dismissOcrReview()

    }



    fun dismissOcrReview() {

        _uiState.update {

            it.copy(showOcrReview = false, ocrReviewText = "")

        }

    }



    fun consumeOcrStatusMessage() {

        _uiState.update { it.copy(ocrStatusMessageResId = null) }

    }



    fun removeAttachment(id: String) = _uiState.update {

        it.copy(attachments = it.attachments.filterNot { entry -> entry.id == id })

    }



    fun onSaveClicked() {

        val state = _uiState.value

        when {

            state.title.isBlank() -> _uiState.update { it.copy(errorMessage = "Document title is required.") }

            state.categoryId.isBlank() -> _uiState.update { it.copy(errorMessage = "Please select a category.") }

            !pinManager.hasPinConfigured() -> _uiState.update {

                it.copy(showPinSetupGate = true, pendingSaveAfterPinSetup = true)

            }

            state.issueDate != null && state.expiryDate != null &&

                isDateBefore(state.expiryDate, state.issueDate) -> {

                _uiState.update {

                    it.copy(

                        pendingExpiryDate = state.expiryDate,

                        showExpiryBeforeIssueDialog = true,

                        pendingSaveAfterExpiryConfirm = true

                    )

                }

            }

            else -> proceedToSaveConfirmation()

        }

    }



    private fun proceedToSaveConfirmation() {
        viewModelScope.launch {
            val state = _uiState.value
            val duplicates = vaultRecordRepository.findLikelyDuplicates(
                categoryId = state.categoryId,
                title = state.title,
                categoryFieldValues = state.categoryFieldValues
            )
            if (duplicates.isNotEmpty()) {
                _uiState.update {
                    it.copy(showDuplicateDialog = true, duplicateTitles = duplicates)
                }
                return@launch
            }
            continueSaveAfterDuplicateCheck()
        }
    }

    fun dismissDuplicateDialog() =
        _uiState.update { it.copy(showDuplicateDialog = false, duplicateTitles = emptyList()) }

    fun confirmSaveDespiteDuplicate() {
        _uiState.update { it.copy(showDuplicateDialog = false, duplicateTitles = emptyList()) }
        continueSaveAfterDuplicateCheck()
    }

    private fun continueSaveAfterDuplicateCheck() {
        val state = _uiState.value
        if (state.attachments.isNotEmpty()) {
            _uiState.update { it.copy(showConfirmDialog = true) }
        } else {
            confirmSave()
        }
    }



    fun onPinSetupComplete() {

        _uiState.update { it.copy(showPinSetupGate = false) }

        if (_uiState.value.pendingSaveAfterPinSetup) {

            _uiState.update { it.copy(pendingSaveAfterPinSetup = false) }

            onSaveClicked()

        }

    }



    fun dismissPinSetupGate() {

        _uiState.update { it.copy(showPinSetupGate = false, pendingSaveAfterPinSetup = false) }

    }



    fun dismissConfirmDialog() = _uiState.update { it.copy(showConfirmDialog = false) }



    fun confirmSave() {

        if (_uiState.value.isSaving) return

        _uiState.update { it.copy(showConfirmDialog = false, isSaving = true, errorMessage = null) }

        viewModelScope.launch {

            val state = _uiState.value

            val result = vaultRecordRepository.saveDocument(

                SaveDocumentRequest(

                    title = state.title,

                    categoryId = state.categoryId,

                    subCategory = state.subCategory.takeIf { it.isNotBlank() },

                    familyMemberId = state.familyMemberId,

                    petId = state.petId,

                    issueDate = state.issueDate,

                    expiryDate = state.expiryDate,

                    notes = state.notes,

                    tags = AttachmentHelper.parseTags(state.tagsText),

                    reminderEnabled = state.reminderEnabled,

                    attachments = state.attachments,

                    categoryFieldValues = state.categoryFieldValues

                )

            )

            when (result) {

                is SaveDocumentResult.Success -> {

                    _uiState.value = AddDocumentUiState(

                        showSuccessMessage = true,

                        savedRecordId = result.recordId

                    )

                }

                is SaveDocumentResult.Error -> {

                    _uiState.update { it.copy(isSaving = false, errorMessage = result.message) }

                }

            }

        }

    }



    fun consumeSuccess() {

        _uiState.update { it.copy(showSuccessMessage = false, savedRecordId = null) }

    }



    fun clearError() = _uiState.update { it.copy(errorMessage = null) }



    override fun onCleared() {

        ocrJob?.cancel()

        super.onCleared()

    }



    private fun maybeStartOcr(info: PendingAttachmentInfo) {

        if (!OcrInputValidator.isOcrEligible(info.mimeType)) return



        when (val validation = OcrInputValidator.validate(info.mimeType, info.sizeBytes)) {

            is OcrValidationResult.Valid -> startOcr(info, validation.mimeType)

            else -> {

                val reason = OcrInputValidator.validationFailureReason(validation) ?: return

                _uiState.update {

                    it.copy(ocrStatusMessageResId = OcrResultMapper.failureMessageResId(reason))

                }

            }

        }

    }



    private fun startOcr(info: PendingAttachmentInfo, mimeType: String) {

        ocrJob?.cancel()

        ocrJob = viewModelScope.launch {

            _uiState.update {

                it.copy(

                    isOcrRunning = true,

                    ocrProgress = OcrProgress.Preparing,

                    ocrStatusMessageResId = null,

                    showOcrReview = false,

                    ocrReviewText = ""

                )

            }



            var tempFile: File? = null

            try {

                tempFile = withContext(ioDispatcher) {

                    ocrTempStore.copyUriToTemp(

                        uri = Uri.parse(info.uri),

                        extension = pdfExtensionForMime(mimeType)

                    )

                }



                val result = ocrEngine.recognize(tempFile, mimeType) { progress ->

                    _uiState.update { state -> state.copy(ocrProgress = progress) }

                }



                when (result) {

                    is OcrResult.Success -> {

                        _uiState.update {

                            it.copy(

                                isOcrRunning = false,

                                ocrProgress = null,

                                showOcrReview = true,

                                ocrReviewText = result.text

                            )

                        }

                    }

                    else -> {

                        _uiState.update {

                            it.copy(

                                isOcrRunning = false,

                                ocrProgress = null,

                                ocrStatusMessageResId = OcrResultMapper.messageResId(result)

                            )

                        }

                    }

                }

            } catch (_: CancellationException) {

                _uiState.update {

                    it.copy(

                        isOcrRunning = false,

                        ocrProgress = null,

                        ocrStatusMessageResId = OcrResultMapper.failureMessageResId(
                            OcrFailureReason.CANCELLED
                        )

                    )

                }

            } catch (_: Exception) {

                _uiState.update {

                    it.copy(

                        isOcrRunning = false,

                        ocrProgress = null,

                        ocrStatusMessageResId = OcrResultMapper.failureMessageResId(
                            OcrFailureReason.ENGINE_ERROR
                        )

                    )

                }

            } finally {

                withContext(ioDispatcher) {

                    ocrTempStore.wipeTemp(tempFile)

                }

            }

        }

    }



    private fun applyAcceptedOcrText(text: String) {
        _uiState.update { state ->
            val applied = OcrAcceptedTextStorage.apply(state.notes, state.tagsText, text)
            val suggestions = OcrFieldSuggestions.extract(text, state.categoryId)
            val mergedFields = state.categoryFieldValues.toMutableMap()
            suggestions.fieldValues.forEach { (key, value) ->
                if (mergedFields[key].isNullOrBlank()) {
                    mergedFields[key] = value
                }
            }
            state.copy(
                notes = applied.notes,
                tagsText = applied.tagsText,
                issueDate = state.issueDate ?: suggestions.issueDateMillis,
                expiryDate = state.expiryDate ?: suggestions.expiryDateMillis,
                categoryFieldValues = mergedFields,
                ocrStatusMessageResId = R.string.ocr_verify_suggested_fields
            )
        }
    }



    private fun isDateBefore(candidateMillis: Long, referenceMillis: Long): Boolean {

        val candidate = Instant.ofEpochMilli(candidateMillis)

            .atZone(ZoneId.systemDefault())

            .toLocalDate()

        val reference = Instant.ofEpochMilli(referenceMillis)

            .atZone(ZoneId.systemDefault())

            .toLocalDate()

        return candidate.isBefore(reference)

    }

}


