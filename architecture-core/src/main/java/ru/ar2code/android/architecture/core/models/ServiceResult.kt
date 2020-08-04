package ru.ar2code.android.architecture.core.models

abstract class ServiceResult<Type>(val payload: Type? = null) where Type : Any {

    class EmptyResult<Type>(payload: Type? = null) : ServiceResult<Type>(payload) where Type : Any

    class BasicResult<Type>(payload: Type? = null) : ServiceResult<Type>(payload) where Type : Any


}