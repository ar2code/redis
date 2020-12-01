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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import ru.ar2code.redis.core.*
import ru.ar2code.utils.Logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Redis service based on kotlin coroutines and works line an Actor.
 * @param scope service scope. You can cancel scope to dispose service.
 * @param dispatcher service dispatcher
 * @param initialState the state that the service receives after creation
 * @param reducers list of reducers used to change service` state
 * @param reducerSelector algorithm how to find reducer for pair state-intent
 * @param listenedServicesIntentSelector algorithm how to find reaction for service state changing that current service listens
 * @param stateTriggers list of triggers that can be called when service change its state
 * @param stateTriggerSelector  algorithm how to find triggers when service change state
 * @param logger logging object
 */
open class RedisCoroutineStateService(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val initialState: State,
    private val reducers: List<StateReducer>,
    private val reducerSelector: ReducerSelector,
    private val listenedServicesIntentSelector: IntentSelector,
    private val stateTriggers: List<StateTrigger>?,
    private val stateTriggerSelector: StateTriggerSelector?,
    protected val logger: Logger,
    private val serviceLogName: String? = null
) : RedisStateService {

    companion object {
        private const val AWAIT_INIT_DELAY_MS = 1L
    }

    private var serviceStateInternal: State = State.Created()

    override var serviceState: State
        get() {
            return serviceStateInternal.clone()
        }
        set(value) {
            serviceStateInternal = value
        }

    override fun objectLogName(): String? {
        return serviceLogName ?: super.objectLogName()
    }

    private val isDisposing = AtomicBoolean(false)

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
     * Listening of state changing of another service.
     */
    @Synchronized
    override fun listen(serviceStateListener: ServiceStateListener) {
        val subscriber = object : ServiceSubscriber {
            override suspend fun onReceive(newState: State) {
                logger.info("${objectLogName()} receive state change for listening service: ${serviceStateListener.listeningService.objectLogName()} newState=${newState.objectLogName()}")

                try {
                    val intent = listenedServicesIntentSelector.findIntent(
                        serviceStateListener.stateIntentMap,
                        newState
                    )
                    sendIntentMessage(intent)

                } catch (e: IntentNotFoundException) {
                    throw IntentNotFoundException(
                        "[${objectLogName()}] Can not find IntentMessage for listened service ${serviceStateListener.listeningService.objectLogName()} for state: ${newState.objectLogName()}",
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
        if (!assertScopeActive("send intent $msg"))
            return

        scope.launch(dispatcher) {
            sendIntentMessage(msg)
        }
    }

    private suspend fun sendIntentMessage(msg: IntentMessage) {
        try {
            awaitPassCreatedState()
            intentMessagesChannel.send(msg)
        } catch (e: ClosedSendChannelException) {
            logger.info("Service [${objectLogName()}] intent channel is closed.")
        }
    }

    /**
     * After disposing service can not get intents and send results.
     */
    override fun dispose() {

        fun unsubscribeListeners() {
            subscribers.forEach {
                unsubscribe(it)
            }
            subscribers.clear()
        }

        fun unsubscribeFromListenedServices() {
            listenedServicesSubscribers.forEach {
                it.key.listeningService.unsubscribe(it.value)
            }
            listenedServicesSubscribers.clear()
        }

        isDisposing.set(true)

        logger.info("Service ${objectLogName()} is going to be disposed.")

        unsubscribeListeners()
        unsubscribeFromListenedServices()

        serviceState = State.Disposed()

        intentMessagesChannel.close()

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
        logger.info("${this.objectLogName()} onStateChanged $old to $new")
    }

    /**
     * Call after service state got first state after [State.Created]
     */
    protected open suspend fun onInitialized() {
        logger.info("${this.objectLogName()} onInitialized")
    }

    /**
     * Call after service completely disposed
     */
    protected open fun onDisposed() {
        logger.info("${this.objectLogName()} onDisposed")
    }

    internal suspend fun broadcastNewState(newServiceState: State) {
        try {

            if (isDisposed()) {
                logger.info("[${objectLogName()}] isDisposed. Can not broadcastNewState and change state to ${newServiceState.objectLogName()}.")
                return
            }

            logger.info("[${objectLogName()}] change state from ${serviceStateInternal.objectLogName()} to ${newServiceState.objectLogName()}")

            val oldState = serviceStateInternal

            serviceState = newServiceState

            resultsChannel.emit(newServiceState)

            dispatchTriggerByState(oldState, newServiceState)

            onStateChanged(oldState, newServiceState)

        } catch (e: ClosedSendChannelException) {
            logger.info("[${objectLogName()}] result channel is closed.")
        }
    }

    private suspend fun dispatchTriggerByState(old: State, new: State) {
        val trigger = stateTriggerSelector?.findTrigger(stateTriggers, old, new)

        logger.info("[${objectLogName()}] try to find trigger for changing state from ${old.objectLogName()} to ${new.objectLogName()}. Trigger is found = ${trigger != null}")

        trigger?.let {

            logger.info("[${objectLogName()}] fire trigger ${it.objectLogName()}.")

            it.invokeAction(old, new)

            val triggerIntent = it.getTriggerIntent(old, new)

            triggerIntent?.let { intent ->
                dispatch(intent)
            }
        }
    }

    private fun initialize() {

        fun initService() {
            assertScopeActive("initialize service")
            subscribeToIntentMessages()
        }

        fun provideInitializedResult() {
            this.scope.launch(dispatcher) {
                logger.info("[${objectLogName()}] on initialized.")
                broadcastNewState(getInitialState())
                onInitialized()
            }
        }

        when (serviceStateInternal) {
            is State.Created -> {
                initService()
                provideInitializedResult()
            }
            is State.Disposed -> {
                throw IllegalStateException("${objectLogName()} is disposed. Cannot initialize again.")
            }
            else -> {
                return
            }
        }
    }

    /**
     * Get initial state that service should get after creation. By default [initialState]
     */
    protected open suspend fun getInitialState(): State {
        return initialState
    }

    private fun subscribeToIntentMessages() {
        scope.launch(dispatcher) {
            while (isActive && !intentMessagesChannel.isClosedForReceive) {
                try {
                    val msg = intentMessagesChannel.receive()

                    val reducer = findReducer(msg)

                    logger.info("[${objectLogName()}] Received intent ${msg.objectLogName()}. Found reducer: ${reducer.objectLogName()}.")

                    val newStateFlow =
                        reducer.reduce(serviceStateInternal, msg)

                    newStateFlow?.let { stateFlow ->
                        stateFlow.collect {
                            broadcastNewState(it)
                        }
                    }
                } catch (e: ClosedReceiveChannelException) {
                    logger.info("[${objectLogName()}] intent channel is closed.")
                }
            }

            disposeIfScopeNotActive()
        }
    }

    private fun findReducer(
        intentMessage: IntentMessage
    ): StateReducer {
        try {
            return reducerSelector.findReducer(reducers, serviceStateInternal, intentMessage)
        } catch (e: ReducerNotFoundException) {
            throw ReducerNotFoundException("${objectLogName()} findReducer exception", e)
        }
    }

    private fun disposeIfScopeNotActive() {
        if (!scope.isActive) {
            logger.info("[${objectLogName()}] scope is not active anymore. Dispose service.")
            dispose()
        }
    }

    private fun assertScopeActive(
        errorPostfixMsgIfNotActive: String,
        throwError: Boolean = true
    ): Boolean {
        if (!scope.isActive) {
            val msg =
                "[${objectLogName()}] scope is not active anymore. Service is disposed and cannot [$errorPostfixMsgIfNotActive]."

            logger.info(msg)

            if (throwError) {
                throw IllegalStateException(msg)
            }

            return false
        }
        return true
    }

    private suspend fun awaitPassCreatedState() {
        while (serviceStateInternal is State.Created) {
            delay(AWAIT_INIT_DELAY_MS)
            logger.info("[${objectLogName()}] await passing initial state. Current state = ${serviceStateInternal.objectLogName()}")
        }
    }
}