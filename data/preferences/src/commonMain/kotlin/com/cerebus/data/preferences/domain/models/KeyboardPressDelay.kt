package com.cerebus.data.preferences.domain.models

enum class KeyboardPressDelay(
    val storageValue: String,
    val intervalMs: Long,
) {
    Fast("fast", 140L),
    Normal("normal", 200L),
    Slow("slow", 280L);

    companion object {
        fun fromStorageValue(value: String?): KeyboardPressDelay? {
            return entries.firstOrNull { it.storageValue == value }
        }
    }
}
