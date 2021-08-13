/*
 * Copyright (c) 2020.  The Redis Open Source Project
 * Author: Alexey Rozhkov https://github.com/ar2code
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.ar2code.redis.core.coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.*
import ru.ar2code.redis.core.*
import ru.ar2code.utils.Logger
import java.lang.Exception
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Redis service based on kotlin coroutines and works line an Actor
 * @param scope service scope. You can cancel scope to dispose service
 * @param dispatcher service dispatcher
 * @param initialState the state that the service receives after creation
 * @param reducers list of reducers used to change service` state
 * @param reducerSelector algorithm how to find reducer for pair state-intent
 * @param listenedServicesIntentSelector algorithm how to find reaction for service state changing that current service listens
 * @param stateTriggers list of triggers that can be called when service change its state
 * @param stateTriggerSelector  algorithm how to find triggers when service change state
 * @param savedStateStore state store implementation
 * @param savedStateHandler object that handle storing/restoring state
 * @param stateStoreSelector algorithm how to find store item for current state
 * @param logger logging object
 * @param serviceLogName object name that is used for logging
 * @param emitErrorAsState if true exceptions inside [StateReducer.reduceState], [StateTrigger.invokeAction], [StateRestore.restoreState], [onBeforeInitialization] will emit as [State.ErrorOccurred] state.
 */
