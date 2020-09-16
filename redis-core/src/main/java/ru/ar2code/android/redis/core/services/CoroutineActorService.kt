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

package ru.ar2code.android.redis.core.services

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.collect
import ru.ar2code.android.redis.core.models.IntentMessage
import ru.ar2code.utils.Logger
import java.util.concurrent.ConcurrentHashMap

@ExperimentalCoroutinesApi
abstract class CoroutineActorService(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val initialState: ActorServiceState,
    private val reducers: List<StateReducer>,
    savedStateHandler: ServiceSavedStateHandler?,
    private val logger: Logger
) : SavedStateActorService(savedStateHandler) {

    companion object {
        private const val AWAIT_INIT_DELAY_MS = 1L
        private val CREATED_STATE = ActorServiceState.Created()
    }

    override var serviceState: ActorServiceState = CREATED_STATE
        get() {
            return field.clone()
        }
        protected set

    private var resultsChannel = BroadcastChannel<ActorServiceState>(Channel.CONFLATED)

    private var intentMessagesChannel = Channel<IntentMessage>(Channel.UNLIMITED)

    private val subscribers =
        ConcurrentHashMap<ServiceSubscriber, ReceiveChannel<ActorServiceState>>()

    init {
        initialize()
    }

    /**
     * Send intent to service for doing some action
     */
    override fun dispatch(msg: IntentMessage) {
        if (!assertScopeActive("send intent ${msg.msgType}"))
            return

        scope.launch(dispatcher) {
            try {
                awaitPassCreatedState()
                intentMessagesChannel.send(msg)
            } catch (e: ClosedSendChannelException) {
                logger.info("Service [${this@CoroutineActorService}] intent channel is closed.")
            }
        }
    }

    /**
     * After disposing service can not get intents and send results.
     */
    override fun dispose() {
        serviceState = ActorServiceState.Disposed()

        intentMessagesChannel.close()
        resultsChannel.close()

        subscribers.clear()

        onDisposed()
    }

    /**
     * @return if true service can not get intents and send results.
     */
    override fun isDisposed(): Boolean {
        disposeIfScopeNotActive()
        return serviceState is ActorServiceState.Disposed
    }

    /**
     * Subscribe to service's results.
     * Subscribing is alive while service is not disposed [isDisposed] and [scope] not cancelled
     */
    @Synchronized
    override fun subscribe(subscriber: ServiceSubscriber) {

        fun isSubscriberExists(): Boolean {
            val isSubscriberExists = subscribers.keys.any { it == subscriber }
            if (isSubscriberExists) {
                logger.warning("Subscriber $subscriber already exists in service $this.")
            }
            return isSubscriberExists
        }

        fun openSubscription(): ReceiveChannel<ActorServiceState> {
            val subscription = resultsChannel.openSubscription()
            subscribers[subscriber] = subscription
            return subscription
        }

        fun listening(subscription: ReceiveChannel<ActorServiceState>) {
            scope.launch(dispatcher) {
                logger.info("Service [${this@CoroutineActorService}] start listening new subscription")

                while (isActive && !subscription.isClosedForReceive) {
                    try {
                        val result = subscription.receive()
                        subscriber.onReceive(result)
                    } catch (e: ClosedReceiveChannelException) {
                        logger.info("Service [${this@CoroutineActorService}] result channel is closed.")
                    }
                }
                disposeIfScopeNotActive()
            }
        }

        if (!assertScopeActive("subscribe $subscriber to service"))
            return

        if (isSubscriberExists())
            return

        val subscription = openSubscription()

        listening(subscription)

    }

    /**
     * Stop listening service`s result by this [subscriber]
     */
    @Synchronized
    override fun unsubscribe(subscriber: ServiceSubscriber) {

        fun getSubscription(): ReceiveChannel<ActorServiceState>? {
            val subscription = subscribers[subscriber]
            if (subscription == null) {
                logger.info("Subscriber $subscriber does not exist in service $this.")
            }
            return subscription
        }

        fun deleteSubscription(subscription: ReceiveChannel<ActorServiceState>?) {
            subscription?.let {
                it.cancel()
                subscribers.remove(subscriber)

                logger.info("Subscriber $subscriber unsubscribe from service $this.")
            }
        }

        val subscription = getSubscription()
        deleteSubscription(subscription)
    }

    /**
     * @return count of active subscribers
     */
    override fun getSubscribersCount() = subscribers.size

    protected open fun onDisposed() {}

    private suspend fun broadcastNewState(newServiceState: ActorServiceState) {
        try {
            logger.info("Service [$this] change state from $serviceState to $newServiceState")

            serviceState = newServiceState
            resultsChannel.send(newServiceState)
        } catch (e: ClosedSendChannelException) {
            logger.info("Service [$this] result channel is closed.")
        }
    }

    private fun isSystemDefinedState(state: ActorServiceState): Boolean {
        return when (state) {
            is ActorServiceState.Created -> true
            is ActorServiceState.Initiated -> true
            is ActorServiceState.Disposed -> true
            else -> false
        }
    }

    private fun initialize() {

        fun initService() {
            assertScopeActive("initialize service")
            subscribeToIntentMessages()
        }

        fun provideInitializedResult() {
            this.scope.launch(dispatcher) {
                logger.info("Service [${this@CoroutineActorService}] on initialized. Send empty result.")
                broadcastNewState(initialState)
                restoreState()
            }
        }

        when (serviceState) {
            is ActorServiceState.Created -> {
                initService()
                provideInitializedResult()
            }
            is ActorServiceState.Disposed -> {
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

                    val reducer = findReducer(msg.msgType)

                    logger.info("Service [${this@CoroutineActorService}] received new intent message with type ${msg.msgType}. Founded reducer: $reducer")

                    val newState = reducer.reduce(serviceState, msg.msgType)

                    newState.let { stateFlow ->
                        stateFlow.collect {
                            broadcastNewState(it)
                        }
                    }
                } catch (e: ClosedReceiveChannelException) {
                    logger.info("Service [${this@CoroutineActorService}] intent channel is closed.")
                }
            }

            disposeIfScopeNotActive()
        }
    }

    private fun findReducer(
        intentMessageType: IntentMessage.IntentMessageType<Any>
    ): StateReducer {
        logger.info("Service [$this] try to find reducer for ($serviceState,$intentMessageType)")

        return reducers.firstOrNull {
            it.isReducerApplicable(serviceState, intentMessageType)
        } ?: throw IllegalArgumentException("Reducer for ($serviceState,$intentMessageType) did not found.")
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
        while (serviceState is ActorServiceState.Created) {
            delay(AWAIT_INIT_DELAY_MS)
            logger.info("Service [$this] await passing initial state. Current state = $serviceState")
        }
    }


}