package ru.ar2code.android.architecture.core.services

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.models.ServiceResult
import ru.ar2code.utils.Logger
import java.util.concurrent.ConcurrentHashMap

@ExperimentalCoroutinesApi
abstract class ActorService<TResult>(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val logger: Logger
) where TResult : Any {

    var serviceState: ActorServiceState = ActorServiceState.Created()
        private set

    private var resultsChannel = BroadcastChannel<ServiceResult<TResult>>(Channel.CONFLATED)

    private var intentMessagesChannel = Channel<IntentMessage>(Channel.UNLIMITED)

    private val subscribers =
        ConcurrentHashMap<ServiceSubscriber<TResult>, ReceiveChannel<ServiceResult<TResult>>>()

    /**
     * This method is used for handling Intents
     * Be aware of concurrency.
     * All suspending functions inside onIntentMsg will execute sequentially and next onIntentMsg will start after this execution finished.
     * If you need to start another coroutines inside onIntentMsg, don`t forget await all of them.
     */
    protected abstract suspend fun onIntentMsg(msg: IntentMessage)

    init {
        initialize()
    }

    /**
     * Send intent to service for doing some action
     */
    fun sendIntent(msg: IntentMessage) {
        if (!assertScopeActive("send intent ${msg.msgType}"))
            return

        scope.launch(dispatcher) {
            try {
                intentMessagesChannel.send(msg)
            } catch (e: ClosedSendChannelException) {
                logger.info("Service [$this] intent channel is closed.")
            }
        }
    }

    /**
     * After disposing service can not get intents and send results.
     */
    fun dispose() {
        serviceState = ActorServiceState.Disposed()

        intentMessagesChannel.close()
        resultsChannel.close()

        subscribers.clear()
    }

    /**
     * @return if true service can not get intents and send results.
     */
    fun isDisposed(): Boolean {
        disposeIfScopeNotActive()
        return serviceState is ActorServiceState.Disposed
    }

    /**
     * Subscribe to service's results.
     * Subscribing is alive while service is not disposed [isDisposed] and [scope] not cancelled
     */
    @Synchronized
    fun subscribe(subscriber: ServiceSubscriber<TResult>) {

        fun isSubscriberExists(): Boolean {
            val isSubscriberExists = subscribers.keys.any { it == subscriber }
            if (isSubscriberExists) {
                logger.warning("Subscriber $subscriber already exists in service $this.")
            }
            return isSubscriberExists
        }

        fun openSubscription(): ReceiveChannel<ServiceResult<TResult>> {
            val subscription = resultsChannel.openSubscription()
            subscribers[subscriber] = subscription
            return subscription
        }

        fun listening(subscription: ReceiveChannel<ServiceResult<TResult>>) {
            scope.launch(dispatcher) {
                while (isActive && !subscription.isClosedForReceive) {
                    try {
                        val result = subscription.receive()
                        subscriber.onReceive(result)
                    } catch (e: ClosedReceiveChannelException) {
                        logger.info("Service [$this] result channel is closed.")
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
    fun unsubscribe(subscriber: ServiceSubscriber<TResult>) {

        fun getSubscription(): ReceiveChannel<ServiceResult<TResult>>? {
            val subscription = subscribers[subscriber]
            if (subscription == null) {
                logger.info("Subscriber $subscriber does not exist in service $this.")
            }
            return subscription
        }

        fun deleteSubscription(subscription: ReceiveChannel<ServiceResult<TResult>>?) {
            subscription?.let {
                it.cancel()
                subscribers.remove(subscriber)

                logger.info("Subscriber $subscriber unsubscribe from service $this.")
            }
        }

        val subscription = getSubscription()
        deleteSubscription(subscription)
    }

    fun getSubscribersCount() = subscribers.size

    /**
     * Set initial service result that send to subscribers when service initialized
     */
    protected open fun getResultFotInitializedState(): ServiceResult<TResult> {
        return ServiceResult.EmptyResult()
    }

    /**
     * You can check previous [serviceState] and decide to change or not to [newServiceState] given by [result]
     * @return true if should change [serviceState] with [newServiceState]
     */
    protected open fun canChangeState(
        newServiceState: ActorServiceState,
        result: ServiceResult<TResult>
    ): Boolean {
        return true
    }

    /**
     * This method is used for cleaning all data after current intent handling finished.
     * After that you can start another intent handling and don`t afraid of concurrent data modification.
     */
    protected open fun onIntentHandlingFinished() {}

    /**
     * Change service state [serviceState] and send result to subscribers if [canChangeState] returns true
     */
    protected suspend fun provideResult(
        newServiceState: ActorServiceState,
        result: ServiceResult<TResult>
    ) {
        onIntentHandlingFinished()

        if (!resultsChannel.isClosedForSend && canChangeState(newServiceState, result)) {

            if (newServiceState !is ActorServiceState.Same) {
                serviceState = newServiceState
            }

            try {
                resultsChannel.send(result)
            } catch (e: ClosedSendChannelException) {
                logger.info("Service [$this] result channel is closed.")
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
                provideResult(ActorServiceState.Initiated(), getResultFotInitializedState())
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
                    onIntentMsg(msg)
                } catch (e: ClosedReceiveChannelException) {
                    logger.info("Service [$this] intent channel is closed.")
                }
            }

            disposeIfScopeNotActive()
        }
    }

    private fun disposeIfScopeNotActive() {
        if (!scope.isActive) {
            logger.info("Service [$this] scope is not active anymore. Dispose service.")
            dispose()
        }
    }

    private fun assertScopeActive(msgIfNotActive: String, throwError: Boolean = true): Boolean {
        if (!scope.isActive) {
            val msg =
                "Service [$this] scope is not active anymore. Service is disposed and cannot [$msgIfNotActive]."

            logger.info(msg)

            if (throwError) {
                throw IllegalStateException(msg)
            }

            return false
        }
        return true
    }

}