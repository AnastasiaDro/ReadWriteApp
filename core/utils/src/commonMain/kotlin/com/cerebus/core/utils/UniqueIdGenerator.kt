package com.cerebus.core.utils

import kotlin.random.Random

private const val DEFAULT_RANDOM_SIZE = 24
private const val ALPHANUMERIC = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

object UniqueIdGenerator {
    fun randomAlphanumeric(
        prefix: String = "loc",
        size: Int = DEFAULT_RANDOM_SIZE,
    ): String {
        val randomPart = buildString(size) {
            repeat(size) {
                append(ALPHANUMERIC[Random.nextInt(ALPHANUMERIC.length)])
            }
        }
        return "${prefix}_$randomPart"
    }
}
