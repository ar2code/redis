/*
 * Copyright (c) 2020. Created by Alexey Rozhkov.
 */

package ru.ar2code.android.architecture.core.android.prepares

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