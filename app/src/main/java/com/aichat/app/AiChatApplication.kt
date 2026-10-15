package com.aichat.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.objectbox.BoxStore
import com.aichat.app.data.local.entity.MyObjectBox

@HiltAndroidApp
class AiChatApplication : Application() {

    lateinit var objectBoxStore: BoxStore
        private set

    override fun onCreate() {
        super.onCreate()
        
        // RNF02, RNF03, RNF04: Dados mantidos ocultos e localmente.
        // O ObjectBox iniciará o banco de dados de RAG dentro do diretório interno
        // da aplicação (Context.filesDir), protegendo-o de acessos externos e outras aplicações.
        objectBoxStore = MyObjectBox.builder()
            .androidContext(this)
            .baseDirectory(filesDir) // Garante o armazenamento no Internal Storage 
            .build()
    }
}
