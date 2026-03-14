package com.cerebus.core.utils

import kotlinx.cinterop.alloc
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitNanosecond
import platform.Foundation.NSCalendarUnitSecond
import platform.posix.gettimeofday
import platform.posix.timeval

@OptIn(ExperimentalForeignApi::class)
actual fun nowMillis(): Long = memScoped {
    val tv = alloc<timeval>()
    gettimeofday(tv.ptr, null)
    (tv.tv_sec * 1000L) + (tv.tv_usec / 1000L)
}

actual fun localStartOfDayMillis(): Long {
    val now = nowMillis()
    val components = NSCalendar.currentCalendar.components(
        NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond or NSCalendarUnitNanosecond,
        fromDate = NSDate(),
    )
    val millisSinceStartOfDay = (components.hour.toLong() * 60L * 60L * 1000L) +
        (components.minute.toLong() * 60L * 1000L) +
        (components.second.toLong() * 1000L) +
        (components.nanosecond.toLong() / 1_000_000L)
    return now - millisSinceStartOfDay
}
