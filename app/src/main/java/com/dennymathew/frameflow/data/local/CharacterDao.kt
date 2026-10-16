package com.dennymathew.frameflow.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface CharacterDao {
    @Upsert
    suspend fun upsertAll(characters: List<CharacterEntity>)

    @Query(
        "SELECT characters.* FROM characters " +
            "INNER JOIN remote_keys ON characters.id = remote_keys.characterId " +
            "ORDER BY characters.id ASC"
    )
    fun pagingSource(): PagingSource<Int, CharacterEntity>

    @Query("SELECT * FROM characters WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): CharacterEntity?

    /** Name substring plus optional exact filters; an empty filter argument matches anything. */
    @Query(
        "SELECT * FROM characters " +
            "WHERE name LIKE '%' || :query || '%' " +
            "AND (:status = '' OR status = :status COLLATE NOCASE) " +
            "AND (:species = '' OR species = :species COLLATE NOCASE) " +
            "AND (:gender = '' OR gender = :gender COLLATE NOCASE) " +
            "ORDER BY id ASC"
    )
    fun searchPagingSource(
        query: String,
        status: String = "",
        species: String = "",
        gender: String = ""
    ): PagingSource<Int, CharacterEntity>

    @Query("DELETE FROM characters")
    suspend fun clearAll()
}
