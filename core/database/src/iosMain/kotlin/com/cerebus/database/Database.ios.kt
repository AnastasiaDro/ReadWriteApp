package com.cerebus.database

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

inline fun <reified T : RoomDatabase> getDatabaseBuilder(
    name: String = "readwriteapp.db"
): RoomDatabase.Builder<T> {
    val dbFilePath = documentDirectory() + "/" + name
    return Room.databaseBuilder<T>(
        name = dbFilePath,
    )
}

@PublishedApi
@OptIn(ExperimentalForeignApi::class)
internal fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}
