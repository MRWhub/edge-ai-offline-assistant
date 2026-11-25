package com.aichat.app.di

import android.content.Context
import com.aichat.app.AiChatApplication
import com.aichat.app.data.local.embedder.MediaPipeEmbedderImpl
import com.aichat.app.data.repository.KnowledgeRepository
import com.aichat.app.domain.embedder.TextEmbedder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.objectbox.BoxStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object KnowledgeModule {

    @Provides
    @Singleton
    fun provideBoxStore(@ApplicationContext context: Context): BoxStore {
        return (context as AiChatApplication).objectBoxStore
    }

    @Provides
    @Singleton
    fun provideTextEmbedder(@ApplicationContext context: Context): TextEmbedder {
        return MediaPipeEmbedderImpl(context)
    }

    @Provides
    @Singleton
    fun providePdfNativeExtractor(): com.aichat.app.domain.usecase.extractor.DocumentExtractor {
        return com.aichat.app.data.local.extractor.PdfNativeExtractorImpl()
    }

    @Provides
    @Singleton
    fun provideKnowledgeRepository(
        boxStore: BoxStore,
        embedder: TextEmbedder
    ): KnowledgeRepository {
        return KnowledgeRepository(boxStore, embedder)
    }
}
