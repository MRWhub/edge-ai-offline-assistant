package com.aichat.app.data.local.embedder

import android.content.Context
import com.aichat.app.domain.embedder.TextEmbedder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder as MPTextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementação do [TextEmbedder] usando o MediaPipe Tasks API.
 * Executa inferência em CPU paralelamente ao LLM, sem colidir com os ponteiros globais do C++.
 */
class MediaPipeEmbedderImpl(
    private val context: Context,
    private val modelFileName: String = "universal_sentence_encoder.tflite"
) : TextEmbedder {

    override val modelId: String = "mediapipe_use_512"
    override val dimensions: Int = 512

    private var embedder: MPTextEmbedder? = null

    init {
        // Inicialização *lazy* apenas quando necessário no lifecycle da aplicação
        val baseOptions = BaseOptions.builder().setModelAssetPath(modelFileName).build()
        val options = TextEmbedderOptions.builder().setBaseOptions(baseOptions).build()
        
        try {
            embedder = MPTextEmbedder.createFromOptions(context, options)
        } catch (e: Exception) {
            // Em cenários de teste ou quando o arquivo TFLite ainda não foi baixado, 
            // a inicialização irá falhar. Será devidamente capturada na chamada `embed()`.
        }
    }

    override suspend fun embed(text: String): FloatArray = withContext(Dispatchers.Default) {
        val currentEmbedder = embedder ?: throw IllegalStateException("Embedder modelo ($modelFileName) não carregado ou ausente nos assets.")
        
        val result = currentEmbedder.embed(text)
        val embedding = result.embeddingResult().embeddings().firstOrNull()
            ?: throw IllegalStateException("O modelo de embedding não retornou vetores.")

        embedding.floatEmbedding()
    }

    override fun close() {
        embedder?.close()
        embedder = null
    }
}
