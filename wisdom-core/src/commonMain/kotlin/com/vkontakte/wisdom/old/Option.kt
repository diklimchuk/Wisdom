package com.vkontakte.wisdom.old

data class Option<T : Any>(
    val value: T?
) {

    val isEmpty = value == null
    val hasValue = !isEmpty

    companion object {
        fun <T : Any> none() = Option<T>(null)
    }
}