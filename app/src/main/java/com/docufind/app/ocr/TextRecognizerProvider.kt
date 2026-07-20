package com.docufind.app.ocr

import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import javax.inject.Inject
import javax.inject.Singleton

interface TextRecognizerProvider {
    fun create(): TextRecognizer
}

@Singleton
class DefaultTextRecognizerProvider @Inject constructor() : TextRecognizerProvider {
    override fun create(): TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
}
