package com.gemini.orchestrator.core.tokenizer

object Tokenizer {
    fun countTokens(text: String): Int {
        return text.split(Regex("\\s+")).size
    }
}
