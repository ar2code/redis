package ru.ar2code.android.architecture.core.prepares

import ru.ar2code.android.architecture.core.services.ServiceSavedStateHandler

class TestMemorySavedStateHandler : ServiceSavedStateHandler {

    private val dictionary = HashMap<String, Any>()

    override fun <T> get(key: String): T? {
        return dictionary.get(key) as? T
    }

    override fun <T> set(key: String, value: T?) {
        if (value != null) {
            dictionary.put(key, value)
        } else {
            dictionary.remove(key)
        }
    }

    override fun keys(): List<String> {
        return dictionary.keys.toList()
    }
}