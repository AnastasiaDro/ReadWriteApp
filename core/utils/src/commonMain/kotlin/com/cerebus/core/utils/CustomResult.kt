package com.cerebus.core.utils

sealed interface CustomResult<out T> {
    data class Success<out T>(val data: T) : CustomResult<T>
    data class Failure(val error: Throwable) : CustomResult<Nothing>
}