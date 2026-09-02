package com.dennymathew.frameflow.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface CharacterDao {
    @Upsert
    suspend fun upsertAll(characters: List<CharacterEntity>)

    @Query("SELECT * FROM characters")
    fun pagingSource(): PagingSource<Int, CharacterEntity>

    @Query("SELECT * FROM characters WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): CharacterEntity?

    @Query(
        "SELECT * FROM characters " +
            "WHERE name LIKE '%' || :query || '%' " +
            "ORDER BY id ASC"
    )
    fun searchPagingSource(query: String): PagingSource<Int, CharacterEntity>

    @Query("DELETE FROM characters")
    suspend fun clearAll()
}
