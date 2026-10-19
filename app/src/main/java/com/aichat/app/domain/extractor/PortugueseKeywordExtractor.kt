package com.aichat.app.domain.extractor

import java.text.Normalizer
import java.util.Locale

/**
 * Extrator determinístico de palavras-chave em Português (Heurística Lexical).
 * Utiliza regras estáticas de pontuação baseadas em Frequência (TF), Tamanho da expressão (N-Grama)
 * e Posição inicial no texto. Não possui compressão semântica (não entende o contexto do texto).
 */
class PortugueseKeywordExtractor {

    // Lista simplificada de Stopwords em português.
    private val stopWords = setOf(
        "o", "a", "os", "as", "um", "uma", "uns", "umas",
        "e", "ou", "mas", "porem", "contudo", "todavia",
        "de", "do", "da", "dos", "das", "em", "no", "na", "nos", "nas",
        "por", "para", "com", "sem", "sob", "sobre", "entre", "ate",
        "que", "qual", "quais", "quem", "como", "quando", "onde", "porque",
        "se", "seu", "sua", "seus", "suas", "meu", "minha", "nosso", "nossa",
        "eu", "tu", "ele", "ela", "nos", "vos", "eles", "elas",
        "ser", "estar", "ter", "haver", "e", "sao", "foi", "foram", "era", "eram",
        "tem", "tinha", "tinham", "ao", "aos", "pelos", "pelas", "ja", "nao", "sim"
    )

    /**
     * Extrai as principais N palavras-chave/expressões de um texto.
     * Retorna uma lista ordenada pelas expressões mais relevantes.
     * Expressões compostas são preservadas e normalizadas.
     */
    fun extract(text: String, maxKeywords: Int = 10): List<String> {
        if (text.isBlank()) return emptyList()

        val tokens = tokenizeAndClean(text)
        if (tokens.isEmpty()) return emptyList()

        // Geração de candidatos a palavras-chave (N-Gramas: 1 a 3 palavras)
        val candidates = generateNGrams(tokens, maxN = 3)

        // Calcula a pontuação determinística de cada candidato
        val scoredCandidates = scoreCandidates(candidates, tokens.size)

        return scoredCandidates
            .sortedByDescending { it.score }
            .take(maxKeywords)
            .map { it.term }
    }

    /**
     * Limpa pontuações, converte para minúsculas e remove acentos para tokenização bruta.
     * Mantém hífens para preservar palavras compostas na tokenização.
     */
    private fun tokenizeAndClean(text: String): List<String> {
        val lowerCase = text.lowercase(Locale.getDefault())
        val normalized = Normalizer.normalize(lowerCase, Normalizer.Form.NFD)
        val withoutAccents = Regex("[\\p{InCombiningDiacriticalMarks}]").replace(normalized, "")
        
        val cleanText = Regex("[^a-z0-9\\s]").replace(withoutAccents, " ")
        
        return cleanText.split(Regex("\\s+")).filter { it.isNotBlank() }
    }

    /**
     * Gera N-Gramas a partir dos tokens originais.
     * Ignora n-gramas que COMEÇAM ou TERMINAM com stop words.
     * Exemplo: "processamento de linguagem natural" é gerado agrupando os tokens.
     */
    private fun generateNGrams(tokens: List<String>, maxN: Int): List<CandidatePhrase> {
        val phrases = mutableListOf<CandidatePhrase>()

        for (n in 1..maxN) {
            for (i in 0..tokens.size - n) {
                val nGramTokens = tokens.subList(i, i + n)
                
                // Regra crítica: A expressão não pode começar nem terminar com uma StopWord.
                // Mas pode CONTER uma StopWord no meio (ex: "processamento DE linguagem").
                if (stopWords.contains(nGramTokens.first()) || stopWords.contains(nGramTokens.last())) {
                    continue
                }

                // Evitar n-gramas de tamanho 1 que sejam menores que 3 caracteres e não sejam números
                if (n == 1 && nGramTokens.first().length < 3 && !nGramTokens.first().matches(Regex("\\d+"))) {
                    continue
                }

                val phraseText = nGramTokens.joinToString(" ")
                phrases.add(CandidatePhrase(phraseText, firstAppearanceIndex = i, lengthInWords = n))
            }
        }
        return phrases
    }

    /**
     * Aplica a fórmula heurística para pontuar cada N-Grama único encontrado.
     * Critérios:
     * - Frequência de ocorrência.
     * - Tamanho do N-Grama (expressões compostas valem mais que palavras únicas se repetidas).
     * - Posição inicial (palavras que aparecem no começo do texto ganham um leve bônus).
     */
    private fun scoreCandidates(candidates: List<CandidatePhrase>, totalTokens: Int): List<ScoredCandidate> {
        // Agrupa as expressões idênticas para contar a frequência
        val frequencyMap = candidates.groupingBy { it.text }.eachCount()
        
        // Armazena a primeira vez que a expressão apareceu para o bônus de posição
        val firstAppearanceMap = candidates.groupBy { it.text }.mapValues { it.value.minOf { c -> c.firstAppearanceIndex } }
        val lengthMap = candidates.groupBy { it.text }.mapValues { it.value.first().lengthInWords }

        val scoredList = mutableListOf<ScoredCandidate>()

        for ((phrase, frequency) in frequencyMap) {
            val length = lengthMap[phrase] ?: 1
            val firstIdx = firstAppearanceMap[phrase] ?: 0

            // Fórmula Base: Frequência * (Tamanho do n-grama)
            // Bônus de Posição: Quanto mais cedo aparece no texto, ganha até 1.0 de bônus.
            val positionBonus = 1.0 - (firstIdx.toDouble() / totalTokens.toDouble())
            
            // Calculo determinístico
            val rawScore = (frequency * length) + (positionBonus * 0.5)

            scoredList.add(ScoredCandidate(phrase, rawScore))
        }

        return scoredList
    }

    private data class CandidatePhrase(val text: String, val firstAppearanceIndex: Int, val lengthInWords: Int)
    private data class ScoredCandidate(val term: String, val score: Double)
}
