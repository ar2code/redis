package ru.ar2code.android.architecture.core.android

/**
 * todo comment here about this class
 */
abstract class ChangeableState<T>(
    val state: T?,

    /**
     * True means that [state] was changed since previous time. You need to update UI with a new [state]
     * False means nothing was changed and you should ignore this state assume that UI is up-to-date with state
     */
    val isChangedSincePrevious: Boolean
) : BaseViewState() where T : Any