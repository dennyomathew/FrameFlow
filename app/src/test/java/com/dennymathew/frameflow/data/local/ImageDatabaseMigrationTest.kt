package com.dennymathew.frameflow.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
