package com.vkontakte.wisdom.memory

import com.vkontakte.wisdom.old.Cache
import com.vkontakte.wisdom.old.CacheElement
import com.vkontakte.wisdom.unsafeAs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import money.vivid.elmslie.core.config.TussaudConfig
import money.vivid.elmslie.core.plot.CoroutinesElmPlot
import money.vivid.elmslie.core.plot.ElmPlot
import money.vivid.elmslie.core.plot.NoOpPerformer

class Memory(
    private val dispatcher: CoroutineDispatcher = TussaudConfig.elmDispatcher
) : Cache {

    //    private val plot = SingleDispatcherElmPlot(
    private val plot = CoroutinesElmPlot(
        plot = ElmPlot(
            scheme = MemoryScheme,
            performer = NoOpPerformer()
        ),
        dispatcher = dispatcher,
    )
//        dispatcher = dispatcher,
//    )

    init {
        plot.accept(MemoryEvent.Input.Init)
    }

    override fun <T : Any> put(
        key: String,
        value: T,
        persistClearing: Boolean,
    ) {
        plot.accept(MemoryEvent.Input.Put<T>(key, { value }, persistClearing))
    }

    override fun <T : Any> put(
        key: String,
        valueProvider: (previousValue: T?) -> T,
        persistClearing: Boolean,
    ) {
        plot.accept(MemoryEvent.Input.Put(key, valueProvider, persistClearing))
    }

    override fun <T : Any> putIfPresent(
        key: String,
        valueProvider: (previousValue: T) -> T,
        persistClearing: Boolean,
    ) {
        plot.accept(MemoryEvent.Input.PutIfPresent(key, valueProvider, persistClearing))
    }

    override fun remove(key: String) {
        plot.accept(MemoryEvent.Input.Remove(key))
    }

    override fun <T : Any> peek(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return plot.acceptWithResult<MemoryEffect.ElementReturned>(MemoryEvent.Input.Peek(key)).value as? T
    }

    override fun <T : Any> observe(key: String): Flow<T?> {
        return plot.effects
            .flowOn(dispatcher)
            .filterIsInstance<MemoryEffect.ElementChanged<*>>()
            .filter { it.key == key }
            .map { it.value }
            .map { it.unsafeAs() }
    }

    override fun contains(key: String): Boolean {
        return plot.acceptWithResult<MemoryEffect.ContainsResult>(MemoryEvent.Input.CheckContains(key)).doesContain
    }

    override fun <T : Any> of(key: String, persistClearing: Boolean): CacheElement<T> {
        return DelegatingCacheElement(key, this, persistClearing)
    }

    override fun clear() {
        plot.accept(MemoryEvent.Input.Clear)
    }

    override fun clearWithPersistable() {
        plot.accept(MemoryEvent.Input.ClearAll)
    }

    override suspend fun updateIfEmpty(key: String) = Unit

    override suspend fun forceUpdate(key: String) = Unit

    override suspend fun updateIfPresent(key: String) = Unit
}