package com.jules.pythonexecutor

import com.chaquo.python.Python

class PythonExecutor {

    fun executeGreet(name: String): String {
        val python = Python.getInstance()
        val helloModule = python.getModule("hello")
        val result = helloModule.callAttr("greet", name)
        return result.toString()
    }
}