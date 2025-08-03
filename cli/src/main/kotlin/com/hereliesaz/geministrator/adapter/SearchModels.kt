package com.hereliesaz.geministrator.adapter

import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val items: List<SearchItem> = emptyList(),
)

@Serializable
data class SearchItem(
    val title: String? = null,
    val link: String? = null,
    val snippet: String? = null,
)