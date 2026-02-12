package com.cerebus.readwrite

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform