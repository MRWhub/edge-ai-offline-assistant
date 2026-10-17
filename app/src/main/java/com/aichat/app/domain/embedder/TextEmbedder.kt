package com.aichat.app.domain.embedder

/**
 * Contrato arquitetural da camada de domínio para geração de vetores (Embeddings).
 * Isolado propositalmente da JNI do Llama.cpp e de implementações específicas (MediaPipe, etc).
 */
interface TextEmbedder {
    
    /**
     * O ID (nome/versão) do modelo carregado, para registro no banco de dados.
     */
    val modelId: String

    /**
     * Tamanho do vetor resultante gerado por este modelo (ex: 512).
     */
    val dimensions: Int

    /**
     * Converte um texto limpo em uma representação vetorial matemática (Embedding).
     * Lança exceções caso o modelo não esteja pronto.
     */
    suspend fun embed(text: String): FloatArray

    /**
     * Descarrega o modelo da memória (se suportado).
     */
    fun close()
}
