package com.aichat.app.domain.extractor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PortugueseKeywordExtractorTest {

    private val extractor = PortugueseKeywordExtractor()

    @Test
    fun `extract deve normalizar acentos e pontuacao`() {
        val text = "Atenção: o João comprou maçãs, pães e água na padaria!"
        val keywords = extractor.extract(text)
        
        // Verifica se termos com acento e pontuação limpos sobreviveram
        assertTrue(keywords.contains("atencao"))
        assertTrue(keywords.contains("joao"))
        assertTrue(keywords.contains("macas"))
        assertTrue(keywords.contains("paes"))
    }

    @Test
    fun `extract deve remover stopwords isoladas mas mante-las no meio de n-gramas`() {
        val text = "O sistema de processamento de linguagem natural é muito bom."
        val keywords = extractor.extract(text)
        
        // O N-Grama completo deve sobreviver (Bônus de expressão composta)
        assertTrue(keywords.contains("processamento de linguagem"))
        
        // Stopwords exclusivas como "o", "de", "e" não devem aparecer soltas
        assertFalse(keywords.contains("de"))
        assertFalse(keywords.contains("o"))
    }

    @Test
    fun `extract deve ordenar por frequencia e tamanho deterministamente`() {
        val text = "Gato come rato. Gato dorme. Gato preto. Cachorro late. Gato come rato de novo."
        val keywords = extractor.extract(text, maxKeywords = 3)
        
        // A expressão "gato come rato" aparece 2 vezes (n-grama de 3 palavras).
        // "gato" solto aparece 4 vezes. Pela heurística, expressões compostas ganham um peso
        // alto por serem mais específicas. E a ordem deve ser estritamente mantida.
        
        assertTrue(keywords.isNotEmpty())
        // Gato come rato tem length 3 e freq 2 (Score bruto ~ 6.0)
        // Gato tem length 1 e freq 4 (Score bruto ~ 4.0)
        assertEquals("gato come rato", keywords[0])
    }

    @Test
    fun `extract deve lidar com strings vazias e espacos sem falhar`() {
        assertTrue(extractor.extract("").isEmpty())
        assertTrue(extractor.extract("    ").isEmpty())
        assertTrue(extractor.extract(".,?!").isEmpty())
    }

    private fun assertFalse(condition: Boolean) = assertTrue(!condition)
}
