package com.aichat.app

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.aichat.app.ui.MainViewModel
import com.aichat.app.ui.screens.MainScreen
import com.aichat.app.utils.VoiceRecognizerHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var voiceRecognizer: VoiceRecognizerHelper? = null

    // File picker launcher
    private val getContent = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = "model-${System.currentTimeMillis().toString(16)}.gguf"
            contentResolver.openInputStream(it)?.let { inputStream ->
                viewModel.loadModelFromUri(fileName, inputStream)
            } ?: run {
                Toast.makeText(this, "Erro ao ler o arquivo GGUF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback {
            Log.w("MainActivity", "Ignore back press for simplicity")
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        onSendPromptToEngine = { userText, contextText ->
                            viewModel.sendPrompt(userText, contextText)
                        },
                        onStartVoiceRecognition = { onResult ->
                            startVoiceRecognition(onResult)
                        },
                        onSelectModelClick = {
                            getContent.launch(arrayOf("*/*"))
                        }
                    )
                }
            }
        }
    }

    private fun startVoiceRecognition(onResult: (String) -> Unit) {
        // Initialize if null
        if (voiceRecognizer == null) {
            voiceRecognizer = VoiceRecognizerHelper(
                context = this,
                onResult = { text ->
                    onResult(text)
                },
                onError = { errorMsg ->
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                }
            )
        }
        voiceRecognizer?.startListening()
    }

    override fun onDestroy() {
        voiceRecognizer?.destroy()
        super.onDestroy()
    }
}
