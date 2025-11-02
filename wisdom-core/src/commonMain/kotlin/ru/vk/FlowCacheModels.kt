package ru.vk.flow

import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.collections.ConcurrentMutableSet
import ru.vk.old.Option
import kotlinx.coroutines.flow.Flow

data class FlowCacheState(
    val clearableKeys: ConcurrentMutableSet<String>,
    val elements: ConcurrentMutableMap<String, Option<Any>>,
    val elementAccessors: ConcurrentMutableMap<String, Pair<() -> Any, (Any) -> Unit>>
)

sealed class FlowCacheEvent {
    sealed class Input : FlowCacheEvent() {
        data object Init : Input()

        data class PutToStorage(
            val key: String,
            /** Retriever for value in storage, should be called in Performer */
            val retriever: () -> Flow<Any>,
            /** Persists value to storage, should eb called in Performer */
            val persister: (Any) -> Unit,
            val persistClearing: Boolean = false,
        ) : Input()

        data class PutToState(
            val key: String,
            /** Provides value for flow storage cache */
            val valueProvider: (Any?) -> Any,
            val persistClearing: Boolean = false,
        ) : Input()

        data class PutToStateIfPresent(
            val key: String,
            val valueProvider: (Any) -> Any,
            val persistClearing: Boolean = false,
        ) : Input()

        data class PutToStorageIfPresent(
            val key: String,
            val retriever: () -> Flow<Any>,
            val persister: (Any) -> Unit,
            val persistClearing: Boolean = false,
        ) : Input()

        data class Remove(val key: String) : Input()

        data class Peek(val key: String) : Input()

        data class CheckContains(val key: String) : Input()

        data object Clear : Input()

        data object ClearAll : Input()
    }

    sealed class Result : FlowCacheEvent() {
        data class ElementChanged(val key: String, val value: Any?) : Result()
    }
}

sealed class FlowCacheEffect {
    data class ElementChanged<ValueType>(val key: String, val value: ValueType?) : FlowCacheEffect()
    data class ContainsResult(val key: String, val doesContain: Boolean) : FlowCacheEffect()
    data class ElementReturned(val key: String, val value: Any?) : FlowCacheEffect()
}

sealed class FlowCacheInstruction {
    data class Persist(val key: String, val persister: (String) -> Unit) : FlowCacheInstruction()
    data class Retrieve<ValueType : Any>(val key: String, val retriever: (String) -> Flow<ValueType>) : FlowCacheInstruction()
}