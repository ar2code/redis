package ru.ar2code.android.architecture.core.services

abstract class ActorServiceState {

    class Created : ActorServiceState()

    class Initiated : ActorServiceState()

    class Ready : ActorServiceState()

    class Disposed : ActorServiceState()
}