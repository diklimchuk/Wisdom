package ru.vk

fun <T, U> T.unsafeAs(): U {
    @Suppress("UNCHECKED_CAST")
    return this as U
}