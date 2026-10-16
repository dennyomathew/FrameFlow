package com.dennymathew.frameflow.data.remote

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterDtoTest {

    private val gson = Gson()

    @Test
    fun parsesAndMapsFullApiCharacter() {
        // Trimmed copy of https://rickandmortyapi.com/api/character/1
        val json = """
            {
              "id": 1, "name": "Rick Sanchez", "status": "Alive", "species": "Human", "type": "",
              "gender": "Male",
              "origin": { "name": "Earth (C-137)", "url": "https://rickandmortyapi.com/api/location/1" },
              "location": { "name": "Citadel of Ricks", "url": "https://rickandmortyapi.com/api/location/3" },
              "image": "https://rickandmortyapi.com/api/character/avatar/1.jpeg",
              "episode": [
                "https://rickandmortyapi.com/api/episode/1",
                "https://rickandmortyapi.com/api/episode/2",
                "https://rickandmortyapi.com/api/episode/3"
              ]
            }
        """.trimIndent()

        val entity = gson.fromJson(json, CharacterDto::class.java).toEntity()

        assertEquals("Rick Sanchez", entity.name)
        assertEquals("Male", entity.gender)
        assertEquals("", entity.type)
        assertEquals("Earth (C-137)", entity.originName)
        assertEquals("Citadel of Ricks", entity.locationName)
        assertEquals(3, entity.episodeCount)
    }

    @Test
    fun missingOptionalFieldsMapToEmpty() {
        val json = """
            { "id": 2, "name": "Morty Smith", "status": "Alive", "species": "Human",
              "image": "https://example.com/2.jpeg" }
        """.trimIndent()

        val entity = gson.fromJson(json, CharacterDto::class.java).toEntity()

        assertEquals("", entity.gender)
        assertEquals("", entity.originName)
        assertEquals("", entity.locationName)
        assertEquals(0, entity.episodeCount)
    }
}
