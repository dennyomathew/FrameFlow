package com.dennymathew.frameflow.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.dennymathew.frameflow.data.local.ImageDatabase
import com.dennymathew.frameflow.data.local.CharacterEntity
import com.dennymathew.frameflow.data.local.hasDetails
import com.dennymathew.frameflow.data.remote.RickAndMortyApi
import com.dennymathew.frameflow.data.remote.CharacterRemoteMediator
import com.dennymathew.frameflow.data.remote.toEntity
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterRepository @Inject constructor(
    private val api: RickAndMortyApi,
    private val database: ImageDatabase
) {
    @OptIn(ExperimentalPagingApi::class)
    fun getCharacters(): Flow<PagingData<CharacterEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            remoteMediator = CharacterRemoteMediator(
                api = api,
                database = database
            ),
            pagingSourceFactory = {
                database.characterDao.pagingSource()
            }
        ).flow
    }

    // DB-backed search for instant local matches while network sync updates Room.
    fun searchCharacters(name: String): Flow<PagingData<CharacterEntity>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { database.characterDao.searchPagingSource(name) }
        ).flow
    }

    suspend fun syncSearchCharacters(name: String) {
        if (name.isBlank()) return

        var page = 1
        repeat(MAX_SEARCH_SYNC_PAGES) {
            try {
                val response = api.getCharacters(page = page, name = name)
                val mapped = response.results.map { it.toEntity() }
                if (mapped.isEmpty()) return
                database.characterDao.upsertAll(mapped)
                if (response.info?.next == null) return
                page += 1
            } catch (e: HttpException) {
                if (e.code() == 404) return
                throw e
            }
        }
    }

    suspend fun getCharacterById(id: Int): CharacterEntity? {
        // Try DB first (offline-first)
        val dbItem = try {
            database.characterDao.getById(id)
        } catch (e: Exception) {
            null
        }

        // Rows cached before schema 3 lack gender, origin etc.; refetch those once.
        if (dbItem != null && dbItem.hasDetails) return dbItem

        // fallback to network, keeping the stale row if that fails
        return try {
            val fetched = api.getCharacterById(id).toEntity()
            database.characterDao.upsertAll(listOf(fetched))
            fetched
        } catch (e: Exception) {
            dbItem
        }
    }

    companion object {
        private const val MAX_SEARCH_SYNC_PAGES = 3
    }
}
