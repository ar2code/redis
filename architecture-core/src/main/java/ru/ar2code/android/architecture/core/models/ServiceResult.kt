package ru.ar2code.android.architecture.core.models

abstract class ServiceResult<Type>(val payload: Type? = null) where Type : Any {

    open class InitResult<Type>(payload: Type? = null) :
        ServiceResult<Type>(payload) where Type : Any

    open class EmptyResult<Type>(payload: Type? = null) :
        ServiceResult<Type>(payload) where Type : Any

    open class BasicResult<Type>(payload: Type? = null) :
        ServiceResult<Type>(payload) where Type : Any


}