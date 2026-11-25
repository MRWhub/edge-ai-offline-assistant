package com.aichat.app.domain.usecase.extractor

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * Interface para extração de texto de múltiplos formatos.
 */
interface DocumentExtractor {
    
    fun supports(mimeType: String): Boolean
    
    /**
     * Extrai texto progressivamente de um documento, emitindo bloco a bloco (ex: por página).
     */
    suspend fun extractText(context: Context, uri: Uri): Flow<String>
}
