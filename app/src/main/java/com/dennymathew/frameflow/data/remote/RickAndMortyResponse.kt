package com.dennymathew.frameflow.data.remote

import com.google.gson.annotations.SerializedName

data class RickAndMortyResponse(
    @SerializedName("results") val results: List<CharacterDto>
)

data class CharacterDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("status") val status: String,
    @SerializedName("species") val species: String,
    @SerializedName("image") val image: String
)
