package com.aichat.app.data.local.entity

import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id

@Entity
class KnowledgeChunk(
    @Id var id: Long = 0,
    
    // O texto original extraído do documento/áudio/chat
    var textContent: String = "",
    
    // Referência da fonte (ex: "nome_do_arquivo.pdf" ou "user_chat")
    var sourceName: String = "",
    
    // Timestamp para filtragem de relevância (RF04)
    var timestamp: Long = System.currentTimeMillis(),

    // O vetor gerado por embeddings que será pesquisado pelo ObjectBox
    @HnswIndex(dimensions = 384) 
    var embedding: FloatArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as KnowledgeChunk
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
