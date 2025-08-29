package com.vkontakte.wisdom

import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.collections.ConcurrentMutableSet
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map


// TODO: Test coverage
class MemoryCache : Cache {

    private val clearableKeys = ConcurrentMutableSet<String>()
    private val cache = ConcurrentMutableMap<String, Pair<SynchronizedObject, MutableStateFlow<Option<Any>>>>()

    override fun <T : Any> put(key: String, value: T) {
        getSubject<T>(key).second.value = Option(value)
    }

    override fun remove(key: String) {
        cache[key]?.second?.value = Option(null)
    }

    override fun <T : Any> peek(key: String): T? = getSubject<T>(key).second.value.value

    override fun <T : Any> putIfPresent(
        key: String,
        newValue: (previousValue: T) -> T,
    ) = updateInternal<T>(key) { it?.let(newValue) }

    override fun <T : Any> put(
        key: String,
        newValue: (previousValue: T?) -> T,
    ) = updateInternal(key, newValue)


    override fun <T : Any> observe(key: String): Flow<T> = getSubject<T>(key).second
        .filter { it.hasValue }
        .flowOn(Dispatchers.Default)
        .map { it.value!! }

    override fun contains(key: String): Boolean = getSubject<Any>(key).second.value.hasValue

    override fun <T : Any> of(
        key: String,
        keepAlways: Boolean,
    ): CacheElement<T> {
        if (!keepAlways) clearableKeys.add(key)
        return DelegatingCacheHandle(key, this)
    }

    /**
     * Clears data in cache and all previously returned [CacheElement]s, except then ones created with keepAlways = true
     */
    override fun clear() {
        clearableKeys.forEach { remove(it) }
    }

    override suspend fun updateIfEmpty(key: String) = Unit

    override suspend fun forceUpdate(key: String) = Unit

    override suspend fun updateIfPresent(key: String) = Unit

    private fun <T : Any> updateInternal(key: String, newValue: (previousValue: T?) -> T?) {
        val subject = getSubject<T>(key)

        synchronized(getSubject<T>(key).first) {
            newValue(subject.second.value.value)?.let { subject.second.value = Option(it) }
        }
    }

    private fun <T : Any> getSubject(key: String): Pair<SynchronizedObject, MutableStateFlow<Option<T>>> {
        @Suppress("UNCHECKED_CAST")
        return cache.getOrPut(key) { SynchronizedObject() to MutableStateFlow(Option(null)) } as Pair<SynchronizedObject, MutableStateFlow<Option<T>>>
    }
}