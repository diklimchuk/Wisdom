package com.vkontakte.wisdom.old

import kotlinx.coroutines.flow.Flow
import kotlin.reflect.typeOf

interface Cache {

    fun <T : Any> put(
        key: String,
        value: T,
        persistClearing: Boolean = false,
    )

    fun <T : Any> put(
        key: String,
        valueProvider: (previousValue: T?) -> T,
        persistClearing: Boolean = false,
    )

    fun <T : Any> putIfPresent(
        key: String,
        valueProvider: (previousValue: T) -> T,
        persistClearing: Boolean = false,
    )

    fun remove(key: String)

    fun <T : Any> peek(key: String): T?

    fun <T : Any> observe(key: String): Flow<T?>

    fun contains(key: String): Boolean

    fun <T : Any> of(
        key: String,
        persistClearing: Boolean,
    ): CacheElement<T>

    /**
     * Clears data in cache and all previously returned [CacheElement]s, except then ones created with keepAlways = true
     */
    fun clear()

    /**
     * Clears all data even the one with persistClearing=true
     */
    fun clearWithPersistable()

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
