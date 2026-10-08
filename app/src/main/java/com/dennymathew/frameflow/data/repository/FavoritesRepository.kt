package com.dennymathew.frameflow.data.repository

import com.dennymathew.frameflow.data.local.CharacterEntity
import com.dennymathew.frameflow.data.local.FavoriteEntity
import com.dennymathew.frameflow.data.local.ImageDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository(
    private val database: ImageDatabase,
    private val now: () -> Long
) {
    @Inject
    constructor(database: ImageDatabase) : this(database, System::currentTimeMillis)

    /** Saved characters, most recently saved first. */
    val favorites: Flow<List<FavoriteEntity>> = database.favoriteDao.observeAll()

    val favoriteIds: Flow<Set<Int>> = database.favoriteDao.observeIds().map { it.toSet() }

    fun isFavorite(characterId: Int): Flow<Boolean> =
        database.favoriteDao.observeIsFavorite(characterId)

    /** Saves [character] if it isn't a favorite yet, otherwise removes it. */
    suspend fun toggle(character: CharacterEntity) {
        val dao = database.favoriteDao
        if (dao.isFavorite(character.id)) {
            dao.delete(character.id)
        } else {
            dao.insert(
                FavoriteEntity(
                    characterId = character.id,
                    name = character.name,
                    status = character.status,
                    species = character.species,
                    imageUrl = character.imageUrl,
                    addedAtMillis = now()
                )
            )
        }
    }
}
