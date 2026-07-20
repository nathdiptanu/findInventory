package com.docufind.app.ocr

sealed interface OcrProgress {
    data object Preparing : OcrProgress
    data object Reading : OcrProgress
    data class ProcessingPage(val page: Int, val totalPages: Int) : OcrProgress
    data object Saving : OcrProgress
}

sealed interface OcrResult {
    data class Success(val text: String) : OcrResult
    data class Failure(val reason: OcrFailureReason, val cause: Throwable? = null) : OcrResult
    data object Empty : OcrResult
}

enum class OcrFailureReason {
    UNSUPPORTED_MIME,
    FILE_TOO_LARGE,
    CORRUPT_FILE,
    PASSWORD_PROTECTED_PDF,
    CANCELLED,
    ENGINE_ERROR
}
