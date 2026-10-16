package com.dennymathew.frameflow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CharacterEntity::class, RemoteKeysEntity::class, FavoriteEntity::class],
    version = 3,
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

        /** Adds type, gender, origin, location and episode count to characters and favorites. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                for (table in listOf("characters", "favorites")) {
                    for (column in listOf("type", "gender", "originName", "locationName")) {
                        db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` TEXT NOT NULL DEFAULT ''")
                    }
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `episodeCount` INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
    }
}
