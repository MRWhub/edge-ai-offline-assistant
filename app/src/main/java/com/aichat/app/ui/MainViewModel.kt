package com.aichat.app.ui

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.UserPreferencesRepository
import com.aichat.app.data.local.manager.GgufModelManager
import com.aichat.app.data.repository.KnowledgeRepository
import com.aichat.app.domain.usecase.RagPromptFormatter
import com.arm.aichat.InferenceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class PerformanceMetrics(
    val tokensPerSecond: Float = 0f,
    val latencyMs: Long = 0,
    val ramMb: Int = 0
)

enum class Screen {
    ONBOARDING,
    CHAT
}

data class UiState(
    val currentScreen: Screen = Screen.ONBOARDING,
    val userRoutineContext: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val isDebugModeEnabled: Boolean = false,
    val metrics: PerformanceMetrics = PerformanceMetrics(),
    val isModelReady: Boolean = false,
    val systemMessage: String = ""
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: UserPreferencesRepository,
    private val knowledgeRepository: KnowledgeRepository,
    private val engine: InferenceEngine,
    private val modelManager: GgufModelManager,
    private val ragFormatter: RagPromptFormatter,
    private val pdfExtractor: com.aichat.app.domain.usecase.extractor.DocumentExtractor
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null
    private val lastAssistantMsg = StringBuilder()

    init {
        // Load initial state from DataStore
        viewModelScope.launch {
            val isCompleted = repository.onboardingCompleted.first()
            val routineText = repository.routineContext.first()

            val initialMessages = if (isCompleted) {
                listOf(
                    ChatMessage(
                        id = "welcome",
                        text = "Olá! Já recuperei suas preferências. Selecione um modelo GGUF no botão superior para começarmos a conversar.",
                        isUser = false
                    )
                )
            } else {
                emptyList()
            }

            _uiState.update {
                it.copy(
                    currentScreen = if (isCompleted) Screen.CHAT else Screen.ONBOARDING,
                    userRoutineContext = routineText,
                    messages = initialMessages
                )
            }
            
            // Auto-load last selected model (RNF07)
            if (isCompleted) {
                val lastModel = repository.lastModelName.first()
                if (!lastModel.isNullOrEmpty()) {
                    val modelFile = modelManager.getModelFile(lastModel)
                    if (modelFile != null && modelFile.exists()) {
                        try {
                            _uiState.update { it.copy(systemMessage = "Restaurando último modelo...", isGenerating = true) }
                            withContext(Dispatchers.IO) {
                                engine.loadModel(modelFile.absolutePath)
                                val systemPrompt = "Você é um assistente de IA focado em privacidade chamado AiChat. Você está executando localmente no celular do usuário. Responda de forma empática e segura. Contexto do usuário: $routineText"
                                engine.setSystemPrompt(systemPrompt)
                            }
                            _uiState.update { it.copy(isModelReady = true, isGenerating = false, systemMessage = "Modelo restaurado!") }
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Failed to auto-load model", e)
                            _uiState.update { it.copy(isGenerating = false, systemMessage = "Falha ao restaurar modelo anterior.") }
                        }
                    }
                }
            }
        }
        
        // Listen to engine state
        viewModelScope.launch {
            engine.state.collect { engineState ->
                when (engineState) {
                    is InferenceEngine.State.ModelReady -> {
                        _uiState.update { it.copy(isModelReady = true, isGenerating = false, systemMessage = "Modelo carregado!") }
                    }
                    is InferenceEngine.State.Generating -> {
                        _uiState.update { it.copy(isGenerating = true, systemMessage = "Gerando resposta...") }
                    }
                    is InferenceEngine.State.Error -> {
                        _uiState.update { it.copy(isGenerating = false, systemMessage = "Erro: ${engineState.exception.message ?: "Desconhecido"}") }
                    }
                    is InferenceEngine.State.LoadingModel -> {
                         _uiState.update { it.copy(isGenerating = true, isModelReady = false, systemMessage = "Carregando pesos...") }
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Called when user selects a GGUF file from the URI picker.
     * Copies to safe storage and loads into InferenceEngine.
     */
    fun loadModelFromUri(fileName: String, inputStream: InputStream) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(systemMessage = "Copiando modelo para armazenamento seguro...") }
                
                // Copy the file to the app's internal filesDir
                val modelFile = modelManager.saveModelFromStream(fileName, inputStream)
                repository.saveLastModelName(fileName)
                
                _uiState.update { it.copy(systemMessage = "Carregando pesos nativamente...") }
                // Load into Llama.cpp
                engine.loadModel(modelFile.absolutePath)
                
                _uiState.update { it.copy(systemMessage = "Injetando Prompt de Sistema...") }
                // Injetar o System Prompt inicial (RF03 / RN05)
                val systemPrompt = "Você é um assistente de IA focado em privacidade chamado AiChat. Você está executando localmente no celular do usuário. Responda de forma empática e segura. Contexto do usuário: ${_uiState.value.userRoutineContext}"
                engine.setSystemPrompt(systemPrompt)

                _uiState.update { it.copy(isModelReady = true, systemMessage = "Modelo carregado com sucesso!") }
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading model", e)
                _uiState.update { it.copy(systemMessage = "Falha ao carregar: ${e.message}") }
            }
        }
    }

    /**
     * Called when user selects a PDF document to attach to the RAG knowledge base.
     */
    fun importPdfDocument(uri: Uri, fileName: String, context: android.content.Context) {
        viewModelScope.launch {
            try {
                if (!pdfExtractor.supports("application/pdf")) {
                    _uiState.update { it.copy(systemMessage = "Erro: Tipo de arquivo não suportado nativamente.") }
                    return@launch
                }
                
                _uiState.update { it.copy(isGenerating = true, systemMessage = "Iniciando processamento nativo do PDF...") }
                
                // OCR e Parsing do Pdf (Isolado no IO dispatcher dentro da lib)
                var totalChunksIngested = 0
                var currentPage = 1
                
                pdfExtractor.extractText(context, uri).collect { pageText ->
                    // UI progress
                    _uiState.update { it.copy(systemMessage = "OCR Concluído na Página $currentPage. Ingerindo Conhecimento...") }
                    
                    // Chunk text gracefully
                    val chunks = com.aichat.app.domain.usecase.extractor.DocumentChunker.chunkText(pageText)
                    
                    // Ingest each chunk
                    for ((index, chunk) in chunks.withIndex()) {
                        knowledgeRepository.ingestChunk(
                            text = chunk,
                            sourceName = "$fileName - Pág. $currentPage",
                            estimatedTokens = chunk.length / 4
                        )
                        totalChunksIngested++
                    }
                    currentPage++
                }
                
                _uiState.update { 
                    it.copy(
                        isGenerating = false, 
                        systemMessage = "Documento importado! ($totalChunksIngested blocos de memória gerados)"
                    )
                }

            } catch (e: SecurityException) {
                Log.e("MainViewModel", "Security error importing PDF", e)
                _uiState.update { it.copy(isGenerating = false, systemMessage = "Documento protegido por senha ou sem permissões.") }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error importing PDF", e)
                _uiState.update { it.copy(isGenerating = false, systemMessage = "Falha ao importar o arquivo: ${e.message}") }
            }
        }
    }

    fun sendPrompt(userText: String, contextText: String) {
        if (userText.isBlank()) return

        // Save user message to state
        val userMsg = ChatMessage(id = System.currentTimeMillis().toString(), text = userText, isUser = true)
        val assistantMsgId = (System.currentTimeMillis() + 1).toString()
        val assistantMsg = ChatMessage(id = assistantMsgId, text = "", isUser = false)
        
        _uiState.update { state ->
            state.copy(messages = state.messages + userMsg + assistantMsg)
        }

        lastAssistantMsg.clear()

        // Send to Engine
        generationJob = viewModelScope.launch {
            try {
                // RAG Step: Search context from local DB first
                val relevantChunks = withContext(Dispatchers.IO) {
                    try {
                        knowledgeRepository.searchRelevantContext(userText, maxResults = 3)
                    } catch (e: Exception) {
                        // MediaPipe falhará intencionalmente se o universal_sentence_encoder.tflite não estiver em Assets.
                        // Nesse caso (fallback), tratamos o erro e devolvemos lista vazia. A conversa seguirá normalmente.
                        emptyList()
                    }
                }
                
                // Format the prompt with potential RAG context
                val finalPrompt = ragFormatter.formatPromptWithContext(userText, relevantChunks)

                // Envia para o motor C++ (Que aplicará o ChatML na JNI)
                engine.sendUserPrompt(finalPrompt)
                    .onCompletion {
                        // Done generating
                    }
                    .collect { token ->
                        // Append token and update UI
                        lastAssistantMsg.append(token)
                        updateLastAssistantMessage(lastAssistantMsg.toString())
                    }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error generating text", e)
                lastAssistantMsg.append("\n[Erro de inferência: ${e.message}]")
                updateLastAssistantMessage(lastAssistantMsg.toString())
            }
        }
    }

    fun updateRoutineContext(text: String) {
        _uiState.update { it.copy(userRoutineContext = text) }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.saveRoutineContext(_uiState.value.userRoutineContext)
            repository.completeOnboarding()

            _uiState.update { currentState ->
                val systemMsg = ChatMessage(
                    id = "welcome",
                    text = "Olá! Já entendi um pouco sobre sua rotina. Selecione um modelo GGUF usando a engrenagem no topo para conversar.",
                    isUser = false
                )
                currentState.copy(
                    currentScreen = Screen.CHAT,
                    messages = if (currentState.messages.isEmpty()) listOf(systemMsg) else currentState.messages
                )
            }
        }
    }

    fun toggleDebugMode() {
        _uiState.update { it.copy(isDebugModeEnabled = !it.isDebugModeEnabled) }
    }

    fun appendMessage(message: ChatMessage) {
        _uiState.update { state ->
            state.copy(messages = state.messages + message)
        }
    }

    private fun updateLastAssistantMessage(newContent: String) {
        _uiState.update { state ->
            val lastIndex = state.messages.indexOfLast { !it.isUser }
            if (lastIndex != -1) {
                val updatedList = state.messages.toMutableList()
                updatedList[lastIndex] = updatedList[lastIndex].copy(text = newContent)
                state.copy(messages = updatedList)
            } else state
        }
    }

    override fun onCleared() {
        generationJob?.cancel()
        // DO NOT call engine.destroy() here yet because the Engine is a Singleton shared by the whole app.
        // It lives as long as the application lives.
        super.onCleared()
    }
}
