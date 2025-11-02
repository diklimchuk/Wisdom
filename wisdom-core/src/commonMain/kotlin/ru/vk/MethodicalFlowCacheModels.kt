package ru.vk.flow.methodical

import co.touchlab.stately.collections.ConcurrentMutableSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import money.vivid.elmslie.core.plot.dsl.SchemePartBuilder

data class MethodicalFlowCacheState(
    val clearableKeys: ConcurrentMutableSet<String>,
)

// TODO: Refactoring to using
// TODO: Make it nested in Event
private typealias Builder<ValueType> = SchemePartBuilder<MethodicalFlowCacheState, MethodicalFlowCacheEffect, MethodicalFlowCacheOperation<ValueType>>
private typealias State = MethodicalFlowCacheState
private typealias Effect = MethodicalFlowCacheEffect
private typealias Operation<ValueType> = MethodicalFlowCacheOperation<ValueType>
private typealias Resources<ValueType> = MethodicalFlowCacheResources<ValueType>

sealed class MethodicalFlowCacheEvent<ValueType : Any> :
        (SchemePartBuilder<State, Effect, Operation<ValueType>>) -> SchemePartBuilder<State, Effect, Operation<ValueType>> {

    sealed class Input<ValueType : Any> : MethodicalFlowCacheEvent<ValueType>() {
        class Init<ValueType : Any> : Input<ValueType>() {
            override fun invoke(
                builder: Builder<ValueType>
            ): Builder<ValueType> {
                return builder.nullableState {
                    MethodicalFlowCacheState(
                        clearableKeys = ConcurrentMutableSet(),
                    )
                }
            }
        }

        data class PutFlowToStorage<ValueType : Any>(
            val key: String,
            val valueFlowProvider: () -> Flow<ValueType>,
            val persistClearing: Boolean = false,
        ) : Input<ValueType>() {
            override fun invoke(
                builder: Builder<ValueType>
            ): Builder<ValueType> = builder.apply {
                if (persistClearing) {
                    state.clearableKeys.remove(key)
                } else {
                    state.clearableKeys.add(key)
                }
                operations {
                    +MethodicalFlowCacheOperation.PersistFlow(key, valueFlowProvider)
                }
            }
        }

        data class PutToStorage<ValueType : Any>(
            val key: String,
            val valueProvider: (ValueType?) -> ValueType,
            val persistClearing: Boolean = false,
        ) : Input<ValueType>() {
            override fun invoke(
                builder: Builder<ValueType>
            ): Builder<ValueType> = builder.apply {
                if (persistClearing) {
                    state.clearableKeys.remove(key)
                } else {
                    state.clearableKeys.add(key)
                }
                operations {
                    +MethodicalFlowCacheOperation.Persist(key, valueProvider)
                }
            }
        }

        data class PutToStorageIfPresent<ValueType : Any>(
            val key: String,
            val valueProvider: (ValueType) -> ValueType,
            val persistClearing: Boolean = false,
        ) : Input<ValueType>() {
            override fun invoke(
                builder: Builder<ValueType>
            ): Builder<ValueType> = builder.apply {
                if (persistClearing) {
                    state.clearableKeys.remove(key)
                } else {
                    state.clearableKeys.add(key)
                }
                operations {
                    +MethodicalFlowCacheOperation.PersistIfPresent(key, valueProvider)
                }
            }
        }

        data class Remove<ValueType : Any>(val key: String) : Input<ValueType>() {
            override fun invoke(
                builder: Builder<ValueType>
            ): Builder<ValueType> = builder.apply {
                state.clearableKeys.remove(key)
                operations {
                    +MethodicalFlowCacheOperation.Persist<ValueType>(key, { null })
                }
            }
        }

        data class CheckContains<ValueType : Any>(
            val key: String
        ) : Input<ValueType>() {
            override fun invoke(
                builder: Builder<ValueType>
            ): Builder<ValueType> = builder.apply {
                operations {
                    +MethodicalFlowCacheOperation.CheckContains<ValueType>(key)
                }
            }
        }

        class Clear<ValueType : Any> : Input<ValueType>() {
            override fun invoke(
                builder: Builder<ValueType>
            ): Builder<ValueType> = builder.apply {
                operations {
                    +MethodicalFlowCacheOperation.Clear<ValueType>(state.clearableKeys)
                }
            }
        }

        class ClearAll<ValueType : Any> : Input<ValueType>() {
            override fun invoke(
                builder: Builder<ValueType>
            ): Builder<ValueType> = builder.apply {
                operations {
                    +MethodicalFlowCacheOperation.ClearAll<ValueType>()
                }
            }
        }
    }

    sealed class Result<ValueType : Any> : MethodicalFlowCacheEvent<ValueType>() {

        class ElementChanged<ValueType : Any>(val key: String, val value: ValueType?) : Result<ValueType>() {
            override fun invoke(
                builder: Builder<ValueType>
            ): Builder<ValueType> {
                return builder.apply {
                    effects { +MethodicalFlowCacheEffect.ElementChanged(key, value) }
                }
            }
        }

        class ContainsResult<ValueType : Any>(
            private val key: String,
            private val containsResult: Boolean,
        ) : Result<ValueType>() {
            override fun invoke(
                builder: Builder<ValueType>
            ): Builder<ValueType> {
                return builder.apply {
                    effects { MethodicalFlowCacheEffect.ContainsResult(key, containsResult) }
                }
            }
        }

        class Cleared<ValueType : Any> : Result<ValueType>() {
            override fun invoke(
                builder: Builder<ValueType>
            ): Builder<ValueType> {
                return builder.apply {
                    effects { MethodicalFlowCacheEffect.Cleared }
                }
            }
        }

        class ClearedAll<ValueType : Any> : Result<ValueType>() {
            override fun invoke(
                builder: Builder<ValueType>
            ): Builder<ValueType> {
                return builder.apply {
                    effects { MethodicalFlowCacheEffect.ClearedAll }
                }
            }
        }
    }
}

