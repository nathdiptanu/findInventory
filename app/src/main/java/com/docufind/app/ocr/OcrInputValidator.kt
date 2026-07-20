package com.docufind.app.ocr

import com.docufind.app.security.file.MAX_VAULT_FILE_BYTES
import com.docufind.app.security.file.SupportedMimeType

sealed interface OcrValidationResult {
    data class Valid(val mimeType: String) : OcrValidationResult
    data object UnsupportedMime : OcrValidationResult
    data object FileTooLarge : OcrValidationResult
    data object UnknownSize : OcrValidationResult
}

object OcrInputValidator {
    fun validate(mimeType: String?, sizeBytes: Long): OcrValidationResult {
        val resolvedMime = mimeType?.takeIf { SupportedMimeType.fromMime(it) != null }
            ?: return OcrValidationResult.UnsupportedMime
        if (sizeBytes <= 0L) return OcrValidationResult.UnknownSize
        if (sizeBytes > MAX_VAULT_FILE_BYTES) return OcrValidationResult.FileTooLarge
        return OcrValidationResult.Valid(resolvedMime)
    }

    fun isOcrEligible(mimeType: String): Boolean =
        SupportedMimeType.fromMime(mimeType) != null

    fun validationFailureReason(result: OcrValidationResult): OcrFailureReason? = when (result) {
        is OcrValidationResult.Valid -> null
        OcrValidationResult.UnsupportedMime -> OcrFailureReason.UNSUPPORTED_MIME
        OcrValidationResult.FileTooLarge -> OcrFailureReason.FILE_TOO_LARGE
        OcrValidationResult.UnknownSize -> OcrFailureReason.CORRUPT_FILE
    }
}
