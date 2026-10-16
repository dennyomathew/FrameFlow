package com.dennymathew.frameflow.data

import com.dennymathew.frameflow.data.local.CharacterEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterFilterTest {

    private val rick = CharacterEntity(
        id = 1, name = "Rick Sanchez", status = "Alive", species = "Human",
        imageUrl = "https://example.com/1.jpeg", gender = "Male"
    )

    @Test
    fun emptyFilter_isInactiveAndMatchesEveryone() {
        assertFalse(CharacterFilter().isActive)
        assertTrue(CharacterFilter().matches(rick))
    }

    @Test
    fun matchesIgnoringCase() {
        assertTrue(CharacterFilter(status = "alive", species = "human", gender = "male").matches(rick))
    }

    @Test
    fun anyMismatchExcludes() {
        assertFalse(CharacterFilter(status = "dead").matches(rick))
        assertFalse(CharacterFilter(species = "Alien").matches(rick))
        assertFalse(CharacterFilter(gender = "female").matches(rick))
    }
}
