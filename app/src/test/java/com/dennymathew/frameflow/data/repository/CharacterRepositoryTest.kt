package com.dennymathew.frameflow.data.repository

import androidx.paging.testing.asSnapshot
import com.dennymathew.frameflow.data.local.CharacterEntity
import com.dennymathew.frameflow.data.local.ImageDatabase
import com.dennymathew.frameflow.testutil.FakeRickAndMortyApi
import com.dennymathew.frameflow.testutil.characterDto
import com.dennymathew.frameflow.testutil.httpError
import com.dennymathew.frameflow.testutil.inMemoryDatabase
import com.dennymathew.frameflow.testutil.pageResponse
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.HttpException
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class CharacterRepositoryTest {

    private lateinit var database: ImageDatabase
    private val api = FakeRickAndMortyApi()
    private lateinit var repository: CharacterRepository

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        repository = CharacterRepository(api, database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getCharacterById_prefersCachedCharacter() = runTest {
        database.characterDao.upsertAll(listOf(entity(7, "Cached")))

        val result = repository.getCharacterById(7)

        assertEquals("Cached", result?.name)
        assertTrue(api.characterByIdRequests.isEmpty())
    }

    @Test
    fun getCharacterById_fallsBackToNetwork() = runTest {
        api.onGetCharacterById = { characterDto(it, name = "Remote") }

        val result = repository.getCharacterById(7)

        assertEquals("Remote", result?.name)
        assertEquals("https://example.com/7.jpeg", result?.imageUrl)
        assertEquals(listOf(7), api.characterByIdRequests)
    }

    @Test
    fun getCharacterById_returnsNullWhenOfflineAndUncached() = runTest {
        api.onGetCharacterById = { throw IOException("offline") }

        assertNull(repository.getCharacterById(7))
    }

    @Test
    fun syncSearch_blankQuery_makesNoRequest() = runTest {
        repository.syncSearchCharacters("  ")

        assertTrue(api.characterPageRequests.isEmpty())
    }

    @Test
    fun syncSearch_followsPagesUntilNoNext() = runTest {
        api.onGetCharacters = { page, _ ->
            when (page) {
                1 -> pageResponse(1..20, hasNext = true)
                else -> pageResponse(21..25, hasNext = false)
            }
        }

        repository.syncSearchCharacters("rick")

        assertEquals(listOf(1 to "rick", 2 to "rick"), api.characterPageRequests)
        assertEquals("Character 25", database.characterDao.getById(25)?.name)
    }

    @Test
    fun syncSearch_stopsAfterThreePages() = runTest {
        api.onGetCharacters = { page, _ ->
            pageResponse((page - 1) * 20 + 1..page * 20, hasNext = true)
        }

        repository.syncSearchCharacters("rick")

        assertEquals(listOf(1, 2, 3), api.characterPageRequests.map { it.first })
    }

    @Test
    fun syncSearch_noMatches_isNotAnError() = runTest {
        api.onGetCharacters = { _, _ -> throw httpError(404) }

        repository.syncSearchCharacters("nobody")

        assertEquals(1, api.characterPageRequests.size)
    }

    @Test(expected = HttpException::class)
    fun syncSearch_serverError_propagates() = runTest {
        api.onGetCharacters = { _, _ -> throw httpError(500) }

        repository.syncSearchCharacters("rick")
    }

    @Test
    fun searchCharacters_matchesCachedNamesInIdOrder() = runTest {
        database.characterDao.upsertAll(
            listOf(entity(3, "Morty Smith"), entity(1, "Rick Sanchez"), entity(2, "Evil Morty"))
        )

        val names = repository.searchCharacters("morty").asSnapshot().map { it.name }

        assertEquals(listOf("Evil Morty", "Morty Smith"), names)
    }

    private fun entity(id: Int, name: String) = CharacterEntity(
        id = id,
        name = name,
        status = "Alive",
        species = "Human",
        imageUrl = "https://example.com/$id.jpeg"
    )
}
