package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CoachHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoachHistoryDao {
    @Query("SELECT * FROM coach_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<CoachHistoryEntity>>

    @Query("SELECT * FROM coach_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CoachHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CoachHistoryEntity): Long

    @Query("DELETE FROM coach_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM coach_history")
    suspend fun clearAll()
}
