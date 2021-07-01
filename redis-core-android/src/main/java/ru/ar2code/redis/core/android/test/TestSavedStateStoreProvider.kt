package ru.ar2code.redis.core.android.test

import androidx.lifecycle.SavedStateHandle
import ru.ar2code.redis.core.android.SavedStateStoreProvider
import ru.ar2code.redis.core.test.TestLogger
import ru.ar2code.utils.Logger

internal class TestSavedStateStoreProvider : SavedStateStoreProvider {

    companion object {
        const val KEY_PREFIX = "TestSavedStateStoreProvider"
    }

    override val logger: Logger
        get() = TestLogger()

    override val keysPrefix: String
        get() = KEY_PREFIX

    override val loggerLevel: String
        get() = "Default"

    private val savedStateHandle = SavedStateHandle()

    override fun getSavedStateHandle(): SavedStateHandle {
        return savedStateHandle
    }
}