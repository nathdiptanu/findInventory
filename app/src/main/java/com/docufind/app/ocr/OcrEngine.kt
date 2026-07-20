package com.docufind.app.ocr

import java.io.File

interface OcrEngine {
    suspend fun recognize(
        file: File,
        mimeType: String,
        onProgress: suspend (OcrProgress) -> Unit = {}
    ): OcrResult
}
