package com.aichat.app.domain.usecase

import com.aichat.app.data.local.entity.KnowledgeChunk
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RagPromptFormatter @Inject constructor() {

    /**
     * Mescla o texto da pergunta do usuário com os pedaços de conhecimento 
     * recuperados do RAG, preparando uma string "limpa" compatível 
     * com o wrapper nativo JNI (que injetará o ChatML no lado C++).
     */
    fun formatPromptWithContext(userQuestion: String, relevantChunks: List<KnowledgeChunk>): String {
        if (relevantChunks.isEmpty()) {
            return userQuestion
        }

        val contextBuilder = StringBuilder()
        contextBuilder.append("Abaixo estão informações de contexto recuperadas de seus documentos locais:\\n\\n")
        
        relevantChunks.forEach { chunk ->
            val source = if (chunk.sourceName.isNotBlank()) " [Fonte: ${chunk.sourceName}]" else ""
            contextBuilder.append("- \"${chunk.textContent}\"$source\\n")
        }

        contextBuilder.append("\\nCom base estritamente nas informações locais fornecidas acima (e apenas se aplicável ao contexto), responda à seguinte pergunta do usuário:\\n\\n")
        contextBuilder.append(userQuestion)

        return contextBuilder.toString()
    }
}
