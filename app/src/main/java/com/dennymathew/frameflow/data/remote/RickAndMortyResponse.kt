package com.dennymathew.frameflow.data.remote

import com.dennymathew.frameflow.data.local.CharacterEntity
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
    @SerializedName("image") val image: String,
    @SerializedName("type") val type: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("origin") val origin: NamedLink? = null,
    @SerializedName("location") val location: NamedLink? = null,
    @SerializedName("episode") val episode: List<String>? = null
)

/** A `{ "name", "url" }` reference to another API resource, such as a location. */
data class NamedLink(
    @SerializedName("name") val name: String? = null,
    @SerializedName("url") val url: String? = null
)

// Gson ignores Kotlin defaults and nullability, so a missing field arrives as null.
fun CharacterDto.toEntity() = CharacterEntity(
    id = id,
    name = name,
    status = status,
    species = species,
    imageUrl = image,
    type = type.orEmpty(),
    gender = gender.orEmpty(),
    originName = origin?.name.orEmpty(),
    locationName = location?.name.orEmpty(),
    episodeCount = episode?.size ?: 0
)
