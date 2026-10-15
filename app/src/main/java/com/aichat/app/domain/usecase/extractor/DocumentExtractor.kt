package com.aichat.app.domain.usecase.extractor

import java.io.File

/**
 * Interface para extração de texto de múltiplos formatos (PDF, DOCX, TXT, etc).
 * A implementação dessa interface converterá o arquivo em texto bruto para ser 
 * "chunkado" e inserido no ObjectBox.
 */
interface DocumentExtractor {
    
    /**
     * Verifica se o arquivo é suportado pelo extrator.
     */
    fun supports(file: File): Boolean
    
    /**
     * Extrai todo o texto possível do arquivo.
     */
    fun extractText(file: File): String
}
