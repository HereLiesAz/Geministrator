package com.hereliesaz.geminiorchestrator.core.tokenizer

object Tokenizer {
    fun countTokens(text: String): Int {
        return text.split(Regex("\\s+")).size
    }
}

