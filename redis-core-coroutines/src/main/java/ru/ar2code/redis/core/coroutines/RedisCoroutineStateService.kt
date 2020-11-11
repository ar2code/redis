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
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import ru.ar2code.redis.core.*
import ru.ar2code.utils.Logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

@ExperimentalCoroutinesApi
open class RedisCoroutineStateService(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val initialState: State,
    private val reducers: List<StateReducer>,
    private val reducerSelector: ReducerSelector,
    private val stateTriggers: List<StateTrigger>?,
    private val stateTriggerSelector: StateTriggerSelector?,
    protected val logger: Logger
) : RedisStateService {

    companion object {
        private const val AWAIT_INIT_DELAY_MS = 1L
    }

    override var serviceState: State = State.Created()
        get() {
            return field.clone()
        }

    private val isDisposing = AtomicBoolean(false)

    private var resultsChannel = MutableSharedFlow<State>(1, 64, BufferOverflow.DROP_OLDEST)

    private var intentMessagesChannel = Channel<IntentMessage>(Channel.UNLIMITED)

    private val subscribers = ConcurrentLinkedQueue<CoroutineServiceSubscriber>()

    private val listenedServicesSubscribers =
        ConcurrentHashMap<ListenedService, CoroutineServiceSubscriber>()

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
    override fun listen(listenedService: ListenedService) {
        val listeningScope = CoroutineScope(dispatcher + Job())

        val subscriber = CoroutineServiceSubscriber(listeningScope,
            object : ServiceSubscriber {
                override fun onReceive(newState: State) {
                    dispatch(listenedService.intentBuilder(newState))
                }
            })

        listenedService.serviceRedis.subscribe(subscriber)
        listenedServicesSubscribers[listenedService] = subscriber
    }

    /**
     * Stop listening of service state changing
     */
    @Synchronized
    override fun stopListening(listenedService: ListenedService) {
        val subscriber = listenedServicesSubscribers[listenedService]
        subscriber?.let {
            listenedService.serviceRedis.unsubscribe(subscriber)
            listenedServicesSubscribers.remove(listenedService)
        }
    }

    /**
     * Send intent to service for doing some action
     */
    override fun dispatch(msg: IntentMessage) {
        if (!assertScopeActive("send intent $msg"))
            return

        scope.launch(dispatcher) {
            try {
                awaitPassCreatedState()
                intentMessagesChannel.send(msg)
            } catch (e: ClosedSendChannelException) {
                logger.info("Service [${this@RedisCoroutineStateService}] intent channel is closed.")
            }
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
                it.key.serviceRedis.unsubscribe(it.value)
            }
            listenedServicesSubscribers.clear()
        }

        isDisposing.set(true)

        logger.info("Service $this is going to be disposed.")

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
        return isDisposing.get() || serviceState is State.Disposed
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
            coroutineServiceSubscriber.scope.launch(dispatcher) {
                logger.info("Service [${this@RedisCoroutineStateService}] start listening new subscription")

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

    }

    /**
     * Call after service state got [initialState]
     */
    protected open suspend fun onInitialized() {

    }

    /**
     * Call after service completely disposed
     */
    protected open fun onDisposed() {

    }

    internal suspend fun broadcastNewState(newServiceState: State) {
        try {

            if (isDisposed()) {
                logger.info("Service [$this] isDisposed. Can not broadcastNewState and change state to $newServiceState.")
                return
            }

            logger.info("Service [$this] change state from $serviceState to $newServiceState")

            val oldState = serviceState

            serviceState = newServiceState
            resultsChannel.emit(newServiceState)

            dispatchTriggerByState(oldState, newServiceState)

            onStateChanged(oldState, newServiceState)

        } catch (e: ClosedSendChannelException) {
            logger.info("Service [$this] result channel is closed.")
        }
    }

    private suspend fun dispatchTriggerByState(old: State, new: State) {
        val trigger = stateTriggerSelector?.findTrigger(stateTriggers, old, new)

        logger.info("Service [$this] try to find trigger for changing state from $old to $new. Trigger is found = ${trigger != null}")

        trigger?.let {

            logger.info("Service [$this] fired trigger $it.")

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
                logger.info("Service [${this@RedisCoroutineStateService}] on initialized.")
                broadcastNewState(initialState)
                onInitialized()
            }
        }

        when (serviceState) {
            is State.Created -> {
                initService()
                provideInitializedResult()
            }
            is State.Disposed -> {
                throw IllegalStateException("Service $this is disposed. Cannot initialize again.")
            }
            else -> {
                return
            }
        }
    }

    private fun subscribeToIntentMessages() {
        scope.launch(dispatcher) {
            while (isActive && !intentMessagesChannel.isClosedForReceive) {
                try {
                    val msg = intentMessagesChannel.receive()

                    val reducer = findReducer(msg)

                    logger.info("Service [${this@RedisCoroutineStateService}] Received intent $msg. Founded reducer: $reducer.")

                    val newStateFlow =
                        reducer.reduce(serviceState, msg)

                    newStateFlow?.let { stateFlow ->
                        stateFlow.collect {
                            logger.info("Service [${this@RedisCoroutineStateService}] collect new state $it")

                            broadcastNewState(it)
                        }
                    }
                } catch (e: ClosedReceiveChannelException) {
                    logger.info("Service [${this@RedisCoroutineStateService}] intent channel is closed.")
                }
            }

            disposeIfScopeNotActive()
        }
    }

    private fun findReducer(
        intentMessage: IntentMessage
    ): StateReducer {
        return reducerSelector.findReducer(reducers, serviceState, intentMessage)
    }

    private fun disposeIfScopeNotActive() {
        if (!scope.isActive) {
            logger.info("Service [$this] scope is not active anymore. Dispose service.")
            dispose()
        }
    }

    private fun assertScopeActive(
        errorPostfixMsgIfNotActive: String,
        throwError: Boolean = true
    ): Boolean {
        if (!scope.isActive) {
            val msg =
                "Service [$this] scope is not active anymore. Service is disposed and cannot [$errorPostfixMsgIfNotActive]."

            logger.info(msg)

            if (throwError) {
                throw IllegalStateException(msg)
            }

            return false
        }
        return true
    }

    private suspend fun awaitPassCreatedState() {
        while (serviceState is State.Created) {
            delay(AWAIT_INIT_DELAY_MS)
            logger.info("Service [$this] await passing initial state. Current state = $serviceState")
        }
    }
}