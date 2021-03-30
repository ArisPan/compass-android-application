package com.example.projectcompass

import androidx.room.Entity
import androidx.room.PrimaryKey

/*
 * @Entity - Each entity corresponds to a table in the associated Room database
 * and each instance of an entity represents a row of data in the corresponding table.
 */
@Entity
data class Measurement(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val latitude: Double,
    val longitude: Double,
    val speed: Float,
    val accuracy: Float,
    val date: String,
    val time: Long,
    val userID: Int,
    val hasBeenPublished: Boolean
)