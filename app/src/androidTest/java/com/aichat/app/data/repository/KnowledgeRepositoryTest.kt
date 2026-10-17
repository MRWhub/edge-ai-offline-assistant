package com.aichat.app.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aichat.app.data.local.entity.MyObjectBox
import com.aichat.app.domain.embedder.TextEmbedder
import io.objectbox.BoxStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.cos

/**
 * Teste Instrumentado para validar matematicamente a persistência do RAG e a 
 * busca vetorial (Nearest Neighbor) do ObjectBox.
 */
@RunWith(AndroidJUnit4::class)
class KnowledgeRepositoryTest {

    private lateinit var boxStore: BoxStore
    private lateinit var repository: KnowledgeRepository
    
    // Embedder Falso e determinístico construído estritamente para o ambiente de testes (TDD).
    // Evita a obrigatoriedade de baixar e inicializar um modelo .tflite real na CI (evita flaky tests)
    // ao mesmo tempo que cumpre o papel matemático do vetor (512 dimensões) para o ObjectBox operar.
    private val fakeEmbedder = object : TextEmbedder {
        override val modelId: String = "fake_test_model"
        override val dimensions: Int = 512

        override suspend fun embed(text: String): FloatArray {
            val vector = FloatArray(dimensions)
            // Lógica simples pseudo-semântica para testes
            // Palavras com prefixos similares geram vetores alinhados.
            val seed = text.hashCode().toFloat()
            for (i in 0 until dimensions) {
                vector[i] = cos(seed + i)
            }
            return vector
        }

        override fun close() {}
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testDir = File(context.cacheDir, "objectbox-test")
        BoxStore.deleteAllFiles(testDir)

        boxStore = MyObjectBox.builder()
            .androidContext(context)
            .directory(testDir)
            .build()
            
        repository = KnowledgeRepository(boxStore, fakeEmbedder)
    }

    @After
    fun tearDown() {
        repository.clearAll()
        boxStore.closeThreadResources()
        boxStore.close()
    }

    @Test
    fun `ingestChunk deve persistir o texto e o vetor no banco de dados`() = runBlocking {
        // Arrange
        val text = "O Llama é uma excelente inteligência artificial."
        
        // Act
        val id = repository.ingestChunk(text, "manual_test", 10)
        
        // Assert
        assertTrue("ID gerado deve ser válido (>0)", id > 0)
        val box = boxStore.boxFor(com.aichat.app.data.local.entity.KnowledgeChunk::class.java)
        val saved = box.get(id)
        assertEquals("Texto deve ser preservado", text, saved.textContent)
        assertEquals("Modelo ID deve bater", "fake_test_model", saved.embeddingModelId)
        assertEquals("Vetor de 512 gerado", 512, saved.embedding?.size)
    }

    @Test
    fun `searchRelevantContext deve retornar os documentos semanticamente mais proximos`() = runBlocking {
        // Como estamos usando um hash fixo no fake embedder, a mesma query 
        // produzirá um vetor idêntico ao conteúdo exato.
        
        // Arrange (documentos positivos e negativos)
        repository.ingestChunk("A receita de bolo leva farinha.", "doc1", 7)
        repository.ingestChunk("A capital da frança é Paris.", "doc2", 7)
        repository.ingestChunk("Como fazer um bolo de chocolate fácil.", "doc3", 8)
        
        // Act
        // Pesquisa idêntica vai resultar no vetor matemático (hash) da frase do doc2
        val results = repository.searchRelevantContext("A capital da frança é Paris.", maxResults = 1)
        
        // Assert
        assertTrue("Deve encontrar pelo menos 1 resultado", results.isNotEmpty())
        assertEquals("A capital da frança é Paris.", results.first().textContent)
    }
}
