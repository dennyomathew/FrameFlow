package com.dennymathew.frameflow.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A character the user saved. It keeps its own copy of the character's details because the
 * `characters` table is a cache that a pull-to-refresh clears and rebuilds.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val characterId: Int,
    val name: String,
    val status: String,
    val species: String,
    val imageUrl: String,
    val addedAtMillis: Long,
    // Added in schema 3. Empty in favorites saved before then.
    @ColumnInfo(defaultValue = "") val type: String = "",
    @ColumnInfo(defaultValue = "") val gender: String = "",
    @ColumnInfo(defaultValue = "") val originName: String = "",
    @ColumnInfo(defaultValue = "") val locationName: String = "",
    @ColumnInfo(defaultValue = "0") val episodeCount: Int = 0
)

fun FavoriteEntity.toCharacter() = CharacterEntity(
    id = characterId,
    name = name,
    status = status,
    species = species,
    imageUrl = imageUrl,
    type = type,
    gender = gender,
    originName = originName,
    locationName = locationName,
    episodeCount = episodeCount
)

fun CharacterEntity.toFavorite(addedAtMillis: Long) = FavoriteEntity(
    characterId = id,
    name = name,
    status = status,
    species = species,
    imageUrl = imageUrl,
    addedAtMillis = addedAtMillis,
    type = type,
    gender = gender,
    originName = originName,
    locationName = locationName,
    episodeCount = episodeCount
)
