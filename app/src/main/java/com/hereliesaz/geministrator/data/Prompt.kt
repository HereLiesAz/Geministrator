package com.hereliesaz.geministrator.data

import kotlinx.serialization.Serializable

@Serializable
data class Prompt(
    val name: String,
    val prompt: String
)
