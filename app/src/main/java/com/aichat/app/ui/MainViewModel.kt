package com.aichat.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val metrics: PerformanceMetrics = PerformanceMetrics()
)

class MainViewModel(private val repository: UserPreferencesRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Load initial state from DataStore
        viewModelScope.launch {
            val isCompleted = repository.onboardingCompleted.first()
            val routineText = repository.routineContext.first()

            val initialMessages = if (isCompleted) {
                listOf(
                    ChatMessage(
                        id = "welcome",
                        text = "Olá! Já recuperei suas preferências. Como posso te ajudar hoje?",
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
                    text = "Olá! Já entendi um pouco sobre sua rotina. Como posso te ajudar hoje?",
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

    fun updateLastAssistantMessage(newContent: String) {
        _uiState.update { state ->
            val lastIndex = state.messages.indexOfLast { !it.isUser }
            if (lastIndex != -1) {
                val updatedList = state.messages.toMutableList()
                updatedList[lastIndex] = updatedList[lastIndex].copy(text = newContent)
                state.copy(messages = updatedList)
            } else state
        }
    }

    fun setGenerating(generating: Boolean) {
        _uiState.update { it.copy(isGenerating = generating) }
    }

    fun updateMetrics(tps: Float, latency: Long, ram: Int) {
        _uiState.update { it.copy(metrics = PerformanceMetrics(tps, latency, ram)) }
    }

    class Factory(private val repository: UserPreferencesRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
