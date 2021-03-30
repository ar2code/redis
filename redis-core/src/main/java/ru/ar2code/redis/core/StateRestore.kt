package ru.ar2code.redis.core

import ru.ar2code.utils.LoggableObject
import ru.ar2code.utils.Logger

abstract class StateRestore(
    private val expectStateName: String?,
    protected val logger: Logger
) : LoggableObject {
    abstract suspend fun restoreState(store: SavedStateStore?): RestoredStateIntent?

    fun isStateRestoreApplicable(stateName: String): Boolean {
        return isAnyState() || expectStateName == stateName
    }

    /**
     * Is universal StateRestore for any state name?
     */
    fun isAnyState(): Boolean {
        return expectStateName.isNullOrEmpty()
    }
}