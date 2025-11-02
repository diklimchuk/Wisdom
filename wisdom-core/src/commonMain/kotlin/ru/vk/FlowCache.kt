package ru.vk.flow

import ru.vk.memory.DelegatingCacheElement
import ru.vk.old.Cache
import ru.vk.old.CacheElement
import ru.vk.unsafeAs
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

class FlowCache<T>(
    private val persister: (T) -> Unit,
    private val dispatcher: CoroutineDispatcher = TussaudConfig.elmDispatcher,
) : Cache {

    //    private val plot = SingleDispatcherElmPlot(
    private val plot = CoroutinesElmPlot(
        plot = ElmPlot(
            scheme = FlowCacheScheme,
            performer = NoOpPerformer()
        ),
        dispatcher = dispatcher,
    )
//        dispatcher = dispatcher,
//    )

    init {
        plot.accept(FlowCacheEvent.Input.Init)
    }

    override fun <T : Any> put(
        key: String,
        value: T,
        persistClearing: Boolean,
    ) {
        plot.accept(
            FlowCacheEvent.Input.PutToState(
                key = key,
                valueProvider = { value },
                persistClearing = persistClearing
            )
        )
    }

    override fun <T : Any> put(
        key: String,
        valueProvider: (previousValue: T?) -> T,
        persistClearing: Boolean,
    ) {
        plot.accept(
            FlowCacheEvent.Input.PutToState(
                key = key,
                valueProvider = valueProvider as ((Any?) -> Any),
                persistClearing = persistClearing,
            )
        )
    }

    override fun <T : Any> put(
        key: String,
        valueFlowProvider: () -> Flow<T>,
        persistClearing: Boolean
    ) {
        plot.accept(
            FlowCacheEvent.Input.PutToStorage(
                key = key,
                persister = persister as (Any) -> Unit,
                retriever = valueFlowProvider as (() -> Flow<Any>),
                persistClearing = persistClearing,
            )
        )
    }

    override fun <T : Any> putIfPresent(
        key: String,
        valueProvider: (previousValue: T) -> T,
        persistClearing: Boolean,
    ) {
        plot.accept(
            FlowCacheEvent.Input.PutToStateIfPresent(
                key = key,
                valueProvider = valueProvider as ((Any) -> Any),
                persistClearing = persistClearing,
            )
        )
    }

    override fun <T : Any> putIfPresent(
        key: String,
        valueFlowProvider: () -> Flow<T>,
        persistClearing: Boolean
    ) {
        plot.accept(
            FlowCacheEvent.Input.PutToStorageIfPresent(
                key = key,
                persister = persister as (Any) -> Unit,
                retriever = valueFlowProvider as (() -> Flow<Any>),
                persistClearing = persistClearing,
            )
        )
    }

    override fun remove(key: String) {
        plot.accept(FlowCacheEvent.Input.Remove(key))
    }

    override suspend fun <T : Any> peek(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return plot.acceptWithResult<FlowCacheEffect.ElementReturned>(FlowCacheEvent.Input.Peek(key)).value as? T
    }

    override fun <T : Any> observe(key: String): Flow<T?> {
        return plot.effects
            .flowOn(dispatcher)
            .filterIsInstance<FlowCacheEffect.ElementChanged<*>>()
            .filter { it.key == key }
            .map { it.value }
            .map { it.unsafeAs() }
    }

    override fun contains(key: String): Boolean {
        return plot.acceptWithResult<FlowCacheEffect.ContainsResult>(
            FlowCacheEvent.Input.CheckContains(
                key
            )
        ).doesContain
    }

    override fun <T : Any> of(key: String, persistClearing: Boolean): CacheElement<T> {
        return DelegatingCacheElement(key, this, persistClearing)
    }

    override fun clear() {
        plot.accept(FlowCacheEvent.Input.Clear)
    }

    override fun clearWithPersistable() {
        plot.accept(FlowCacheEvent.Input.ClearAll)
    }

    override suspend fun updateIfEmpty(key: String) = Unit

    override suspend fun forceUpdate(key: String) = Unit

    override suspend fun updateIfPresent(key: String) = Unit
}