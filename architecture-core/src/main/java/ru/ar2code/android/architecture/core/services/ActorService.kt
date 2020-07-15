package ru.ar2code.android.architecture.core.services

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import ru.ar2code.android.architecture.core.models.IntentMessage
import ru.ar2code.android.architecture.core.models.ServiceResult

@ExperimentalCoroutinesApi
abstract class ActorService<TResult>(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) where TResult : Any {

    protected var serviceState: ActorServiceState = ActorServiceState.Created()
        private set

    private var resultsChannel = BroadcastChannel<ServiceResult<TResult>>(Channel.CONFLATED)

    private var intentMessagesChannel = Channel<IntentMessage>(Channel.UNLIMITED)

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

    fun sendIntent(msg: IntentMessage) {
        scope.launch (dispatcher){
            intentMessagesChannel.send(msg)
        }
    }

    fun dispose() {
        serviceState = ActorServiceState.Disposed()

        intentMessagesChannel.close()
        resultsChannel.close()
    }

    fun isDisposed() = serviceState is ActorServiceState.Disposed

    fun subscribe(onResult: (ServiceResult<TResult>?) -> Unit) {
        scope.launch(dispatcher) {
            val subscription = resultsChannel.openSubscription()

            while (isActive && !subscription.isClosedForReceive) {
                try {
                    val result = subscription.receive()
                    onResult(result)
                } catch (e: ClosedReceiveChannelException) {
                    //channel is closed
                }
            }
        }
    }

    private fun subscribeToIntentMessages() {
        scope.launch (dispatcher){
            while (isActive && !intentMessagesChannel.isClosedForReceive) {
                try {
                    val msg = intentMessagesChannel.receive()
                    onIntentMsg(msg)
                } catch (e: ClosedReceiveChannelException) {
                    //channel is closed
                }
            }
        }
    }

    protected open fun getResultFotInitializedState(): ServiceResult<TResult> {
        return ServiceResult.EmptyResult()
    }

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
}