package com.vkontakte.wisdom.memory

import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.collections.ConcurrentMutableSet
import com.vkontakte.wisdom.old.Option

data class MemoryState(
    val clearableKeys: ConcurrentMutableSet<String>,
    val elements: ConcurrentMutableMap<String, Option<Any>>
)

sealed class MemoryEvent {
    sealed class Input : MemoryEvent() {
        data object Init : Input()

        data class Put<ValueType : Any>(
            val key: String,
            val valueProvider: (previousValue: ValueType?) -> ValueType,
            val persistClearing: Boolean = false,
        ) : Input()

        data class PutIfPresent<ValueType : Any>(
            val key: String,
            val valueProvider: (previousValue: ValueType) -> ValueType,
            val persistClearing: Boolean = false,
        ) : Input()

        data class Remove(val key: String) : Input()

        data class Peek(val key: String) : Input()

        data class CheckContains(val key: String) : Input()

        data object Clear : Input()

        data object ClearAll : Input()
    }
}

sealed class MemoryEffect {
    data class ElementChanged<ValueType>(val key: String, val value: ValueType?) : MemoryEffect()
    data class ContainsResult(val key: String, val doesContain: Boolean) : MemoryEffect()
    data class ElementReturned(val key: String, val value: Any?) : MemoryEffect()
}

sealed class MemoryInstruction