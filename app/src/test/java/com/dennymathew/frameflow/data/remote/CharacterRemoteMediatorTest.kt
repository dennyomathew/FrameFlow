package com.dennymathew.frameflow.data.remote

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator.MediatorResult
import com.dennymathew.frameflow.data.local.CharacterEntity
import com.dennymathew.frameflow.data.local.ImageDatabase
import com.dennymathew.frameflow.testutil.FakeRickAndMortyApi
import com.dennymathew.frameflow.testutil.httpError
import com.dennymathew.frameflow.testutil.inMemoryDatabase
import com.dennymathew.frameflow.testutil.pageResponse
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
@RunWith(RobolectricTestRunner::class)
class CharacterRemoteMediatorTest {

    private lateinit var database: ImageDatabase
    private val api = FakeRickAndMortyApi()
    private lateinit var mediator: CharacterRemoteMediator

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        mediator = CharacterRemoteMediator(api, database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun refresh_loadsFirstPageAndStoresKeys() = runTest {
        api.onGetCharacters = { _, _ -> pageResponse(1..20, hasNext = true) }

        val result = mediator.load(LoadType.REFRESH, pagingState())

        assertTrue(result is MediatorResult.Success)
        assertFalse((result as MediatorResult.Success).endOfPaginationReached)
        assertEquals(listOf(1 to null), api.characterPageRequests)
        assertEquals(20, characterCount())
        val keys = database.remoteKeysDao.getRemoteKeysForCharacterId(20)!!
        assertNull(keys.prevKey)
        assertEquals(2, keys.nextKey)
    }

    @Test
    fun refresh_withoutNextPage_reachesEndOfPagination() = runTest {
        api.onGetCharacters = { _, _ -> pageResponse(1..5, hasNext = false) }

        val result = mediator.load(LoadType.REFRESH, pagingState())

        assertTrue((result as MediatorResult.Success).endOfPaginationReached)
        assertNull(database.remoteKeysDao.getRemoteKeysForCharacterId(5)!!.nextKey)
    }

    @Test
    fun refresh_replacesPreviouslyCachedData() = runTest {
        api.onGetCharacters = { _, _ -> pageResponse(1..40, hasNext = true) }
        mediator.load(LoadType.REFRESH, pagingState())

        api.onGetCharacters = { _, _ -> pageResponse(1..20, hasNext = true) }
        mediator.load(LoadType.REFRESH, pagingState())

        assertEquals(20, characterCount())
        assertNull(database.remoteKeysDao.getRemoteKeysForCharacterId(40))
    }

    @Test
    fun append_requestsNextKeyOfLastLoadedItem() = runTest {
        api.onGetCharacters = { _, _ -> pageResponse(1..20, hasNext = true) }
        mediator.load(LoadType.REFRESH, pagingState())
        val loaded = database.characterDao.getById(20)!!

        api.onGetCharacters = { _, _ -> pageResponse(21..40, hasNext = true) }
        val result = mediator.load(LoadType.APPEND, pagingState(listOf(loaded)))

        assertFalse((result as MediatorResult.Success).endOfPaginationReached)
        assertEquals(2, api.characterPageRequests.last().first)
        assertEquals(40, characterCount())
        val keys = database.remoteKeysDao.getRemoteKeysForCharacterId(40)!!
        assertEquals(1, keys.prevKey)
        assertEquals(3, keys.nextKey)
    }

    @Test
    fun append_skipsEmptyTrailingPagesWhenFindingLastItem() = runTest {
        api.onGetCharacters = { _, _ -> pageResponse(1..20, hasNext = true) }
        mediator.load(LoadType.REFRESH, pagingState())
        val loaded = database.characterDao.getById(20)!!

        api.onGetCharacters = { _, _ -> pageResponse(21..40, hasNext = true) }
        mediator.load(LoadType.APPEND, pagingState(listOf(loaded), emptyList()))

        assertEquals(2, api.characterPageRequests.last().first)
    }

    @Test
    fun append_afterLastPage_reachesEndWithoutNetworkCall() = runTest {
        api.onGetCharacters = { _, _ -> pageResponse(1..5, hasNext = false) }
        mediator.load(LoadType.REFRESH, pagingState())
        val loaded = database.characterDao.getById(5)!!
        api.characterPageRequests.clear()

        val result = mediator.load(LoadType.APPEND, pagingState(listOf(loaded)))

        assertTrue((result as MediatorResult.Success).endOfPaginationReached)
        assertTrue(api.characterPageRequests.isEmpty())
    }

    @Test
    fun append_beforeAnyItemsLoaded_doesNotEndPagination() = runTest {
        val result = mediator.load(LoadType.APPEND, pagingState())

        assertFalse((result as MediatorResult.Success).endOfPaginationReached)
        assertTrue(api.characterPageRequests.isEmpty())
    }

    @Test
    fun prepend_alwaysReachesEnd() = runTest {
        val result = mediator.load(LoadType.PREPEND, pagingState())

        assertTrue((result as MediatorResult.Success).endOfPaginationReached)
        assertTrue(api.characterPageRequests.isEmpty())
    }

    @Test
    fun notFoundResponse_isTreatedAsEndOfPagination() = runTest {
        api.onGetCharacters = { _, _ -> throw httpError(404) }

        val result = mediator.load(LoadType.REFRESH, pagingState())

        assertTrue((result as MediatorResult.Success).endOfPaginationReached)
    }

    @Test
    fun serverError_returnsErrorAndKeepsCache() = runTest {
        api.onGetCharacters = { _, _ -> pageResponse(1..20, hasNext = true) }
        mediator.load(LoadType.REFRESH, pagingState())

        api.onGetCharacters = { _, _ -> throw httpError(500) }
        val result = mediator.load(LoadType.REFRESH, pagingState())

        assertTrue(result is MediatorResult.Error)
        assertEquals(20, characterCount())
    }

    @Test
    fun networkFailure_returnsErrorAndKeepsCache() = runTest {
        api.onGetCharacters = { _, _ -> pageResponse(1..20, hasNext = true) }
        mediator.load(LoadType.REFRESH, pagingState())

        api.onGetCharacters = { _, _ -> throw IOException("offline") }
        val result = mediator.load(LoadType.REFRESH, pagingState())

        assertTrue(result is MediatorResult.Error)
        assertEquals(20, characterCount())
    }

    private fun pagingState(vararg pages: List<CharacterEntity>) = PagingState<Int, CharacterEntity>(
        pages = pages.map { PagingSource.LoadResult.Page<Int, CharacterEntity>(data = it, prevKey = null, nextKey = null) },
        anchorPosition = null,
        config = PagingConfig(pageSize = 20),
        leadingPlaceholderCount = 0
    )

    private suspend fun characterCount(): Int =
        (1..100).count { database.characterDao.getById(it) != null }
}
