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

    companion object {
        private const val AWAIT_INIT_DELAY_MS = 1L
    }

    var serviceState: ActorServiceState = ActorServiceState.Created()
        private set

    protected var savedStateHandler: ServiceSavedStateHandler? = null

    private var resultsChannel = BroadcastChannel<ServiceResult<TResult>>(Channel.CONFLATED)

    private var intentMessagesChannel = Channel<IntentMessage>(Channel.UNLIMITED)

    private val subscribers =
        ConcurrentHashMap<ServiceSubscriber<TResult>, ReceiveChannel<ServiceResult<TResult>>>()

    /**
     * This method is used for handling Intents
     * Be aware of concurrency.
     * All suspending functions inside onIntentMsg will execute sequentially and next onIntentMsg will start after this execution finished.
     * If you need to start another coroutines inside onIntentMsg, don`t forget await all of them.
     *
     * @return can be null in this case you should broadcast new result with [broadcastNewStateWithResult] method
     */
    protected abstract suspend fun onIntentMsg(msg: IntentMessage): ServiceStateWithResult<TResult>?

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
                awaitPassCreatedState()

                logger.info("Service [${this@ActorService}] send msg to intent channel.")

                intentMessagesChannel.send(msg)
            } catch (e: ClosedSendChannelException) {
                logger.info("Service [${this@ActorService}] intent channel is closed.")
            }
        }
    }

    /**
     * Set ServiceSavedStateHandler implementation for saving service state due to process kill or even app closing.
     */
    fun setServiceSavedStateHandler(serviceSavedStateHandler: ServiceSavedStateHandler?) {
        savedStateHandler = serviceSavedStateHandler
        restoreState()
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
                logger.info("Service [${this@ActorService}] start listening new subscription")

                while (isActive && !subscription.isClosedForReceive) {
                    try {
                        val result = subscription.receive()
                        subscriber.onReceive(result)
                    } catch (e: ClosedReceiveChannelException) {
                        logger.info("Service [${this@ActorService}] result channel is closed.")
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

    /**
     * @return count of active subscribers
     */
    fun getSubscribersCount() = subscribers.size

    /**
     * Set initial service result that send to subscribers when service initialized
     */
    protected open fun getResultFotInitializedState(): ServiceResult<TResult> {
        return ServiceResult.InitResult()
    }

    /**
     * You can check [savedStateHandler] and restore previous saved state.
     * Best practice is to save some Id (or any small identical piece of data) that you got inside [onIntentMsg] than restore that Id here and send same intent.
     */
    protected open fun restoreState() {

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
     * Change service state [serviceState] and send result to subscribers if [canChangeState] returns true
     */
    protected suspend fun broadcastNewStateWithResult(
        stateWithResult: ServiceStateWithResult<TResult>
    ) {
        val canChangeState =
            isSystemDefinedState(stateWithResult.newServiceState) || canChangeState(
                stateWithResult.newServiceState,
                stateWithResult.result
            )

        if (!resultsChannel.isClosedForSend && canChangeState
        ) {

            if (stateWithResult.newServiceState !is ActorServiceState.Same) {
                serviceState = stateWithResult.newServiceState
            }

            try {
                resultsChannel.send(stateWithResult.result)
            } catch (e: ClosedSendChannelException) {
                logger.info("Service [$this] result channel is closed.")
            }
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
                logger.info("Service [${this@ActorService}] on initialized. Send empty result.")
                broadcastNewStateWithResult(
                    ServiceStateWithResult(
                        ActorServiceState.Initiated(),
                        getResultFotInitializedState()
                    )
                )
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

                    logger.info("Service [${this@ActorService}] received new intent message $msg")

                    val result = onIntentMsg(msg)

                    result?.let {
                        broadcastNewStateWithResult(it)
                    }
                } catch (e: ClosedReceiveChannelException) {
                    logger.info("Service [${this@ActorService}] intent channel is closed.")
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

    private fun assertScopeActive(errorPostfixMsgIfNotActive: String, throwError: Boolean = true): Boolean {
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
            logger.info("Service [$this] await passing created state. Current state = $serviceState")
        }
    }


}