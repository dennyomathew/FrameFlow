package com.dennymathew.frameflow.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface RickAndMortyApi {
    @GET("character")
    suspend fun getCharacters(
        @Query("page") page: Int,
        @Query("name") name: String? = null
    ): RickAndMortyResponse

    @GET("character/{id}")
    suspend fun getCharacterById(
        @retrofit2.http.Path("id") id: Int
    ): CharacterDto

    companion object {
        const val BASE_URL = "https://rickandmortyapi.com/api/"
    }
}
