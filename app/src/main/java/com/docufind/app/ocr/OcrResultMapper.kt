package com.docufind.app.ocr

import com.docufind.app.R

object OcrResultMapper {
    fun messageResId(result: OcrResult): Int? = when (result) {
        is OcrResult.Success -> null
        OcrResult.Empty -> R.string.ocr_empty
        is OcrResult.Failure -> failureMessageResId(result.reason)
    }

    fun failureMessageResId(reason: OcrFailureReason): Int = when (reason) {
        OcrFailureReason.UNSUPPORTED_MIME -> R.string.ocr_unsupported_mime
        OcrFailureReason.FILE_TOO_LARGE -> R.string.file_too_large
        OcrFailureReason.CORRUPT_FILE -> R.string.ocr_corrupt_file
        OcrFailureReason.PASSWORD_PROTECTED_PDF -> R.string.ocr_password_protected_pdf
        OcrFailureReason.CANCELLED -> R.string.ocr_cancelled
        OcrFailureReason.ENGINE_ERROR -> R.string.ocr_failed
    }

    fun progressMessageResId(progress: OcrProgress): Int = when (progress) {
        OcrProgress.Preparing -> R.string.ocr_progress_preparing
        OcrProgress.Reading -> R.string.ocr_progress_reading
        is OcrProgress.ProcessingPage -> R.string.ocr_progress_page
        OcrProgress.Saving -> R.string.ocr_progress_saving
    }

    fun progressMessageArgs(progress: OcrProgress): Array<Any> = when (progress) {
        is OcrProgress.ProcessingPage -> arrayOf(progress.page, progress.totalPages)
        else -> emptyArray()
    }
}
