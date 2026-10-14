package com.example.llama.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.llama.ui.ChatMessage
import com.example.llama.ui.MainViewModel
import com.example.llama.ui.Screen

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onSendPromptToEngine: (String, String) -> Unit,
    onStartVoiceRecognition: ((String) -> Unit) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState.currentScreen) {
        Screen.ONBOARDING -> {
            OnboardingScreen(
                routineText = uiState.userRoutineContext,
                onRoutineTextChange = { viewModel.updateRoutineContext(it) },
                onSaveAndStart = { viewModel.completeOnboarding() },
                onMicClick = {
                    onStartVoiceRecognition { recognizedText ->
                        viewModel.updateRoutineContext(uiState.userRoutineContext + " " + recognizedText)
                    }
                }
            )
        }
        Screen.CHAT -> {
            ChatScreen(
                messages = uiState.messages,
                isDebugModeEnabled = uiState.isDebugModeEnabled,
                metrics = uiState.metrics,
                isGenerating = uiState.isGenerating,
                onToggleDebugMode = { viewModel.toggleDebugMode() },
                onSendMessage = { userText ->
                    // Append user message
                    val userMsg = ChatMessage(
                        id = System.currentTimeMillis().toString(),
                        text = userText,
                        isUser = true
                    )
                    viewModel.appendMessage(userMsg)

                    // Append empty assistant message placeholder
                    val assistantMsg = ChatMessage(
                        id = (System.currentTimeMillis() + 1).toString(),
                        text = "",
                        isUser = false
                    )
                    viewModel.appendMessage(assistantMsg)

                    // Send to llama engine
                    onSendPromptToEngine(userText, uiState.userRoutineContext)
                },
                onMicClick = { onResult ->
                    onStartVoiceRecognition(onResult)
                }
            )
        }
    }
}
