package org.example.lang.values

sealed interface AValue<out T> {
    data class Const<T>(val value: T) : AValue<T>
    object Unknown : AValue<Nothing>
}