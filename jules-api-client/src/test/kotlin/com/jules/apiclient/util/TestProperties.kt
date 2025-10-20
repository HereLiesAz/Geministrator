package com.jules.apiclient.util

import java.io.File
import java.io.FileInputStream
import java.util.Properties

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
