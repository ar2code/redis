package ru.ar2code.android.architecture.core.services

abstract class ActorServiceState {

    class Created : ActorServiceState()

    class Initiated : ActorServiceState()

    class Disposed : ActorServiceState()

    class Same : ActorServiceState()
}