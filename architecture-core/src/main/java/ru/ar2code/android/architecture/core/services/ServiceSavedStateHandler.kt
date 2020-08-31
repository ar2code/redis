package ru.ar2code.android.architecture.core.services

interface ServiceSavedStateHandler {

    fun <T> get(key: String): T?

    fun <T> set(key: String, value: T?)

    fun keys(): List<String>
}