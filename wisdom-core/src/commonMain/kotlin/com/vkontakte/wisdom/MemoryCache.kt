package com.vkontakte.wisdom

import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.collections.ConcurrentMutableSet
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

/**
 * Represents a single entry in cache.
 * For example, it can be a cache for a list of items.
 */
interface CacheElement<T> {

    fun set(value: T)

    suspend fun get(): T?

    fun observe(): Flow<T>

    fun clear()

    fun peek(): T?

    fun updateIfPresent(newValue: (previousValue: T) -> T)

    fun update(newValue: (previousValue: T?) -> T)

    fun hasValue(): Boolean
}

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

    fun <T : Any> of(
        customKey: String? = null,
        keepAlways: Boolean = false,
        type: KClass<T>,
    ): CacheElement<T>

    /**
     * Clears data in cache and all previously returned [CacheElement]s, except then ones created with keepAlways = true
     */
    fun clear()
}

class MemoryCache : Cache {

    private val clearableKeys = ConcurrentMutableSet<String>()
    private val cache = ConcurrentMutableMap<String, MutableStateFlow<Option<Any>>>()

    override fun <T : Any> put(key: String, value: T) {
        getSubject<T>(key).value = Option(value)
    }

    override fun remove(key: String) {
        cache[key]?.value = Option(null)
    }

    override fun <T : Any> peek(key: String): T? = getSubject<T>(key).value.value

    override fun <T : Any> putIfPresent(
        key: String,
        newValue: (previousValue: T) -> T,
    ) = updateInternal<T>(key) { it?.let(newValue) }

    override fun <T : Any> put(
        key: String,
        newValue: (previousValue: T?) -> T,
    ) = updateInternal(key, newValue)


    override fun <T : Any> observe(key: String): Flow<T> = getSubject<T>(key)
        .filter { it.hasValue }
        .flowOn(Dispatchers.Default) // TODO: FlowOn, Dispatchers io
        .map { it.value!! }

    override fun contains(key: String): Boolean = getSubject<Any>(key).value.hasValue

    override fun <T : Any> of(
        key: String,
        keepAlways: Boolean,
    ): CacheElement<T> {
        if (!keepAlways) clearableKeys.add(key)
//        return DelegatingCacheHandle(key, this)
        return object : CacheElement<T> {
            override fun set(value: T) {
                TODO("Not yet implemented")
            }

            override suspend fun get(): T? {
                TODO("Not yet implemented")
            }

            override fun observe(): Flow<T> {
                TODO("Not yet implemented")
            }

            override fun clear() {
                TODO("Not yet implemented")
            }

            override fun peek(): T? {
                TODO("Not yet implemented")
            }

            override fun updateIfPresent(newValue: (previousValue: T) -> T) {
                TODO("Not yet implemented")
            }

            override fun update(newValue: (previousValue: T?) -> T) {
                TODO("Not yet implemented")
            }

            override fun hasValue(): Boolean {
                TODO("Not yet implemented")
            }
        }
    }

    override fun <T : Any> of(
        customKey: String?,
        keepAlways: Boolean,
        type: KClass<T>,
    ): CacheElement<T> {
//        val key = CacheKeyGenerator(TypeToken.get(type), customKey).generate(InMemoryCacheArg)
//        return of(key, keepAlways)
        return of("", keepAlways)
    }

    /**
     * Clears data in cache and all previously returned [CacheElement]s, except then ones created with keepAlways = true
     */
    override fun clear() {
        clearableKeys.forEach { remove(it) }
    }

    private fun <T : Any> updateInternal(key: String, newValue: (previousValue: T?) -> T?) {
        val subject = getSubject<T>(key)

        kotlinx.atomicfu.locks.synchronized(SynchronizedObject()) {
            newValue(subject.value.value)?.let { subject.value = Option(it) }
        }
    }

    private fun <T : Any> getSubject(key: String): MutableStateFlow<Option<T>> {
        @Suppress("UNCHECKED_CAST")
        return cache.getOrPut(key) { MutableStateFlow(Option(null)) } as MutableStateFlow<Option<T>>
    }
}