package com.dennymathew.frameflow.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.dennymathew.frameflow.data.local.ImageDatabase
import com.dennymathew.frameflow.data.local.CharacterEntity
import com.dennymathew.frameflow.data.remote.RickAndMortyApi
import com.dennymathew.frameflow.data.remote.CharacterRemoteMediator
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
                val mapped = response.results.map {
                    CharacterEntity(
                        id = it.id,
                        name = it.name,
                        status = it.status,
                        species = it.species,
                        imageUrl = it.image
                    )
                }
                if (mapped.isEmpty()) return
                database.characterDao.upsertAll(mapped)
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

        if (dbItem != null) return dbItem

        // fallback to network
        return try {
            val dto = api.getCharacterById(id)
            CharacterEntity(
                id = dto.id,
                name = dto.name,
                status = dto.status,
                species = dto.species,
                imageUrl = dto.image
            )
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val MAX_SEARCH_SYNC_PAGES = 3
    }
}
