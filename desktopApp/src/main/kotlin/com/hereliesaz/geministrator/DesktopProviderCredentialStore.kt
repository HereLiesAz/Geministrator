package com.hereliesaz.geministrator

import java.util.prefs.Preferences

internal class DesktopProviderCredentialStore {
    private val preferences = Preferences.userRoot().node(PREFERENCES_NODE)

    fun read(providerId: String): String? =
        preferences.get(providerId, null)
            ?.trim()
            ?.takeIf(String::isNotEmpty)

    fun readAll(): Map<String, String> = buildMap {
        ProviderCatalog.entries.forEach { entry ->
            read(entry.id)?.let { put(entry.id, it) }
        }
    }

    fun write(providerId: String, apiKey: String) {
        require(ProviderCatalog.entry(providerId) != null) { "Unknown provider $providerId" }
        val clean = apiKey.trim()
        require(clean.isNotEmpty()) { "API key is required" }
        preferences.put(providerId, clean)
        preferences.flush()
    }

    fun clear(providerId: String) {
        preferences.remove(providerId)
        preferences.flush()
    }

    private companion object {
        const val PREFERENCES_NODE = "com/hereliesaz/haive/provider-credentials"
    }
}
