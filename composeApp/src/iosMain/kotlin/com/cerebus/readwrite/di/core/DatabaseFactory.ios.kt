package com.cerebus.readwrite.di.core

import androidx.room.RoomDatabase
import com.cerebus.data.database.getDatabaseBuilder
import com.cerebus.readwrite.data.ReadWriteDatabase

actual fun provideDatabaseBuilder(): RoomDatabase.Builder<ReadWriteDatabase> {
    return getDatabaseBuilder()
}
