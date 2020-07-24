package ru.ar2code.android.architecture.core.services

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import ru.ar2code.android.architecture.core.impl.DefaultLogger
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.models.ServiceResult
import ru.ar2code.utils.Logger

@ExperimentalCoroutinesApi
abstract class ActorService<TResult>(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val logger: Logger = DefaultLogger()
) where TResult : Any {

    protected var serviceState: ActorServiceState = ActorServiceState.Created()
        private set

    private var resultsChannel = BroadcastChannel<ServiceResult<TResult>>(Channel.CONFLATED)

    private var intentMessagesChannel = Channel<IntentMessage>(Channel.UNLIMITED)

    private val subscribers =
        mutableMapOf<ServiceSubscriber<TResult>, ReceiveChannel<ServiceResult<TResult>>>()

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
        scope.launch(dispatcher) {
            intentMessagesChannel.send(msg)
        }
    }

    /**
     * After disposing service can not get intents and send results.
     */
    fun dispose() {
        serviceState = ActorServiceState.Disposed()

        intentMessagesChannel.close()
        resultsChannel.close()
    }

    /**
     * @return if true service can not get intents and send results.
     */
    fun isDisposed() = serviceState is ActorServiceState.Disposed

    /**
     * Subscribe to service's results.
     * Subscribing is alive while service is not disposed [isDisposed] and [scope] not cancelled
     */
    @Synchronized
    fun subscribe(subscriber: ServiceSubscriber<TResult>) {
        scope.launch(dispatcher) {

            val isSubscriberExists = subscribers.keys.any { it == subscriber }
            if (isSubscriberExists) {
                logger.warning("Subscriber $subscriber already exists in service $this.")
                return@launch
            }

            val subscription = resultsChannel.openSubscription()

            subscribers[subscriber] = subscription

            while (isActive && !subscription.isClosedForReceive) {
                try {
                    val result = subscription.receive()
                    subscriber.onReceive(result)

                } catch (e: ClosedReceiveChannelException) {
                    logger.info("Service's result channel is closed.")
                }
            }
        }
    }

    /**
     * Stop listening service`s result by this [subscriber]
     */
    @Synchronized
    fun unsubscribe(subscriber: ServiceSubscriber<TResult>) {
        val subscription = subscribers[subscriber]
        subscription?.let {
            it.cancel()
            subscribers.remove(subscriber)

            logger.warning("Subscriber $subscriber unsubscribe from service $this.")
        } ?: kotlin.run {
            logger.warning("Subscriber $subscriber does not exist in service $this.")
        }
    }

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
     * Change service [serviceState] and send result to subscribers if [canChangeState] returns true
     */
    protected suspend fun provideResult(
        newServiceState: ActorServiceState,
        result: ServiceResult<TResult>
    ) {
        onIntentHandlingFinished()

        if (!resultsChannel.isClosedForSend && canChangeState(newServiceState, result)) {
            serviceState = newServiceState
            resultsChannel.send(result)
        }
    }

    private fun initialize() {

        fun initService() {
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
                throw IllegalStateException("ServiceActor is disposed. Cannot initialize again.")
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
                    logger.info("Service's intent channel is closed.")
                }
            }
        }
    }

}