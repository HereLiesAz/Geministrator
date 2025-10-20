package com.jules.apiclient.util

import java.io.File
import java.io.FileInputStream
import java.util.Properties

/**
 * Loads properties from a `local.properties` file in the project root.
 * This utility is intended for use in pure Kotlin modules (like `:jules-api-client`)
 * that do not have access to the Android `BuildConfig` class.
 */
object TestProperties {

    private val properties = Properties()

    init {
        val localPropertiesFile = File("../local.properties").let {
            if (it.exists()) it else File("local.properties")
        }

        if (localPropertiesFile.exists()) {
            properties.load(FileInputStream(localPropertiesFile))
        } else {
            println("Warning: local.properties file not found. Integration tests may be skipped.")
        }
    }

    fun getProperty(name: String): String? {
        return properties.getProperty(name)
    }
}