open class RedisCoroutineStateService(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val initialState: State,
    private val reducers: List<StateReducer<*, *>>,
    private val reducerSelector: ReducerSelector,
    private val listenedServicesIntentSelector: IntentSelector,
    private val stateTriggers: List<StateTrigger<*, *>>?,
    private val stateTriggerSelector: StateTriggerSelector?,
    private val savedStateStore: SavedStateStore?,
    private val savedStateHandler: SavedStateHandler?,
    private val stateStoreSelector: StateStoreSelector?,
    protected val logger: Logger,
    private val serviceLogName: String? = null,
    private val emitErrorAsState: Boolean = false
) : RedisStateService {

    companion object {
        private const val AWAIT_INIT_DELAY_MS = 1L
    }

    /**
     * Listener for getting callback when intent is dispatched to the service.
     * When [ServiceIntentDispatcherListener.onIntentDispatched] called it means
     * that service is handling this intent. Next step is to find an appropriate reducer.
     *
     * Using for testing purposes.
     */
    internal var serviceIntentDispatcherListener: ServiceIntentDispatcherListener? = null

    private var serviceStateInternal: State = State.Created()

    override var serviceState: State
        get() {
            return serviceStateInternal.clone()
        }
        set(value) {
            serviceStateInternal = value
        }

    override val objectLogName: String?
        get() = serviceLogName ?: "${super.objectLogName}#${hashCode()}"

    private val isDisposing = AtomicBoolean(false)

    private val isServiceInitialized = AtomicBoolean(false)

    private var isServiceWasRestored = false

    private var resultsChannel = MutableSharedFlow<State>(1)

    private var intentMessagesChannel = Channel<IntentMessage>(Channel.UNLIMITED)

    private val subscribers = ConcurrentLinkedQueue<CoroutineServiceSubscriber>()

    private val listenedServicesSubscribers =
        ConcurrentHashMap<ServiceStateListener, ServiceSubscriber>()

    /**
     * You can subscribe to service's state flow with .collect method
     * To unsubscribe just cancel scope used for collecting states
     */
    val stateFlow = resultsChannel.asSharedFlow()

    init {
        initialize()
    }

    /**
     * Get count of active services that current service is listening
     */
    override fun getListenServiceCount(): Int {
        return listenedServicesSubscribers.count()
    }

    /**
     * Listening of state changing of another service.
     */
    @Synchronized
    override fun listen(serviceStateListener: ServiceStateListener) {
        val subscriber = object : ServiceSubscriber {
            override suspend fun onReceive(newState: State) {
                logger.info("[$objectLogName] receive state change for listening service: ${serviceStateListener.listeningService.objectLogName} newState=${newState.objectLogName}")

                try {
                    val intent = listenedServicesIntentSelector.findIntent(
                        serviceStateListener.stateIntentMap,
                        newState
                    )

                    if (isDisposing.get() || isDisposed()) {
                        logger.info("[$objectLogName] is disposed. Ignore listen service state changing.")
                        return
                    }

                    sendIntentMessage(intent)

                } catch (e: IntentNotFoundException) {
                    throw IntentNotFoundException(
                        "[$objectLogName] Can not find IntentMessage for listened service ${serviceStateListener.listeningService.objectLogName} for state: ${newState.objectLogName}",
                        e
                    )
                }
            }
        }

        serviceStateListener.listeningService.subscribe(subscriber)
        listenedServicesSubscribers[serviceStateListener] = subscriber
    }

    /**
     * Stop listening of service state changing
     */
    @Synchronized
    override fun stopListening(serviceStateListener: ServiceStateListener) {
        val subscriber = listenedServicesSubscribers[serviceStateListener]
        subscriber?.let {
            serviceStateListener.listeningService.unsubscribe(subscriber)
            listenedServicesSubscribers.remove(serviceStateListener)
        }
    }

    /**
     * Send intent to service for doing some action
     */
    override fun dispatch(msg: IntentMessage) {
        if (!assertScopeActive("send intent ${msg.objectLogName}"))
            return

        scope.launch(dispatcher) {
            sendIntentMessage(msg)
        }
    }

    private suspend fun sendIntentMessage(msg: IntentMessage, awaitInitialization: Boolean = true) {
        try {
            logger.info("[$objectLogName] is going to dispatch intent ${msg.objectLogName}")

            if (awaitInitialization) {
                awaitPassInitializedState()
            }
            intentMessagesChannel.send(msg)
        } catch (e: ClosedSendChannelException) {
            logger.info("[$objectLogName] intent channel is closed.")
        }
    }

    override suspend fun isServiceRestoredState(): Boolean {
        awaitFirstState()
        return isServiceWasRestored
    }

    /**
     * After disposing service can not get intents and send results.
     */
    override fun dispose() {

        fun broadcastSubscribersWithDisposedState(
            subscriber: CoroutineServiceSubscriber,
            afterBroadcastAction: () -> Unit
        ) {
            subscriber.scope.launch {
                subscriber.onReceive(State.Disposed())
                afterBroadcastAction()
            }
        }

        fun unsubscribeListeners() {
            subscribers.forEach {
                broadcastSubscribersWithDisposedState(it) {
                    unsubscribe(it)
                }
            }
            subscribers.clear()
        }

        fun unsubscribeFromListenedServices() {
            listenedServicesSubscribers.forEach {
                it.key.listeningService.unsubscribe(it.value)
            }
            listenedServicesSubscribers.clear()
        }

        fun clearSavedState() {
            val storedKeys = savedStateHandler?.getStoredKeys()
            storedKeys?.let {
                savedStateHandler?.stateStoreKeyName?.let {
                    savedStateStore?.delete(it)
                }

                savedStateStore?.delete(storedKeys)
            }
        }

        isDisposing.set(true)

        logger.info("[$objectLogName] is going to be disposed.")

        clearSavedState()
        unsubscribeListeners()
        unsubscribeFromListenedServices()

        intentMessagesChannel.close()

        serviceIntentDispatcherListener = null

        serviceState = State.Disposed()

        onDisposed()
    }

    /**
     * @return if true service can not get intents and send results.
     */
    override fun isDisposed(): Boolean {
        disposeIfScopeNotActive()
        return isDisposing.get() || serviceStateInternal is State.Disposed
    }

    /**
     * Subscribe to service's results.
     * Subscribing is alive while service is not disposed [isDisposed] and [scope] not cancelled
     */
    override fun subscribe(subscriber: ServiceSubscriber) {

        val coroutineServiceSubscriber = CoroutineServiceSubscriber(
            CoroutineScope(dispatcher + Job()),
            subscriber
        )

        fun isSubscriberExists(): Boolean {
            return subscribers.firstOrNull { it.originalSubscriber == subscriber } != null
        }

        fun addSubscriber() {
            subscribers.add(coroutineServiceSubscriber)
        }

        fun listening() {
            coroutineServiceSubscriber.scope.launch {
                resultsChannel
                    .collect {
                        coroutineServiceSubscriber.onReceive(it)
                    }
            }
        }

        if (!assertScopeActive("subscribe $coroutineServiceSubscriber to service"))
            return

        if (isSubscriberExists())
            return

        addSubscriber()

        listening()
    }

    /**
     * Stop listening service`s result by this [subscriber]
     */
    override fun unsubscribe(subscriber: ServiceSubscriber) {

        fun findSubscriber(): CoroutineServiceSubscriber? {
            return subscribers.firstOrNull { it.originalSubscriber == subscriber }
        }

        val coroutineServiceSubscriber = findSubscriber()
        coroutineServiceSubscriber?.let {
            it.scope.cancel("Cancel from service.unsubscribe method.")
            subscribers.remove(it)
        }
    }

    /**
     * @return count of active subscribers
     */
    override fun getSubscribersCount() = subscribers.size

    /**
     * Call when state changed from [old] to [new]
     */
    protected open suspend fun onStateChanged(old: State, new: State) {
        logger.info("[${this.objectLogName}] onStateChanged ${old.objectLogName} to ${new.objectLogName}")
    }

    private suspend fun storeState(new: State) {
        savedStateHandler?.let {
            val stateStore = stateStoreSelector?.findStateStore(new, it.stateStores)
            stateStore?.let { store ->
                logger.info("[$objectLogName] store state with ${store.objectLogName}")

                savedStateStore?.set(it.stateStoreKeyName, store.storedStateName)

                store.storeStateData(new, savedStateStore)
            }
        }
    }

    /**
     * Call after service state got first state after creation.
     *
     * Inside this method the service is ready and can handle intents.
     */
    protected open suspend fun onInitialized() {
        logger.info("[${this.objectLogName}] onInitialized")
    }

    /**
     * Call after service was disposed
     */
    protected open fun onDisposed() {
        logger.info("[${this.objectLogName}] onDisposed")
    }

    private suspend fun broadcastNewState(newServiceState: State) {
        try {

            if (isDisposed()) {
                logger.info("[$objectLogName] isDisposed. Can not broadcastNewState and change state to ${newServiceState.objectLogName}.")
                return
            }

            logger.info("[$objectLogName] changing state from ${serviceStateInternal.objectLogName} to ${newServiceState.objectLogName}")

            storeState(newServiceState)

            val oldState = serviceStateInternal

            serviceStateInternal = newServiceState

            resultsChannel.emit(newServiceState)

            val errorState = dispatchTriggerByState(oldState, newServiceState)

            onStateChanged(oldState, newServiceState)

            if (errorState != null) {
                broadcastNewState(errorState)
            }

        } catch (e: ClosedSendChannelException) {
            logger.info("[$objectLogName] result channel is closed.")
        }
    }

    private suspend fun dispatchTriggerByState(old: State, new: State): State.ErrorOccurred? {
        val trigger = stateTriggerSelector?.findTrigger(stateTriggers, old, new)

        logger.info("[$objectLogName] try to find trigger for changing state from ${old.objectLogName} to ${new.objectLogName}. Trigger is found = ${trigger != null}")

        return runActionCatching(null) {
            trigger?.let {

                logger.info("[$objectLogName] fire trigger ${it.objectLogName}.")

                it.invokeAction(old, new)

                val triggerIntent = it.getTriggerIntent(old, new)

                triggerIntent?.let { intent ->
                    sendIntentMessage(intent)
                }
            }
        }
    }

    /**
     * Call after service created but before service get initial state.
     *
     * Difference from [onInitialized] is that inside onCreate method you can do some actions before service receive any intent.
     *
     */
    protected open suspend fun onBeforeInitialization() {
        /**
         * You can do some initial stuff here.
         */
    }

    private fun initialize() {

        fun initService() {
            assertScopeActive("initialize service")
            subscribeToIntentMessages()
        }

        suspend fun dispatchIntentAfterInitializing() {
            lastRestoredStateIntent?.intentMessage?.let {
                sendIntentMessage(it, awaitInitialization = false)
            }
        }

        fun provideInitializedResult() {
            this.scope.launch(dispatcher) {
                val errorState = runActionCatching(null) { onBeforeInitialization() }

                broadcastNewState(getInitialState())

                errorState?.let {
                    broadcastNewState(it)
                }

                dispatchIntentAfterInitializing()

                lastRestoredStateIntent = null

                isServiceInitialized.set(true)

                onInitialized()
            }
        }

        when (serviceStateInternal) {
            is State.Created -> {
                initService()
                provideInitializedResult()
            }
            is State.Disposed -> {
                throw IllegalStateException("$objectLogName is disposed. Cannot initialize again.")
            }
            else -> {
                return
            }
        }
    }

    /**
     * Get initial state that service should get after creation. By default [initialState].
     */
    private suspend fun getInitialState(): State {
        logger.info("[$objectLogName]:getInitialState() savedStateHandler=$savedStateHandler")

        savedStateHandler?.let { handler ->
            val storedStateName = savedStateStore?.get<String>(handler.stateStoreKeyName)

            logger.info("[$objectLogName]:getInitialState() storedStateName=$storedStateName")

            storedStateName?.let {
                val stateRestore = stateStoreSelector?.findStateRestore(it, handler.stateRestores)

                logger.info("[$objectLogName]:getInitialState() stateRestore=$stateRestore")

                stateRestore?.let { restore ->
                    logger.info("[$objectLogName] restore state with ${restore.objectLogName}")
                    lastRestoredStateIntent = runRestoreCatching(restore, null)
                }
            }
        }

        isServiceWasRestored =
            lastRestoredStateIntent?.state != null || lastRestoredStateIntent?.intentMessage != null

        logger.info("[$objectLogName]:getInitialState() lastRestoredState=${lastRestoredStateIntent?.state}, lastRestoredIntent=${lastRestoredStateIntent?.intentMessage}, isServiceWasRestored=${isServiceWasRestored}")

        return lastRestoredStateIntent?.state ?: initialState
    }

    private fun subscribeToIntentMessages() {
        scope.launch(dispatcher) {
            while (isActive && !intentMessagesChannel.isClosedForReceive) {
                try {
                    val msg = intentMessagesChannel.receive()

                    serviceIntentDispatcherListener?.onIntentDispatched(msg)

                    val currentState = serviceStateInternal

                    if (isDisposed()) {
                        logger.info("[$objectLogName] currentState is disposed. Skip reducer and return.")
                        return@launch
                    }

                    val reducer = findReducer(currentState, msg)

                    logger.info("[$objectLogName] Received intent ${msg.objectLogName}. Found reducer: ${reducer.objectLogName}.")

                    val newStateFlow =
                        reducer.reduceState(currentState, msg)

                    newStateFlow?.let { stateFlow ->
                        stateFlow
                            .catch { runFlowCatching(this, it, msg) }
                            .collect {
                                broadcastNewState(it)
                            }
                    } ?: kotlin.run {
                        logger.info("[$objectLogName] reducer returned null.")
                    }
                } catch (e: ClosedReceiveChannelException) {
                    logger.info("[$objectLogName] intent channel is closed.")
                }
            }

            disposeIfScopeNotActive()
        }
    }

    private fun findReducer(
        state: State,
        intentMessage: IntentMessage
    ): StateReducer<*, *> {
        try {
            return reducerSelector.findReducer(reducers, state, intentMessage)
        } catch (e: ReducerNotFoundException) {
            throw ReducerNotFoundException("$objectLogName findReducer exception", e)
        }
    }

    private fun disposeIfScopeNotActive() {
        if (!scope.isActive) {
            logger.info("[$objectLogName] scope is not active anymore. Dispose service.")
            dispose()
        }
    }

    private fun assertScopeActive(
        errorPostfixMsgIfNotActive: String,
        throwError: Boolean = true
    ): Boolean {
        if (!scope.isActive) {
            val msg =
                "[$objectLogName] scope is not active anymore. Service is disposed and cannot [$errorPostfixMsgIfNotActive]."

            logger.info(msg)

            if (throwError) {
                throw IllegalStateException(msg)
            }

            return false
        }
        return true
    }

    private suspend fun awaitPassInitializedState() {
        while (!isServiceInitialized.get()) {
            delay(AWAIT_INIT_DELAY_MS)
            logger.info("[$objectLogName] await passing initial state. Current state = ${serviceStateInternal.objectLogName}")
        }
    }

    private var lastRestoredStateIntent: RestoredStateIntent? = null

    private fun getServiceNameForErrorState() =
        this.serviceLogName ?: this::class.simpleName ?: "unknown service name"

    private suspend fun runActionCatching(
        intentMessage: IntentMessage?,
        block: suspend () -> Unit
    ): State.ErrorOccurred? {
        return try {
            block()
            null
        } catch (e: Exception) {
            logger.info("[$objectLogName] runActionCatching exception=$e")
            if (emitErrorAsState) {
                State.ErrorOccurred(
                    getServiceNameForErrorState(),
                    e,
                    serviceStateInternal.clone(),
                    intentMessage
                )
            } else {
                throw e
            }
        }
    }

    private suspend fun runRestoreCatching(
        stateRestore: StateRestore,
        intentMessage: IntentMessage?
    ): RestoredStateIntent? {
        return try {
            stateRestore.restoreState(savedStateStore)
        } catch (e: Exception) {
            logger.info("[$objectLogName] runRestoreCatching exception=$e")
            if (emitErrorAsState) {
                RestoredStateIntent(
                    State.ErrorOccurred(
                        getServiceNameForErrorState(),
                        e,
                        serviceStateInternal.clone(),
                        intentMessage
                    ), null
                )
            } else {
                throw e
            }
        }
    }

    private suspend fun runFlowCatching(
        flowCollector: FlowCollector<State>,
        throwable: Throwable,
        intentMessage: IntentMessage?
    ) {
        if (emitErrorAsState) {
            logger.info("[$objectLogName] runFlowCatching throwable=$throwable")
            flowCollector.emit(
                State.ErrorOccurred(
                    getServiceNameForErrorState(),
                    throwable,
                    serviceStateInternal.clone(),
                    intentMessage
                )
            )
        } else {
            throw throwable
        }
    }
}