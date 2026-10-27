package com.dennymathew.frameflow.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.dennymathew.frameflow.data.CharacterFilter
import com.dennymathew.frameflow.data.local.ImageDatabase
import com.dennymathew.frameflow.data.local.CharacterEntity
import com.dennymathew.frameflow.data.local.hasDetails
import com.dennymathew.frameflow.data.local.toCharacter
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
    fun searchCharacters(
        name: String,
        filter: CharacterFilter = CharacterFilter()
    ): Flow<PagingData<CharacterEntity>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                database.characterDao.searchPagingSource(
                    query = name,
                    status = filter.status.orEmpty(),
                    species = filter.species.orEmpty(),
                    gender = filter.gender.orEmpty()
                )
            }
        ).flow
    }

    suspend fun syncSearchCharacters(name: String, filter: CharacterFilter = CharacterFilter()) {
        if (name.isBlank() && !filter.isActive) return

        var page = 1
        repeat(MAX_SEARCH_SYNC_PAGES) {
            try {
                val response = api.getCharacters(
                    page = page,
                    name = name.ifBlank { null },
                    status = filter.status,
                    species = filter.species,
                    gender = filter.gender
                )
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

        // fallback to network; if that fails, use what's stored locally
        return try {
            val fetched = api.getCharacterById(id).toEntity()
            database.characterDao.upsertAll(listOf(fetched))
            fetched
        } catch (e: Exception) {
            offlineFallback(id, dbItem)
        }
    }

    // A pull-to-refresh clears the cache but not favorites, which keep their own copy.
    // Prefer whichever has details: a favorite with details beats a stale cache row.
    private suspend fun offlineFallback(id: Int, dbItem: CharacterEntity?): CharacterEntity? {
        val favorite = try {
            database.favoriteDao.getById(id)?.toCharacter()
        } catch (e: Exception) {
            null
        }
        return if (favorite?.hasDetails == true) favorite else dbItem ?: favorite
    }

    companion object {
        private const val MAX_SEARCH_SYNC_PAGES = 3
    }
}
