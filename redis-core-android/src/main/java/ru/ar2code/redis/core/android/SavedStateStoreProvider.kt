package ru.ar2code.redis.core.android

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import ru.ar2code.redis.core.SavedStateStore
import ru.ar2code.redis.core.android.ext.toRedisSavedStateStore
import ru.ar2code.utils.Logger

/**
 * Provider of [SavedStateStore]
 *
 * All your services can use this provider to get access to a common SavedStateStore object.
 *
 * This provider can copy string values to/from android Bundle.
 * You can use activity's savedInstanceState bundle to keep stored data in a process death purposes.
 *
 * The prefix [keysPrefix] is used to distinguish between application keys.
 * ScopeStateProvider only stores String values.
 */
interface SavedStateStoreProvider {

    val logger: Logger

    val keysPrefix: String

    val loggerLevel: String

    fun getSavedStateHandle(): SavedStateHandle

    fun getSavedStateStore(): SavedStateStore {
        return getSavedStateHandle().toRedisSavedStateStore()
    }

    fun copyStateProviderToBundle(bundle: Bundle) {
        val savedState = getSavedStateHandle()

        savedState.keys().forEach { key ->
            val v = savedState.get<String>(key)

            val outKey = if (key.startsWith(keysPrefix)) {
                key
            } else {
                "${keysPrefix}$key"
            }

            bundle.putString(outKey, v)

            logger.info(
                "copy key-value from state provider to outState: $key -> $v",
                loggerLevel
            )
        }
    }

    fun copyBundleToStateProvider(bundle: Bundle?) {
        val savedState = getSavedStateHandle()

        bundle?.keySet()?.forEach { key ->

            if (key.startsWith(keysPrefix)) {

                val value = bundle.getString(key)

                val inKey = key.replaceFirst(keysPrefix, "")

                savedState.set(inKey, value)

                logger.info(
                    "copy key-value from savedInstanceState bundle to state provider: $inKey -> $value",
                    loggerLevel
                )
            }
        }
    }
}