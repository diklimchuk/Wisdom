package com.vkontakte.wisdom

import kotlinx.coroutines.flow.Flow

internal class DelegatingCacheHandle<T : Any>(
    private val key: String,
    private val cache: Cache
) : CacheElement<T> {

    override fun set(value: T) = cache.put(key, value)

    override fun observe(): Flow<T> = cache.observe(key)

    override fun clear() = cache.remove(key)

    override fun peek(): T? = cache.peek(key)

    override fun updateIfPresent(newValue: (previousValue: T) -> T) = cache.putIfPresent(key, newValue)

    override fun update(newValue: (previousValue: T?) -> T) = cache.put(key, newValue)

    override fun hasValue(): Boolean = cache.contains(key)

    override suspend fun updateIfPresent() = cache.updateIfPresent(key)

    override suspend fun updateIfEmpty() = cache.updateIfEmpty(key)

    override suspend fun forceUpdate() = cache.forceUpdate(key)
}