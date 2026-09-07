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
        val constrainedProviderId = when (val constraints = request.constraints) {
            ProviderConstraints.None -> null
            is ProviderConstraints.RequireProvider -> constraints.providerId
            is ProviderConstraints.RequireCapabilities -> null
        }

        val required = buildSet {
            addAll(request.requiredCapabilities)
            val constraints = request.constraints
            if (constraints is ProviderConstraints.RequireCapabilities) {
                addAll(constraints.capabilities)
            }
        }

        val ordered = buildList {
            constrainedProviderId?.let { providersById[it] }?.let(::add)
            request.preferredProviderId
                ?.takeIf { it != constrainedProviderId }
                ?.let { providersById[it] }
                ?.let(::add)
            providersById.values.forEach { provider ->
                if (provider !in this) add(provider)
            }
        }

        for (provider in ordered) {
            if (constrainedProviderId != null && provider.id != constrainedProviderId) continue
            if (provider.capabilities().supported.containsAll(required)) return provider
        }

        val constrained = constrainedProviderId?.value?.let { " provider=$it" }.orEmpty()
        error("No agent provider satisfies required capabilities $required$constrained")
    }
}
