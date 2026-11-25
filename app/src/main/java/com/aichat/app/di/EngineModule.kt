package com.aichat.app.di

import android.content.Context
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import com.aichat.app.data.local.manager.GgufModelManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    @Singleton
    fun provideInferenceEngine(@ApplicationContext context: Context): InferenceEngine {
        // Initializes the native engine and returns the singleton wrapper
        return AiChat.getInferenceEngine(context)
    }

    @Provides
    @Singleton
    fun provideGgufModelManager(@ApplicationContext context: Context): GgufModelManager {
        return GgufModelManager(context)
    }
}
