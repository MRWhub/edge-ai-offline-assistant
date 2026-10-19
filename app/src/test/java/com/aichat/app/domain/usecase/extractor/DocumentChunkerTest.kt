package com.aichat.app.domain.usecase.extractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
class DocumentChunkerTest {
    @Test
    fun testChunking() {
        val text = "Frase um. Frase dois! Frase tres."
        val chunks = DocumentChunker.chunkText(text, maxChars = 20, overlap = 5)
        assertTrue(chunks.size > 1)
        assertEquals("Frase um.", chunks[0])
        assertEquals("e um. Frase dois!", chunks[1])
        assertEquals("dois! Frase tres.", chunks[2])
    }
}
