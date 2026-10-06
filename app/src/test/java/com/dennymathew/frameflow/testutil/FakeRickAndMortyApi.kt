package com.dennymathew.frameflow.testutil

import com.dennymathew.frameflow.data.remote.CharacterDto
import com.dennymathew.frameflow.data.remote.PageInfo
import com.dennymathew.frameflow.data.remote.RickAndMortyApi
import com.dennymathew.frameflow.data.remote.RickAndMortyResponse
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

/** Programmable [RickAndMortyApi] that records every request it receives. */
class FakeRickAndMortyApi : RickAndMortyApi {

    val characterPageRequests = mutableListOf<Pair<Int, String?>>()
    val characterByIdRequests = mutableListOf<Int>()

    var onGetCharacters: (page: Int, name: String?) -> RickAndMortyResponse = { _, _ ->
        RickAndMortyResponse()
    }
    var onGetCharacterById: (id: Int) -> CharacterDto = { throw httpError(404) }

    override suspend fun getCharacters(page: Int, name: String?): RickAndMortyResponse {
        characterPageRequests += page to name
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
    image = "https://example.com/$id.jpeg"
)

/** A response for [ids]; [hasNext] controls whether the API reports a following page. */
fun pageResponse(ids: IntRange, hasNext: Boolean) = RickAndMortyResponse(
    info = PageInfo(next = if (hasNext) "https://example.com/next" else null),
    results = ids.map { characterDto(it) }
)

fun httpError(code: Int) = HttpException(
    Response.error<Any>(code, "".toResponseBody(null))
)