// TODO: Think about solving generics
sealed class MethodicalFlowCacheEffect {
    data class ElementChanged<ValueType>(val key: String, val value: ValueType?) :
        MethodicalFlowCacheEffect()

    data class ContainsResult(val key: String, val doesContain: Boolean) :
        MethodicalFlowCacheEffect()

    data class ElementReturned(val key: String, val value: Any?) : MethodicalFlowCacheEffect()

    data object Cleared : MethodicalFlowCacheEffect()

    data object ClearedAll : MethodicalFlowCacheEffect()
}

sealed class MethodicalFlowCacheOperation<ValueType : Any> :
        (MethodicalFlowCacheResources<ValueType>) -> Flow<MethodicalFlowCacheEvent<ValueType>> {

    class Clear<ValueType : Any>(
        private val keys: Set<String>,
    ) : MethodicalFlowCacheOperation<ValueType>() {
        override fun invoke(resources: MethodicalFlowCacheResources<ValueType>): Flow<MethodicalFlowCacheEvent<ValueType>> {
            return flow {
                resources.storage.clear(keys)
                emit(MethodicalFlowCacheEvent.Result.Cleared())
            }
        }
    }

    class ClearAll<ValueType : Any>() : MethodicalFlowCacheOperation<ValueType>() {
        override fun invoke(resources: MethodicalFlowCacheResources<ValueType>): Flow<MethodicalFlowCacheEvent<ValueType>> {
            return flow {
                resources.storage.clearAll()
                emit(MethodicalFlowCacheEvent.Result.ClearedAll())
            }
        }
    }

    class CheckContains<ValueType : Any>(
        val key: String,
    ) : MethodicalFlowCacheOperation<ValueType>() {
        override fun invoke(resources: MethodicalFlowCacheResources<ValueType>): Flow<MethodicalFlowCacheEvent<ValueType>> {
            return flow {
                val containsCheckResult = resources.storage.contains(key)
                emit(MethodicalFlowCacheEvent.Result.ContainsResult(key, containsCheckResult))
            }
        }
    }

    class PersistIfPresent<ValueType : Any>(
        val key: String,
        val valueProvider: (ValueType) -> ValueType?,
    ) : MethodicalFlowCacheOperation<ValueType>() {
        override fun invoke(resources: MethodicalFlowCacheResources<ValueType>): Flow<MethodicalFlowCacheEvent<ValueType>> {
            return flow {
                val oldValue = resources.storage.peek(key) ?: return@flow
                val newValue = valueProvider(oldValue)
                resources.storage.persist(key, newValue)
                emit(MethodicalFlowCacheEvent.Result.ElementChanged(key, newValue))
            }
        }
    }

    class Persist<ValueType : Any>(
        val key: String,
        val valueProvider: (ValueType?) -> ValueType?,
    ) : MethodicalFlowCacheOperation<ValueType>() {
        override fun invoke(resources: MethodicalFlowCacheResources<ValueType>): Flow<MethodicalFlowCacheEvent<ValueType>> {
            return flow {
                val oldValue = resources.storage.peek(key)
                val newValue = valueProvider(oldValue)
                resources.storage.persist(key, newValue)
                emit(MethodicalFlowCacheEvent.Result.ElementChanged(key, newValue))
            }
        }
    }

    class PersistFlow<ValueType : Any>(
        val key: String,
        val valueFlowProvider: () -> Flow<ValueType?>,
    ) : MethodicalFlowCacheOperation<ValueType>() {
        override fun invoke(resources: MethodicalFlowCacheResources<ValueType>): Flow<MethodicalFlowCacheEvent<ValueType>> {
            return valueFlowProvider()
                .onEach { newValue -> resources.storage.persist(key, newValue) }
                .map { newValue -> MethodicalFlowCacheEvent.Result.ElementChanged(key, newValue) }
        }
    }

    data class Retrieve<ValueType : Any>(
        val key: String,
    ) : MethodicalFlowCacheOperation<ValueType>() {
        override fun invoke(resources: MethodicalFlowCacheResources<ValueType>): Flow<MethodicalFlowCacheEvent<ValueType>> {
            return resources.storage.retrieve(key)
                .map { MethodicalFlowCacheEvent.Result.ElementChanged(key, it) }
        }
    }
}