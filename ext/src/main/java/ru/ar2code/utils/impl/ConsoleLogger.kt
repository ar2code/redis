package ru.ar2code.utils.impl

import ru.ar2code.utils.Logger
import java.util.logging.Level

open class ConsoleLogger(tag: String) : Logger(tag) {

    open val javaLogger = java.util.logging.Logger.getLogger(tag).apply {
        level = Level.ALL
    } ?: null

    override fun info(msg: String) {
        javaLogger?.log(Level.INFO, msg)
    }

    override fun error(msg: String, t: Throwable) {
        javaLogger?.log(Level.SEVERE, msg, t)
    }

    override fun warning(msg: String) {
        javaLogger?.log(Level.WARNING, msg)
    }

}