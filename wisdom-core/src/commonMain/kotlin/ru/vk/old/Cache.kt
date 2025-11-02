package ru.vk.old

import kotlinx.coroutines.flow.Flow
import kotlin.reflect.typeOf

interface Cache<T : Any> {

    fun put(
        key: String,
        value: T,
        persistClearing: Boolean = false,
    )

    fun put(
        key: String,
        valueProvider: (previousValue: T?) -> T,
        persistClearing: Boolean = false,
    )

    fun put(
        key: String,
        valueFlowProvider: () -> Flow<T>,
        persistClearing: Boolean = false,
    )

    fun putIfPresent(
        key: String,
        valueProvider: (previousValue: T) -> T,
        persistClearing: Boolean = false,
    )

    fun remove(key: String)

    suspend fun peek(key: String): T?

    fun observe(key: String): Flow<T?>

    fun contains(key: String): Boolean

    fun of(
        key: String,
        persistClearing: Boolean,
    ): CacheElement<T>

    /**
     * Clears data in cache and all previously returned [CacheElement]s, except then ones created with keepAlways = true
     */
    suspend fun clear()

    /**
     * Clears all data even the one with persistClearing=true
     */
    suspend fun clearWithPersistable()

    suspend fun updateIfEmpty(key: String)

    suspend fun forceUpdate(key: String)

    suspend fun updateIfPresent(key: String)
}


inline fun <reified T : Any> Cache<T>.of(
    keepAlways: Boolean = false,
): CacheElement<T> {
    val key = typeOf<T>().toString()
    return of(key, keepAlways)
}
