package com.aichat.app.data.local.extractor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.aichat.app.domain.usecase.extractor.DocumentExtractor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfNativeExtractorImpl @Inject constructor() : DocumentExtractor {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override fun supports(mimeType: String): Boolean {
        return mimeType == "application/pdf"
    }

    override suspend fun extractText(context: Context, uri: Uri): Flow<String> = flow {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: throw IllegalArgumentException("Não foi possível acessar o documento PDF.")

            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount

            for (i in 0 until pageCount) {
                var page: PdfRenderer.Page? = null
                var bitmap: Bitmap? = null

                try {
                    page = renderer.openPage(i)

                    // Escala moderada para economizar RAM enquanto garante legibilidade para OCR
                    val densityDpi = context.resources.displayMetrics.densityDpi
                    val scale = 1.5f 
                    val width = (page.width * scale).toInt()
                    val height = (page.height * scale).toInt()

                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val image = InputImage.fromBitmap(bitmap, 0)
                    
                    // Await OCR (Roda no ML Kit background, aguardamos na coroutine)
                    val result = recognizer.process(image).await()
                    
                    val text = result.text.trim()
                    if (text.isNotBlank()) {
                        // Emite a página para ser chunkada assincronamente sem acumular todas na RAM
                        emit(text)
                    }
                } catch (e: Exception) {
                    Log.e("PdfExtractor", "Falha ao processar a página $i: ${e.message}")
                } finally {
                    page?.close()
                    bitmap?.recycle()
                }
            }
        } catch (security: SecurityException) {
            Log.e("PdfExtractor", "PDF protegido por senha ou sem permissão.", security)
            throw security
        } finally {
            renderer?.close()
            pfd?.close()
        }
    }.flowOn(Dispatchers.IO)
}
