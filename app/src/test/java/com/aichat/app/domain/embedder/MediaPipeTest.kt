package com.aichat.app.domain.embedder

import java.io.File

fun main() {
    val modelPath = "app/src/main/assets/universal_sentence_encoder.tflite"
    val file = File(modelPath)
    if (!file.exists()) {
        println("File not found at: " + file.absolutePath)
        return
    }

    println("File exists and weighs: " + file.length() + " bytes")
}
