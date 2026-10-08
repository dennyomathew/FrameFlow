package com.dennymathew.frameflow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CharacterEntity::class, RemoteKeysEntity::class, FavoriteEntity::class],
    version = 2,
    exportSchema = true
)
abstract class ImageDatabase : RoomDatabase() {
    abstract val characterDao: CharacterDao
    abstract val remoteKeysDao: RemoteKeysDao
    abstract val favoriteDao: FavoriteDao

    companion object {
        const val DATABASE_NAME = "frameflow_db"

        /** Adds favorites. Favorites are user data, so every schema change needs a migration. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favorites` (" +
                        "`characterId` INTEGER NOT NULL, `name` TEXT NOT NULL, " +
                        "`status` TEXT NOT NULL, `species` TEXT NOT NULL, " +
                        "`imageUrl` TEXT NOT NULL, `addedAtMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`characterId`))"
                )
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
    }
}
