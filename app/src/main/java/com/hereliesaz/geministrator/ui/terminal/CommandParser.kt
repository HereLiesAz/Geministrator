package com.hereliesaz.geministrator.ui.terminal

data class ParsedCommand(
    val command: String,
    val args: List<String>
)

fun parseCommand(input: String): ParsedCommand {
    val parts = input.trim().split("\\s+".toRegex())
    val command = parts.firstOrNull() ?: ""
    val args = parts.drop(1)
    return ParsedCommand(command, args)
}
