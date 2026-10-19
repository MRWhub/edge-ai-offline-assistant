package com.aichat.app.data.repository

import com.aichat.app.data.local.entity.KnowledgeChunk
import com.aichat.app.data.local.entity.KnowledgeChunk_
import com.aichat.app.domain.embedder.TextEmbedder
import com.aichat.app.domain.extractor.PortugueseKeywordExtractor
import io.objectbox.Box
import io.objectbox.BoxStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositório responsável por orquestrar a ingestão de texto (Embedder -> ObjectBox)
 * e a recuperação de contexto (RAG Query).
 */
class KnowledgeRepository(
    private val boxStore: BoxStore,
    private val embedder: TextEmbedder
) {
    private val chunkBox: Box<KnowledgeChunk> = boxStore.boxFor(KnowledgeChunk::class.java)
    private val keywordExtractor = PortugueseKeywordExtractor()

    /**
     * Ingere um trecho de texto, extrai palavras-chave, gera o embedding e salva no ObjectBox.
     */
    suspend fun ingestChunk(text: String, sourceName: String, estimatedTokens: Int): Long = withContext(Dispatchers.IO) {
        val vector = embedder.embed(text)
        
        // Extrai até 5 termos chave para índice lexical (Busca híbrida)
        val extractedKeywords = keywordExtractor.extract(text, maxKeywords = 5).joinToString(",")
        
        val chunk = KnowledgeChunk(
            textContent = text,
            sourceName = sourceName,
            embeddingModelId = embedder.modelId,
            tokenCount = estimatedTokens,
            keywords = extractedKeywords,
            embedding = vector
        )
        
        chunkBox.put(chunk)
    }

    /**
     * Busca os N trechos mais relevantes do banco de dados com base na similaridade matemática (HNSW).
     */
    suspend fun searchRelevantContext(query: String, maxResults: Int = 3): List<KnowledgeChunk> = withContext(Dispatchers.IO) {
        if (chunkBox.isEmpty) return@withContext emptyList()

        val queryVector = embedder.embed(query)
        
        // ObjectBox v4 API para busca de Nearest Neighbors
        val queryBuilder = chunkBox.query()
            .nearestNeighbors(KnowledgeChunk_.embedding, queryVector, maxResults)
            
        val results = queryBuilder.build().find()
        
        // Podemos adicionar filtragem baseada no `modelId` no futuro
        // se migrarmos de modelo e os vetores antigos ainda estiverem no banco.
        results.filter { it.embeddingModelId == embedder.modelId }
    }
    
    /**
     * Limpa o banco de conhecimentos.
     */
    fun clearAll() {
        chunkBox.removeAll()
    }
}
