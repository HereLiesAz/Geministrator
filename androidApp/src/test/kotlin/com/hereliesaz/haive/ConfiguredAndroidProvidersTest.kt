package com.hereliesaz.haive

import com.hereliesaz.geministrator.domain.AgentProviderId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfiguredAndroidProvidersTest {
    @Test
    fun blankCredentialLeavesProviderRegistryEmpty() {
        assertTrue(configuredAndroidProviders("   ").isEmpty())
    }

    @Test
    fun storedCredentialConfiguresJulesProvider() {
        val providers = configuredAndroidProviders(" test-key ")

        assertEquals(1, providers.size)
        assertEquals(AgentProviderId("jules"), providers.single().id)
    }
}
