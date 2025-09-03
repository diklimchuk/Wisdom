package com.vkontakte.wisdom.memory

import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.collections.ConcurrentMutableSet
import com.vkontakte.wisdom.old.Option
import money.vivid.elmslie.core.plot.ElmScheme
import money.vivid.elmslie.core.plot.dsl.SchemePartBuilder

object MemoryScheme : ElmScheme<MemoryState, MemoryEvent.Input, MemoryEffect, MemoryInstruction>() {
    override fun SchemePartBuilder<MemoryState, MemoryEffect, MemoryInstruction>.reduce(event: MemoryEvent.Input): Unit = when (event) {
        is MemoryEvent.Input.Init -> {
            nullableState { MemoryState(clearableKeys = ConcurrentMutableSet(), elements = ConcurrentMutableMap()) }
        }

        is MemoryEvent.Input.Put<*> -> {
            if (event.persistClearing) {
                state.clearableKeys.remove(event.key)
            } else {
                state.clearableKeys.add(event.key)
            }
            val newValue = (event.valueProvider as (Any?) -> Any)(state.elements[event.key]?.value)
            @Suppress("UNCHECKED_CAST")
            state.elements[event.key] = Option(newValue)
            effects { +MemoryEffect.ElementChanged(event.key, newValue) }
        }

        is MemoryEvent.Input.PutIfPresent<*> -> {
            if (event.persistClearing) {
                state.clearableKeys.remove(event.key)
            } else {
                state.clearableKeys.add(event.key)
            }
            val newValue = state.elements[event.key]?.value?.let { (event.valueProvider as (Any) -> Any)(it) }
            @Suppress("UNCHECKED_CAST")
            state.elements[event.key] = Option(newValue)
            effects { +MemoryEffect.ElementChanged(event.key, newValue) }
        }

        is MemoryEvent.Input.CheckContains -> {
            effects { +MemoryEffect.ContainsResult(key = event.key, doesContain = state.elements.contains(event.key)) }
        }

        is MemoryEvent.Input.Clear -> {
            state.clearableKeys.forEach { key ->
                effects { +MemoryEffect.ElementChanged(key, state.elements[key]) }
                state.elements.remove(key)
            }
        }

        is MemoryEvent.Input.ClearAll -> {
            state.elements.forEach {
                effects { +MemoryEffect.ElementChanged(it.key, it.value) }
            }
            state.elements.clear()
        }

        is MemoryEvent.Input.Peek -> {
            effects { +MemoryEffect.ElementReturned(event.key, state.elements[event.key]?.value) }
        }

        is MemoryEvent.Input.Remove -> {
            state.elements.remove(event.key)
            effects { +MemoryEffect.ElementChanged(event.key, null) }
        }
    }
}