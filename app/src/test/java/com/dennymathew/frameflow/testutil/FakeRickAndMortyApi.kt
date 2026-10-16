package com.dennymathew.frameflow.testutil

import com.dennymathew.frameflow.data.CharacterFilter
import com.dennymathew.frameflow.data.remote.CharacterDto
import com.dennymathew.frameflow.data.remote.NamedLink
import com.dennymathew.frameflow.data.remote.PageInfo
import com.dennymathew.frameflow.data.remote.RickAndMortyApi
import com.dennymathew.frameflow.data.remote.RickAndMortyResponse
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

/** Programmable [RickAndMortyApi] that records every request it receives. */
class FakeRickAndMortyApi : RickAndMortyApi {

    val characterPageRequests = mutableListOf<Pair<Int, String?>>()
    /** The status/species/gender filters of each request in [characterPageRequests]. */
    val characterFilterRequests = mutableListOf<CharacterFilter>()
    val characterByIdRequests = mutableListOf<Int>()

    var onGetCharacters: (page: Int, name: String?) -> RickAndMortyResponse = { _, _ ->
        RickAndMortyResponse()
    }
    var onGetCharacterById: (id: Int) -> CharacterDto = { throw httpError(404) }

    override suspend fun getCharacters(
        page: Int,
        name: String?,
        status: String?,
        species: String?,
        gender: String?
    ): RickAndMortyResponse {
        characterPageRequests += page to name
        characterFilterRequests += CharacterFilter(status, species, gender)
        return onGetCharacters(page, name)
    }

    override suspend fun getCharacterById(id: Int): CharacterDto {
        characterByIdRequests += id
        return onGetCharacterById(id)
    }
}

fun characterDto(id: Int, name: String = "Character $id") = CharacterDto(
    id = id,
    name = name,
    status = "Alive",
    species = "Human",
    image = "https://example.com/$id.jpeg",
    type = "",
    gender = "Male",
    origin = NamedLink("Earth (C-137)", "https://example.com/location/1"),
    location = NamedLink("Citadel of Ricks", "https://example.com/location/3"),
    episode = listOf("https://example.com/episode/1", "https://example.com/episode/2")
)

/** A response for [ids]; [hasNext] controls whether the API reports a following page. */
fun pageResponse(ids: IntRange, hasNext: Boolean) = RickAndMortyResponse(
    info = PageInfo(next = if (hasNext) "https://example.com/next" else null),
    results = ids.map { characterDto(it) }
)

fun httpError(code: Int) = HttpException(
    Response.error<Any>(code, "".toResponseBody(null))
)
