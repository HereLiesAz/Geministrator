package com.hereliesaz.geministrator.core.tokenizer

object Tokenizer {
    /**
     * A simple heuristic to approximate token count.
     * The official Gemini tokenizer is more complex, but a common rule of thumb
     * is that one token is approximately 4 characters for English text.
     */
    fun countTokens(text: String): Int {
        return (text.length / 4.0).toInt()
    }
}