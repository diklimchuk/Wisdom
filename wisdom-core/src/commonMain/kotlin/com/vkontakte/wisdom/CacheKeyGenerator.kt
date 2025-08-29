package com.vkontakte.wisdom

import kotlinx.serialization.json.Json
import kotlin.reflect.typeOf

/**
 * Generates unique keys for cache handles.
 * Uses combination of information about Args and Result
 */
inline fun <reified Args : Any, reified Result> generateCacheKey(args: Args): String {
    val key = typeOf<Result>().toString()
    val className = (args::class).qualifiedName ?: error("Can't get class name of $args")
    val serializedArgs = Json.encodeToString(args)
    return "$key$serializedArgs$className"
}
