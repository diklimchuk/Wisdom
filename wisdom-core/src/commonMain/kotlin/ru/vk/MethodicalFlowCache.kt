package ru.vk.flow.methodical

import co.touchlab.stately.collections.ConcurrentMutableMap
import ru.vk.flow.FlowCacheEffect
import ru.vk.memory.DelegatingCacheElement
import ru.vk.old.Cache
import ru.vk.old.CacheElement
import ru.vk.unsafeAs
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import money.vivid.elmslie.core.config.TussaudConfig
import money.vivid.elmslie.core.plot.CoroutinesElmPlot
import money.vivid.elmslie.core.plot.methodical.MethodicalElmPlot
import kotlin.run

interface FlowCacheStorage<T> {
    suspend fun persist(key: String, value: T?)
    fun retrieve(key: String): Flow<T?>
    fun peek(key: String): T?

    fun contains(key: String): Boolean

    fun clear(keys: Set<String>)

    fun clearAll()
}

class MethodicalFlowCacheResources<T>(
    val storage: FlowCacheStorage<T>,
)

class MemoryFlowCacheStorage<T> : FlowCacheStorage<T> {

    private val lock = SynchronizedObject()
    private val storage = ConcurrentMutableMap<String, MutableStateFlow<T?>>()

    override suspend fun persist(key: String, value: T?) {
        synchronized(lock) {
            storage[key]?.let { it.value = value }
                ?: run { storage[key] = MutableStateFlow(value) }
        }
    }

    override fun retrieve(key: String): Flow<T?> {
        synchronized(lock) {
            val value = storage[key] ?: MutableStateFlow<T?>(null)
            storage[key] = value
            return value
        }
    }

    override fun peek(key: String): T? {
        synchronized(lock) {
            return storage[key]?.value
        }
    }

    override fun contains(key: String): Boolean {
        synchronized(lock) {
            return storage.contains(key)
        }
    }

    override fun clear(keys: Set<String>) {
        synchronized(lock) {
            keys.forEach {
                storage.remove(it)
            }
        }
    }

    override fun clearAll() {
        synchronized(lock) {
            storage.clear()
        }
    }
}

class MethodicalFlowCache<T : Any>(
    storage: FlowCacheStorage<T>,
    private val persister: (T) -> Unit,
    private val dispatcher: CoroutineDispatcher = TussaudConfig.elmDispatcher,
) : Cache<T> {

    private val methodicalPlot = MethodicalElmPlot<
            MethodicalFlowCacheState,
            MethodicalFlowCacheEffect,
            MethodicalFlowCacheOperation<T>,
            MethodicalFlowCacheEvent<T>,
            MethodicalFlowCacheResources<T>
            >(
        resources = MethodicalFlowCacheResources(storage)
    )

    //    private val plot = SingleDispatcherElmPlot(
    private val plot = CoroutinesElmPlot(
        plot = methodicalPlot,
        dispatcher = dispatcher,
    )
//        dispatcher = dispatcher,
//    )

    init {
        plot.accept(MethodicalFlowCacheEvent.Input.Init())
    }

    override fun put(
        key: String,
        value: T,
        persistClearing: Boolean,
    ) {
        plot.accept(
            MethodicalFlowCacheEvent.Input.PutToStorage(
                key = key,
                valueProvider = { value },
                persistClearing = persistClearing
            )
        )
    }

    override fun put(
        key: String,
        valueProvider: (previousValue: T?) -> T,
        persistClearing: Boolean,
    ) {
        plot.accept(
            MethodicalFlowCacheEvent.Input.PutToStorage(
                key = key,
                valueProvider = valueProvider,
                persistClearing = persistClearing,
            )
        )
    }

    override fun put(
        key: String,
        valueFlowProvider: () -> Flow<T>,
        persistClearing: Boolean
    ) {
        plot.accept(
            MethodicalFlowCacheEvent.Input.PutFlowToStorage(
                key = key,
                valueFlowProvider = valueFlowProvider,
                persistClearing = persistClearing,
            )
        )
    }

    override fun putIfPresent(
        key: String,
        valueProvider: (previousValue: T) -> T,
        persistClearing: Boolean,
    ) {
        plot.accept(
            MethodicalFlowCacheEvent.Input.PutToStorageIfPresent(
                key = key,
                valueProvider = valueProvider,
                persistClearing = persistClearing,
            )
        )
    }

    override fun remove(key: String) {
        plot.accept(MethodicalFlowCacheEvent.Input.Remove(key))
    }

    override suspend fun peek(key: String): T? {
        return plot.effects.filterIsInstance<FlowCacheEffect.ElementReturned>().filter { it.key == key }.first().value as T?
    }

    override fun observe(key: String): Flow<T?> {
        return plot.effects
            .flowOn<T>(dispatcher)
            .filterIsInstance<FlowCacheEffect.ElementChanged<*>>()
            .filter { it.key == key }
            .map { it.value }
            .map { it.unsafeAs() }
    }

    override fun contains(key: String): Boolean {
        return plot.acceptWithResult<FlowCacheEffect.ContainsResult>(
            MethodicalFlowCacheEvent.Input.CheckContains(
                key
            )
        ).doesContain
    }

    override fun of(key: String, persistClearing: Boolean): CacheElement<T> {
        return DelegatingCacheElement(key, this, persistClearing)
    }

    override suspend fun clear() {
        plot.accept(MethodicalFlowCacheEvent.Input.Clear)
    }

    override suspend fun clearWithPersistable() {
        plot.accept(MethodicalFlowCacheEvent.Input.ClearAll)
    }

    override suspend fun updateIfEmpty(key: String) = Unit

    override suspend fun forceUpdate(key: String) = Unit

    override suspend fun updateIfPresent(key: String) = Unit
}