package com.hereliesaz.GeminiOrchestrator.core.tokenizer

object Tokenizer {
    fun countTokens(text: String): Int {
        return text.split(Regex("\\s+")).size
    }
}

