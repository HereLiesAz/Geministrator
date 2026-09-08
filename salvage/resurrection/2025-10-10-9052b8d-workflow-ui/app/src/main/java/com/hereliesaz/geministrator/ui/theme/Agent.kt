package com.hereliesaz.geministrator.ui.theme

import androidx.compose.ui.graphics.Color

enum class Agent(val color: Color) {
    ORCHESTRATOR(OrchestratorColor),
    ARCHITECT(ArchitectColor),
    RESEARCHER(ResearcherColor),
    DESIGNER(DesignerColor),
    ANTAGONIST(AntagonistColor),
    TECH_SUPPORT(TechSupportColor),
    MANAGER(ManagerColor),
    UNKNOWN(Color.Gray); // Default color for unknown sources

    companion object {
        private val agentNameMap = entries.associateBy { it.name }
        fun fromString(name: String): Agent = agentNameMap[name.uppercase()] ?: UNKNOWN
    }
}