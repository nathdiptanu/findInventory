package com.docufind.app.di

import com.docufind.app.ocr.DefaultTextRecognizerProvider
import com.docufind.app.ocr.MlKitOcrEngine
import com.docufind.app.ocr.OcrEngine
import com.docufind.app.ocr.TextRecognizerProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OcrModule {
    @Binds
    @Singleton
    abstract fun bindOcrEngine(impl: MlKitOcrEngine): OcrEngine

    @Binds
    @Singleton
    abstract fun bindTextRecognizerProvider(impl: DefaultTextRecognizerProvider): TextRecognizerProvider
}
