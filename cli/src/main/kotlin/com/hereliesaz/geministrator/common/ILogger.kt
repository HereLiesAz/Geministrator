package com.hereliesaz.geministrator.common

interface ILogger {
    fun info(message: String)
    fun error(message: String, e: Throwable? = null)
    fun interactive(message: String)
    fun prompt(message: String): String?
}