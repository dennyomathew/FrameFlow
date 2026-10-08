package com.dennymathew.frameflow.data.local

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
    val addedAtMillis: Long
)

fun FavoriteEntity.toCharacter() = CharacterEntity(
    id = characterId,
    name = name,
    status = status,
    species = species,
    imageUrl = imageUrl
)
