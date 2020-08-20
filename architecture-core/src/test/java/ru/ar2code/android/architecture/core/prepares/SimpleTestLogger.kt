package ru.ar2code.android.architecture.core.prepares

import ru.ar2code.utils.Logger

class SimpleTestLogger : Logger("TestLogger") {
    override fun info(msg: String) {

    }

    override fun error(msg: String, t: Throwable) {
        println(msg)
    }

    override fun warning(msg: String) {
        println(msg)
    }
}