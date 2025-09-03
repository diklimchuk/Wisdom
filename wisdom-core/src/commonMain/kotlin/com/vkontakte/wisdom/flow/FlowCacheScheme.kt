package com.vkontakte.wisdom.flow

import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.collections.ConcurrentMutableSet
import com.vkontakte.wisdom.old.Option
import money.vivid.elmslie.core.plot.ElmScheme
import money.vivid.elmslie.core.plot.dsl.SchemePartBuilder

object FlowCacheScheme : ElmScheme<FlowCacheState, FlowCacheEvent.Input, FlowCacheEffect, FlowCacheInstruction>() {
    override fun SchemePartBuilder<FlowCacheState, FlowCacheEffect, FlowCacheInstruction>.reduce(event: FlowCacheEvent.Input): Unit = when (event) {
        is FlowCacheEvent.Input.Init -> {
            nullableState {
                FlowCacheState(
                    clearableKeys = ConcurrentMutableSet(),
                    elements = ConcurrentMutableMap(),
                    elementAccessors = ConcurrentMutableMap(),
                )
            }
        }

        is FlowCacheEvent.Input.Put -> {
            if (event.persistClearing) {
                state.clearableKeys.remove(event.key)
            } else {
                state.clearableKeys.add(event.key)
            }
            val newValue = (event.valueProvider as (Any?) -> Any)(state.elements[event.key]?.value)
            @Suppress("UNCHECKED_CAST")
            state.elements[event.key] = Option(newValue)
            effects { +FlowCacheEffect.ElementChanged(event.key, newValue) }
        }

        is FlowCacheEvent.Input.PutIfPresent -> {
            if (event.persistClearing) {
                state.clearableKeys.remove(event.key)
            } else {
                state.clearableKeys.add(event.key)
            }
            val newValue = state.elements[event.key]?.value?.let { (event.valueProvider as (Any) -> Any)(it) }
            @Suppress("UNCHECKED_CAST")
            state.elements[event.key] = Option(newValue)
            effects { +FlowCacheEffect.ElementChanged(event.key, newValue) }
        }

        is FlowCacheEvent.Input.CheckContains -> {
            effects { +FlowCacheEffect.ContainsResult(key = event.key, doesContain = state.elements.contains(event.key)) }
        }

        is FlowCacheEvent.Input.Clear -> {
            state.clearableKeys.forEach { key ->
                effects { +FlowCacheEffect.ElementChanged(key, state.elements[key]) }
                state.elements.remove(key)
            }
        }

        is FlowCacheEvent.Input.ClearAll -> {
            state.elements.forEach {
                effects { +FlowCacheEffect.ElementChanged(it.key, it.value) }
            }
            state.elements.clear()
        }

        is FlowCacheEvent.Input.Peek -> {
            effects { +FlowCacheEffect.ElementReturned(event.key, state.elements[event.key]?.value) }
        }

        is FlowCacheEvent.Input.Remove -> {
            state.elements.remove(event.key)
            effects { +FlowCacheEffect.ElementChanged(event.key, null) }
        }
    }
}