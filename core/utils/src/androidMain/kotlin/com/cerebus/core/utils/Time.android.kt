package com.cerebus.core.utils

import java.util.Calendar

actual fun nowMillis(): Long = System.currentTimeMillis()

actual fun localStartOfDayMillis(): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
