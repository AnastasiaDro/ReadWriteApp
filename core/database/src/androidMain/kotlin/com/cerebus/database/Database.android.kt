package com.cerebus.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

inline fun <reified T : RoomDatabase> getDatabaseBuilder(
    context: Context,
    name: String = "readwriteapp.db"
): RoomDatabase.Builder<T> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath(name)
    return Room.databaseBuilder<T>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}
