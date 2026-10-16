package com.dennymathew.frameflow.data

import com.dennymathew.frameflow.data.local.CharacterEntity

/**
 * Status, species and gender filters. Each value is the API's spelling (`alive`, `Human`,
 * `genderless`), or null for "any". The API and Room both compare them case-insensitively.
 */
data class CharacterFilter(
    val status: String? = null,
    val species: String? = null,
    val gender: String? = null
) {
    val isActive: Boolean get() = status != null || species != null || gender != null

    fun matches(character: CharacterEntity): Boolean =
        status.matches(character.status) &&
            species.matches(character.species) &&
            gender.matches(character.gender)

    private fun String?.matches(value: String) = this == null || equals(value, ignoreCase = true)

    companion object {
        val STATUSES = listOf("alive", "dead", "unknown")
        val GENDERS = listOf("female", "male", "genderless", "unknown")
        // The API's most common species; it has no endpoint listing them.
        val SPECIES = listOf(
            "Human", "Alien", "Humanoid", "Robot", "Animal", "Mythological Creature",
            "Cronenberg", "Poopybutthole", "Disease", "unknown"
        )
    }
}
