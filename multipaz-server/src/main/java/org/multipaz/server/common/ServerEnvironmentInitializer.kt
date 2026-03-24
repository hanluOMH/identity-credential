package org.multipaz.server.common

import kotlin.reflect.KClass

interface ServerEnvironmentInitializer {
    fun<T: Any> add(clazz: KClass<T>, instance: T)
}