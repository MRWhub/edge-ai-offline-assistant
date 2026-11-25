package com.aichat.app.domain.extractor

fun main() {
    val text = "Atenção: o João comprou maçãs, pães e água na padaria!"
    val extractor = PortugueseKeywordExtractor()
    println(extractor.extract(text, 20))
}
