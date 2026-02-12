package com.cerebus.readwrite

import android.app.Application
import com.cerebus.readwrite.di.initKoin
import com.cerebus.readwrite.di.modules

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(modules)
    }

    companion object Companion {
        lateinit var instance: MyApp
            private set
    }

    init {
        instance = this
    }
}