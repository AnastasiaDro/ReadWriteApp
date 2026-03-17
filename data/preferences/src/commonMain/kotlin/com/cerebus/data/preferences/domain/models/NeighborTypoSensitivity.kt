package com.cerebus.data.preferences.domain.models

enum class NeighborTypoSensitivity(
    val storageValue: String,
) {
    Strict("strict"),
    Normal("normal"),
    Soft("soft");

    companion object {
        fun fromStorageValue(value: String?): NeighborTypoSensitivity? {
            return entries.firstOrNull { it.storageValue == value }
        }
    }
}
