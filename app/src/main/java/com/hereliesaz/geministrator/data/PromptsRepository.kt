package com.hereliesaz.geministrator.data

interface PromptsRepository {
    suspend fun getPrompts(): List<Prompt>
}
