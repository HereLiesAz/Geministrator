package com.hereliesaz.julesapisdk

data class Session(
    val id: String,
    val title: String = "",
    val prompt: String = "",
    val source: Source? = null
)
