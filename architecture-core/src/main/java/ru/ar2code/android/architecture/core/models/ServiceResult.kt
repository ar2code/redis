package ru.ar2code.android.architecture.core.models

abstract class ServiceResult<Type>(val payload: Type? = null) where Type : Any