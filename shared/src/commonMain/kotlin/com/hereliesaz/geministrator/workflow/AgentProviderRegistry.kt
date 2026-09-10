package com.hereliesaz.geministrator.workflow

import com.hereliesaz.geministrator.domain.AgentCapability
import com.hereliesaz.geministrator.domain.AgentProviderId
import com.hereliesaz.geministrator.domain.ProviderConstraints
import com.hereliesaz.geministrator.providers.AgentProvider

data class ProviderSelectionRequest(
    val preferredProviderId: AgentProviderId? = null,
    val requiredCapabilities: Set<AgentCapability> = emptySet(),
    val constraints: ProviderConstraints = ProviderConstraints.None,
)

class AgentProviderRegistry(
    providers: Collection<AgentProvider>,
) {
    private val providersById = providers.associateBy { it.id }

    init {
        require(providersById.size == providers.size) { "Provider IDs must be unique" }
    }

    fun provider(id: AgentProviderId): AgentProvider? = providersById[id]

    suspend fun select(request: ProviderSelectionRequest): AgentProvider {
        val required = buildSet {
            addAll(request.requiredCapabilities)
            val constraints = request.constraints
            if (constraints is ProviderConstraints.RequireCapabilities) {
                addAll(constraints.capabilities)
            }
        }

        when (val constraints = request.constraints) {
            is ProviderConstraints.RequireProvider -> {
                val provider = providersById[constraints.providerId]
                    ?: error("Required provider ${constraints.providerId.value} is not registered")
                if (!provider.capabilities().supported.containsAll(required)) {
                    error("Provider ${constraints.providerId.value} does not satisfy required capabilities $required")
                }
                return provider
            }
            else -> Unit
        }

        val ordered = buildList {
            request.preferredProviderId?.let { providersById[it] }?.let(::add)
            providersById.values.forEach { provider -> if (provider !in this) add(provider) }
        }

        for (provider in ordered) {
            if (provider.capabilities().supported.containsAll(required)) return provider
        }

        error("No agent provider satisfies required capabilities $required")
    }
}
