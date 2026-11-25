package com.aichat.app.domain.extractor
import java.text.Normalizer
import java.util.Locale

fun main() {
    val text = "Atenção: o João comprou maçãs, pães e água na padaria!"
    val lowerCase = text.lowercase(Locale.getDefault())
    val normalized = Normalizer.normalize(lowerCase, Normalizer.Form.NFD)
    val withoutAccents = Regex("\\p{InCombiningDiacriticalMarks}+").replace(normalized, "")
    val cleanText = Regex("[^a-z\\s]").replace(withoutAccents, " ")
    val tokens = cleanText.split(Regex("\\s+")).filter { it.isNotBlank() }
    println(tokens)
}
