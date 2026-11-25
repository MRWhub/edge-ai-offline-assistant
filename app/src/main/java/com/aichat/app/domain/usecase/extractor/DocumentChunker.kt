package com.aichat.app.domain.usecase.extractor

/**
 * Utilitário determinístico para segmentar blocos gigantes de texto (Páginas ou Arquivos)
 * em "Chunks" semânticos controlados, focando em preservar frases e sentenças
 * para caber no limite da matriz matemática do modelo de Embedding (~500 a 1000 tokens).
 */
object DocumentChunker {
    
    /**
     * @param text Texto bruto extraído do PDF/Word.
     * @param maxChars Máximo de caracteres por Chunk (~1000 equivale a cerca de 250-300 tokens do Universal Sentence Encoder).
     * @param overlap Quantidade de caracteres que a janela "volta" para manter a conexão de contexto entre um bloco e outro.
     */
    fun chunkText(text: String, maxChars: Int = 1000, overlap: Int = 150): List<String> {
        if (text.isBlank()) return emptyList()
        
        val cleanText = text.replace(Regex("\\s+"), " ").trim()
        val chunks = mutableListOf<String>()
        var currentIndex = 0
        
        while (currentIndex < cleanText.length) {
            val endIndex = currentIndex + maxChars
            
            // Fim do texto alcançado
            if (endIndex >= cleanText.length) {
                chunks.add(cleanText.substring(currentIndex).trim())
                break
            }
            
            // 1. Tentar cortar exatamente no final de uma frase (ponto, exclamação ou interrogação)
            var breakPoint = cleanText.lastIndexOfAny(charArrayOf('.', '!', '?'), endIndex - 1)
            
            // 2. Se não houver fim de frase no bloco, tenta cortar no último espaço vazio (para não quebrar palavra)
            if (breakPoint <= currentIndex) {
                breakPoint = cleanText.lastIndexOf(' ', endIndex - 1)
            }
            
            // 3. Palavra bizarramente longa (ex: URL ou código base64), corta seco onde der
            if (breakPoint <= currentIndex) {
                breakPoint = endIndex - 1
            }
            
            val chunkText = cleanText.substring(currentIndex, breakPoint + 1).trim()
            if (chunkText.isNotEmpty()) {
                chunks.add(chunkText)
            }
            
            // Avança o índice "recuando" o overlap para não perder o gancho
            currentIndex = breakPoint + 1 - overlap
            
            // Proteção contra Loop Infinito
            if (currentIndex <= breakPoint + 1 - maxChars) {
                currentIndex = breakPoint + 1
            }
        }
        
        return chunks.filter { it.isNotBlank() }
    }
}
