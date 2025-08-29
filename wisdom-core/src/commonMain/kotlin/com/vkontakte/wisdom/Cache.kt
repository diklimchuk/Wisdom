package com.vkontakte.wisdom

import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

interface Cache {
    fun <T : Any> put(key: String, value: T)

    fun remove(key: String)

    fun <T : Any> peek(key: String): T?

    fun <T : Any> putIfPresent(
        key: String,
        newValue: (previousValue: T) -> T,
    )

    fun <T : Any> put(
        key: String,
        newValue: (previousValue: T?) -> T,
    )

    fun <T : Any> observe(key: String): Flow<T>

    fun contains(key: String): Boolean

    fun <T : Any> of(
        key: String,
        keepAlways: Boolean,
    ): CacheElement<T>

    /**
     * Clears data in cache and all previously returned [CacheElement]s, except then ones created with keepAlways = true
     */
    fun clear()

    suspend fun updateIfEmpty(key: String)

    suspend fun forceUpdate(key: String)

    suspend fun updateIfPresent(key: String)
}


inline fun <reified T : Any> Cache.of(
    keepAlways: Boolean = false,
): CacheElement<T> {
    val key = typeOf<T>().toString()
    return of(key, keepAlways)
}
