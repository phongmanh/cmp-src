package com.liam.cmp_src.core.database

import androidx.room3.migration.Migration
import androidx.sqlite.execSQL

/**
 * Adds the `customers` table.
 *
 * Version 2 held nothing but the single encrypted session row, so there is no data to move: the
 * statement below is the one Room exports as this table's `createSql`, copied verbatim so that the
 * table a migrated database gets is byte-for-byte the table a fresh install gets. Room compares the
 * two on open, and any drift here surfaces as a failed migration rather than a subtly wrong table.
 *
 * Keep it in step with [com.liam.cmp_src.core.database.entities.Customer]: a column added to the
 * entity needs its own migration, never an edit to this one, because a device that has already run
 * this migration will never run it again.
 */
internal val MIGRATION_2_3 = Migration(startVersion = 2, endVersion = 3) { connection ->
    connection.execSQL(
        "CREATE TABLE IF NOT EXISTS `customers` (" +
            "`id` TEXT NOT NULL, " +
            "`ownerId` TEXT NOT NULL, " +
            "`firstName` TEXT, " +
            "`lastName` TEXT, " +
            "`companyName` TEXT, " +
            "`email` TEXT, " +
            "`phone` TEXT, " +
            "`addressLine1` TEXT, " +
            "`addressLine2` TEXT, " +
            "`city` TEXT, " +
            "`region` TEXT, " +
            "`postalCode` TEXT, " +
            "`countryCode` TEXT, " +
            "`notes` TEXT, " +
            "`status` TEXT NOT NULL, " +
            "`createdAt` TEXT NOT NULL, " +
            "`updatedAt` TEXT NOT NULL, " +
            "`deletedAt` TEXT, " +
            "PRIMARY KEY(`id`)" +
            ")",
    )
}

/**
 * Gives `customers` the two columns offline editing needs: `remoteId`, the server's id for a row,
 * and `syncState`, whether the server has heard about the row's latest change.
 *
 * Nothing wrote the table before this version, so any row already there can only have come from
 * the server: it is already synced, and its `id` is the server's. The `DEFAULT` is the one Room
 * records for the entity's `@ColumnInfo(defaultValue = ...)` — the two have to agree or the
 * post-migration schema check fails.
 */
internal val MIGRATION_3_4 = Migration(startVersion = 3, endVersion = 4) { connection ->
    connection.execSQL("ALTER TABLE `customers` ADD COLUMN `remoteId` TEXT")
    connection.execSQL(
        "ALTER TABLE `customers` ADD COLUMN `syncState` TEXT NOT NULL DEFAULT 'SYNCED'",
    )
    connection.execSQL("UPDATE `customers` SET `remoteId` = `id`")
}

/**
 * The migration chain [getRoomDatabase] installs, ordered oldest first.
 *
 * Every schema change from version 2 onwards belongs here. A bump to [DATABASE_VERSION] without a
 * matching entry fails at open time instead of deleting the file, which is the whole point of
 * dropping the blanket destructive fallback — see [getRoomDatabase].
 *
 * Declared below the migrations it holds: top-level properties initialize in file order, so an
 * entry declared later than this array would read as `null` at startup.
 */
internal val APP_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_2_3, MIGRATION_3_4)
