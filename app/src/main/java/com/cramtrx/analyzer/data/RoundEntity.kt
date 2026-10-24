package com.cramtrx.analyzer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aviator_rounds")
data class RoundEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampMillis: Long,
    val roundTimeStr: String,
    val multiplier: Double,
    val observedDateStr: String
)
