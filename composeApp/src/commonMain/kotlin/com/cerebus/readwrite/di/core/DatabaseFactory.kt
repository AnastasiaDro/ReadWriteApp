package com.cerebus.readwrite.di.core

import androidx.room.RoomDatabase
import com.cerebus.database.getRoomDatabase
import com.cerebus.readwrite.data.ReadWriteDatabase

expect fun provideDatabaseBuilder(): RoomDatabase.Builder<ReadWriteDatabase>

fun createDatabase(): ReadWriteDatabase {
    return getRoomDatabase(provideDatabaseBuilder())
}
