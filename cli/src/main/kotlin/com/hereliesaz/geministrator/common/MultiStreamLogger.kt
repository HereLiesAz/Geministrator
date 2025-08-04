package com.hereliesaz.geministrator.common

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date

class MultiStreamLogger(configDir: File) : ILogger {
    private val logFile: File

    init {
        configDir.mkdirs()
        logFile = File(configDir, "geministrator.log")
    }

    private fun writeToFile(level: String, message: String, e: Throwable? = null) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())
        PrintWriter(FileWriter(logFile, true)).use { writer ->
            writer.println("[$timestamp] [$level] $message")
            e?.let { ex -> ex.printStackTrace(writer) }
        }
    }

    override fun info(message: String) {
        writeToFile("INFO", message)
    }

    override fun error(message: String, e: Throwable?) {
        val fullMessage = if (e != null) "$message: ${e.message}" else message
        System.err.println("[ERROR] $fullMessage")
        writeToFile("ERROR", message, e)
    }

    override fun interactive(message: String) {
        println(message)
        writeToFile("INTERACTIVE", message)
    }

    override fun prompt(message: String): String? {
        print(message)
        writeToFile("PROMPT", message)
        val response = readlnOrNull()
        writeToFile("RESPONSE", response ?: "<empty>")
        return response
    }
}