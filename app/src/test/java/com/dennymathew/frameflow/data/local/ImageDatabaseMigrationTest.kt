package com.dennymathew.frameflow.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Every schema version must upgrade without losing data; schemas live in `app/schemas`. */
@RunWith(RobolectricTestRunner::class)
class ImageDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ImageDatabase::class.java
    )

    @Test
    fun migrate1To2_keepsCharactersAndAddsFavorites() = runTest {
        helper.createDatabase(DB_NAME, 1).apply {
            execSQL(
                "INSERT INTO characters (id, name, status, species, imageUrl) " +
                    "VALUES (1, 'Rick Sanchez', 'Alive', 'Human', 'https://example.com/1.jpeg')"
            )
            close()
        }

        // Validates the migrated schema against 2.json.
        helper.runMigrationsAndValidate(DB_NAME, 2, true, ImageDatabase.MIGRATION_1_2).close()

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ImageDatabase::class.java,
            DB_NAME
        ).addMigrations(*ImageDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals("Rick Sanchez", database.characterDao.getById(1)?.name)
            assertEquals(emptyList<FavoriteEntity>(), database.favoriteDao.observeAll().first())
        } finally {
            database.close()
        }
    }

    @Test
    fun migrate2To3_keepsRowsAndAddsEmptyDetails() = runTest {
        helper.createDatabase(DB_NAME, 2).apply {
            execSQL(
                "INSERT INTO characters (id, name, status, species, imageUrl) " +
                    "VALUES (1, 'Rick Sanchez', 'Alive', 'Human', 'https://example.com/1.jpeg')"
            )
            execSQL(
                "INSERT INTO favorites (characterId, name, status, species, imageUrl, addedAtMillis) " +
                    "VALUES (2, 'Morty Smith', 'Alive', 'Human', 'https://example.com/2.jpeg', 42)"
            )
            close()
        }

        // Validates the migrated schema against 3.json.
        helper.runMigrationsAndValidate(DB_NAME, 3, true, ImageDatabase.MIGRATION_2_3).close()

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ImageDatabase::class.java,
            DB_NAME
        ).addMigrations(*ImageDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            val rick = database.characterDao.getById(1)
            assertEquals("Rick Sanchez", rick?.name)
            assertEquals("", rick?.gender)
            assertEquals(0, rick?.episodeCount)
            assertFalse(rick!!.hasDetails)

            val morty = database.favoriteDao.observeAll().first().single()
            assertEquals("Morty Smith", morty.name)
            assertEquals(42L, morty.addedAtMillis)
            assertEquals("", morty.originName)
        } finally {
            database.close()
        }
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
