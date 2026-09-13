package com.liam.cmp_src.core.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

internal const val APP_DIRECTORY_NAME = ".cmpsrc"

/**
 * Desktop's half of the database wiring. The JVM has no per-app sandbox, so the file goes in a
 * dot-directory under the user's home rather than the working directory, which changes with
 * however the app was launched.
 */
fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val appDirectory = File(System.getProperty("user.home"), APP_DIRECTORY_NAME)
    appDirectory.mkdirs()
    return Room.databaseBuilder<AppDatabase>(name = File(appDirectory, DATABASE_NAME).absolutePath)
        .setDriver(BundledSQLiteDriver())
}
