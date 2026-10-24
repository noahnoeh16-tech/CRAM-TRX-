package com.cramtrx.analyzer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RoundDao {
    @Query("SELECT * FROM aviator_rounds ORDER BY timestampMillis DESC")
    fun getAllRoundsFlow(): Flow<List<RoundEntity>>

    @Query("SELECT * FROM aviator_rounds ORDER BY timestampMillis ASC")
    suspend fun getAllRoundsAsc(): List<RoundEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRound(round: RoundEntity): Long

    @Delete
    suspend fun deleteRound(round: RoundEntity)

    @Query("DELETE FROM aviator_rounds")
    suspend fun clearAll()
}
