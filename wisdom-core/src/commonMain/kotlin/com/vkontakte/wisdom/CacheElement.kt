package com.vkontakte.wisdom

import kotlinx.coroutines.flow.Flow

/**
 * Represents a single entry in cache.
 * For example, it can be a cache for a list of items.
 */
interface CacheElement<T> {

    fun set(value: T)

    fun observe(): Flow<T>

    fun clear()

    fun peek(): T?

    fun updateIfPresent(newValue: (previousValue: T) -> T)

    fun update(newValue: (previousValue: T?) -> T)

    fun hasValue(): Boolean

    suspend fun updateIfEmpty()

    suspend fun forceUpdate()

    suspend fun updateIfPresent()
}