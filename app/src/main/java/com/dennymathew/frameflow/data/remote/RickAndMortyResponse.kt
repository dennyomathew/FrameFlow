package com.dennymathew.frameflow.data.remote

import com.google.gson.annotations.SerializedName

data class RickAndMortyResponse(
    @SerializedName("info") val info: PageInfo? = null,
    @SerializedName("results") val results: List<CharacterDto> = emptyList()
)

data class PageInfo(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("pages") val pages: Int = 0,
    @SerializedName("next") val next: String? = null,
    @SerializedName("prev") val prev: String? = null
)

data class CharacterDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("status") val status: String,
    @SerializedName("species") val species: String,
    @SerializedName("image") val image: String
)
