package com.dennymathew.frameflow.data.repository

import com.dennymathew.frameflow.data.local.CharacterEntity
import com.dennymathew.frameflow.data.local.ImageDatabase
import com.dennymathew.frameflow.testutil.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavoritesRepositoryTest {

    private lateinit var database: ImageDatabase
    private var nowMillis = 1_000L
    private lateinit var repository: FavoritesRepository

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        repository = FavoritesRepository(database, now = { nowMillis++ })
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun toggle_addsThenRemovesFavorite() = runTest {
        repository.toggle(character(1))
        assertTrue(repository.isFavorite(1).first())
        assertEquals(setOf(1), repository.favoriteIds.first())

        repository.toggle(character(1))
        assertFalse(repository.isFavorite(1).first())
        assertTrue(repository.favorites.first().isEmpty())
    }

    @Test
    fun favorites_listMostRecentlySavedFirst() = runTest {
        repository.toggle(character(1))
        repository.toggle(character(2))
        repository.toggle(character(3))

        assertEquals(listOf(3, 2, 1), repository.favorites.first().map { it.characterId })
    }

    @Test
    fun favorites_keepCharacterDetails() = runTest {
        repository.toggle(character(7, name = "Birdperson"))

        val saved = repository.favorites.first().single()
        assertEquals("Birdperson", saved.name)
        assertEquals("https://example.com/7.jpeg", saved.imageUrl)
    }

    @Test
    fun favorites_surviveCharacterCacheBeingCleared() = runTest {
        database.characterDao.upsertAll(listOf(character(1)))
        repository.toggle(character(1))

        // What a pull-to-refresh does to the cache.
        database.characterDao.clearAll()

        assertEquals(listOf(1), repository.favorites.first().map { it.characterId })
    }

    private fun character(id: Int, name: String = "Character $id") = CharacterEntity(
        id = id,
        name = name,
        status = "Alive",
        species = "Human",
        imageUrl = "https://example.com/$id.jpeg"
    )
}
