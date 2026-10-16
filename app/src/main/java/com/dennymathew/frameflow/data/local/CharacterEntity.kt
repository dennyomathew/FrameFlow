package com.dennymathew.frameflow.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val status: String,
    val species: String,
    val imageUrl: String,
    // Added in schema 3. Empty in rows cached before then; see [hasDetails].
    @ColumnInfo(defaultValue = "") val type: String = "",
    @ColumnInfo(defaultValue = "") val gender: String = "",
    @ColumnInfo(defaultValue = "") val originName: String = "",
    @ColumnInfo(defaultValue = "") val locationName: String = "",
    @ColumnInfo(defaultValue = "0") val episodeCount: Int = 0
)

/** False for rows cached before schema 3; the API always sends a gender, even "unknown". */
val CharacterEntity.hasDetails: Boolean get() = gender.isNotEmpty()
