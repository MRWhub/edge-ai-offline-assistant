package com.aichat.app.domain.usecase

import com.aichat.app.data.local.entity.KnowledgeChunk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RagPromptFormatterTest {

    private val formatter = RagPromptFormatter()

    @Test
    fun `when relevant chunks are empty, it should return only the original user prompt`() {
        // Arrange
        val userQuestion = "Qual é a receita de pão?"
        val chunks = emptyList<KnowledgeChunk>()

        // Act
        val result = formatter.formatPromptWithContext(userQuestion, chunks)

        // Assert
        assertEquals(userQuestion, result)
        // O C++ vai adicionar as tags <|im_start|> user... então aqui não podemos ter formatações
    }

    @Test
    fun `when relevant chunks exist, it should append the context and source above the question`() {
        // Arrange
        val userQuestion = "Como faço para trocar o pneu?"
        val chunks = listOf(
            KnowledgeChunk(
                textContent = "Para trocar o pneu, use o macaco e a chave de roda.",
                sourceName = "manual_do_carro.pdf"
            )
        )

        // Act
        val result = formatter.formatPromptWithContext(userQuestion, chunks)

        // Assert
        assertTrue("Deve conter a instrução informando que é contexto recuperado", result.contains("recuperadas de seus documentos locais"))
        assertTrue("Deve conter a string exata do chunk", result.contains("Para trocar o pneu, use o macaco e a chave de roda."))
        assertTrue("Deve identificar a fonte do documento", result.contains("[Fonte: manual_do_carro.pdf]"))
        assertTrue("Deve manter a pergunta original no final do prompt", result.endsWith(userQuestion))
    }
}
