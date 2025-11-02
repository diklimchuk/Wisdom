package ru.vk.flow

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import money.vivid.elmslie.core.plot.Performer

class FlowCachePerformer : Performer<FlowCacheInstruction, FlowCacheEvent.Result>() {
    override fun execute(instruction: FlowCacheInstruction): Flow<FlowCacheEvent.Result> = when (instruction) {
        is FlowCacheInstruction.Persist -> flow { instruction.persister(instruction.key) }
        is FlowCacheInstruction.Retrieve<*> -> instruction.retriever(instruction.key)
            .mapEvents(onResult = { FlowCacheEvent.Result.ElementChanged(instruction.key, it) })
    }
}