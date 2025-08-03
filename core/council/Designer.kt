package com.hereliesaz.geministrator_plugin.core.council

import com.hereliesaz.geministrator_plugin.common.AbstractCommand
import com.hereliesaz.geministrator_plugin.common.ExecutionAdapter

class Designer(private val logger: ILogger, private val adapter: ExecutionAdapter) {
    fun createSpecification(feature: String): List<AbstractCommand> {
        logger.log("Designer: Creating feature specification for '$feature'.")
        val sanitizedFeature = feature.replace(" ", "_").replace(Regex("[^A-Za-z0-9_]"), "")
        return listOf(AbstractCommand.WriteFile(
            path = "docs/specs/$sanitizedFeature.md",
            content = "# Feature: $feature\n\nThis feature should allow users to..."
        ))
    }
    fun updateChangelog(commitMessage: String) {
        logger.log("Designer: Updating changelog.")
        adapter.execute(AbstractCommand.AppendToFile("CHANGELOG.md", "\n- $commitMessage"))
    }
    fun recordHistoricalLesson(lesson: String) {
        logger.log("Designer: Recording important lesson in project history.")
        adapter.execute(AbstractCommand.AppendToFile("docs/history.md", "\n- $lesson"))
    }
}