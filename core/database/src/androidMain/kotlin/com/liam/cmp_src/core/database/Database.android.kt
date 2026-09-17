package com.liam.cmp_src.core.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/**
 * Android's half of the database wiring: resolves [DATABASE_NAME] under the app's private
 * database directory and picks the bundled SQLite driver, which is a platform choice rather
 * than a common one (see [getRoomDatabase]).
 *
 * Takes the application context, never an `Activity` — this builder outlives any screen.
 */
fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath(DATABASE_NAME)
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    ).setDriver(BundledSQLiteDriver())
}
