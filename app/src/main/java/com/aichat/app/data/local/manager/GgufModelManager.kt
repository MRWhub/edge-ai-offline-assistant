package com.aichat.app.data.local.manager

import android.content.Context
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gerenciador responsável pelo armazenamento seguro dos modelos de linguagem (.gguf)
 * Ele copia ou baixa os modelos diretamente para o [Context.filesDir]/models, 
 * ocultando-os de acessos externos e protegendo-os conforme RNF04 e RNF05.
 */
class GgufModelManager(private val context: Context) {

    private val modelsDir = File(context.filesDir, "models").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    /**
     * Retorna o arquivo de modelo caso ele exista.
     */
    fun getModelFile(filename: String): File? {
        val file = File(modelsDir, filename)
        return if (file.exists()) file else null
    }

    /**
     * Lista todos os modelos .gguf armazenados localmente.
     */
    fun listLocalModels(): List<File> {
        return modelsDir.listFiles { _, name -> name.endsWith(".gguf") }?.toList() ?: emptyList()
    }

    /**
     * Copia um modelo de um InputStream (ex: selecionado pelo DocumentPicker ou via HTTP) 
     * para o armazenamento protegido de forma segura e em background.
     */
    suspend fun saveModelFromStream(
        fileName: String,
        inputStream: InputStream,
        onProgress: (Int) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        val destFile = File(modelsDir, fileName)
        
        inputStream.use { input ->
            destFile.outputStream().use { output ->
                val buffer = ByteArray(8 * 1024) // 8KB buffer
                var bytesRead: Int
                var totalRead = 0L
                val totalSize = inputStream.available().toLong().coerceAtLeast(1L) // Estimativa de tamanho se possível
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    // Progress report (simplificado, já que `available` nem sempre reflete tamanho real)
                    val progress = ((totalRead * 100) / totalSize).toInt().coerceIn(0, 100)
                    onProgress(progress)
                }
            }
        }
        destFile
    }

    /**
     * Apaga um modelo do armazenamento.
     */
    fun deleteModel(filename: String): Boolean {
        return getModelFile(filename)?.delete() ?: false
    }
}
