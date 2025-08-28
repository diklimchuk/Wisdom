package com.vkontakte.wisdom

//import kotlinx.serialization.InternalSerializationApi
//import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

inline fun <reified T> cacheKeyGenerator(): CacheKeyGenerator {
    val key = typeOf<T>().toString()
    return CacheKeyGenerator(key)
}

/**
 * Generates unique keys for cache handles.
 * Uses combination of information about Args and Result
 */
class CacheKeyGenerator(
    private val customKey: String,
) {

    fun generatePrefix(): String = customKey

//    @OptIn(InternalSerializationApi::class)
    private fun kclassToString(): String {
//        resultToken.serializer()
        return ""
    }

    fun <Args : Any> generate(args: Args): String {
        val className = (args::class).qualifiedName ?: error("Can't get class name of $args")
        val serializedArgs = Json.toString(args)
        val prefix = generatePrefix()
//        return "$prefix$serializedArgs$className"
        return ""
    }
}
