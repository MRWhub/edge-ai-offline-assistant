package com.example.llama.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class VoiceRecognizerHelper(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(this)
        } else {
            Log.e("VoiceRecognizer", "Speech recognition not available on this device.")
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            onError("Reconhecimento de voz não suportado neste dispositivo.")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR") // Adjust based on user locale if needed
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            onError("Erro ao iniciar reconhecimento de voz: ${e.message}")
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun destroy() {
        speechRecognizer?.destroy()
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Erro de áudio"
            SpeechRecognizer.ERROR_CLIENT -> "Erro no cliente"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissão insuficiente"
            SpeechRecognizer.ERROR_NETWORK -> "Erro de rede"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout de rede"
            SpeechRecognizer.ERROR_NO_MATCH -> "Nenhuma correspondência encontrada"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconhecedor ocupado"
            SpeechRecognizer.ERROR_SERVER -> "Erro no servidor"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nenhuma fala detectada"
            else -> "Erro desconhecido"
        }
        onError(errorMessage)
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            onResult(matches[0])
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            onResult(matches[0])
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
